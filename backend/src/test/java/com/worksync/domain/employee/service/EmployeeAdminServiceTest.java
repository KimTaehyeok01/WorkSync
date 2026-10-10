// EmployeeAdminService 단위 테스트 — 직원 CSV 일괄 등록
package com.worksync.domain.employee.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.dto.EmployeeAdminDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.employee.repository.EmployeeHistoryRepository;
import com.worksync.domain.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** EmployeeAdminService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class EmployeeAdminServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private EmployeeHistoryRepository employeeHistoryRepository;
	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private EmployeeAdminService employeeAdminService;

	private EmployeeAdminDto.BulkRow buildRow(String empNo, String email) {
		EmployeeAdminDto.BulkRow row = new EmployeeAdminDto.BulkRow();
		row.setEmpNo(empNo);
		row.setName("홍길동");
		row.setEmail(email);
		row.setPassword("init-password");
		return row;
	}

	@DisplayName("올바른 행은 비밀번호를 인코딩해 등록하고, 직급을 비우면 사원으로 등록한다")
	@Test
	void bulkCreate_validRow_savesWithDefaults() {
		// given
		EmployeeAdminDto.BulkRow row = buildRow("EMP101", "hong@worksync.com");
		row.setDepartmentName("개발팀");
		row.setHireDate("2026-01-02");
		Department department = Department.builder().id(1L).name("개발팀").build();
		given(departmentRepository.findByName("개발팀")).willReturn(Optional.of(department));
		given(passwordEncoder.encode("init-password")).willReturn("encoded");

		// when
		EmployeeAdminDto.BulkResponse result = employeeAdminService.bulkCreate(List.of(row));

		// then
		ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
		verify(employeeRepository).save(captor.capture());
		assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
		assertThat(captor.getValue().getJobGrade()).isEqualTo(JobGrade.STAFF);
		assertThat(captor.getValue().getDepartment()).isEqualTo(department);
		assertThat(captor.getValue().getHireDate()).isEqualTo(LocalDate.of(2026, 1, 2));
		assertThat(result.getSuccessCount()).isEqualTo(1);
		assertThat(result.getFailCount()).isZero();
	}

	@DisplayName("문제 있는 행만 건너뛰고 나머지는 등록하며, 행별 실패 사유를 돌려준다")
	@Test
	void bulkCreate_mixedRows_savesValidAndReportsInvalid() {
		// given
		EmployeeAdminDto.BulkRow valid = buildRow("EMP101", "a@worksync.com");
		EmployeeAdminDto.BulkRow missingName = buildRow("EMP102", "b@worksync.com");
		missingName.setName(" ");
		EmployeeAdminDto.BulkRow badEmail = buildRow("EMP103", "not-an-email");
		EmployeeAdminDto.BulkRow badGrade = buildRow("EMP104", "d@worksync.com");
		badGrade.setJobGrade("BOSS");
		EmployeeAdminDto.BulkRow existing = buildRow("EMP001", "e@worksync.com");
		given(employeeRepository.existsByEmpNo("EMP101")).willReturn(false);
		given(employeeRepository.existsByEmpNo("EMP001")).willReturn(true);
		given(passwordEncoder.encode(any())).willReturn("encoded");

		// when
		EmployeeAdminDto.BulkResponse result = employeeAdminService.bulkCreate(
				List.of(valid, missingName, badEmail, badGrade, existing));

		// then
		verify(employeeRepository, times(1)).save(any(Employee.class));
		assertThat(result.getSuccessCount()).isEqualTo(1);
		assertThat(result.getFailCount()).isEqualTo(4);
		assertThat(result.getResults()).extracting(EmployeeAdminDto.BulkRowResult::getIsSuccess)
				.containsExactly(true, false, false, false, false);
		assertThat(result.getResults().get(4).getMessage()).contains("사번");
	}

	@DisplayName("같은 파일 안에서 사번이 겹치면 두 번째 행은 등록하지 않는다")
	@Test
	void bulkCreate_duplicateInFile_rejectsSecond() {
		// given
		given(passwordEncoder.encode(any())).willReturn("encoded");

		// when
		EmployeeAdminDto.BulkResponse result = employeeAdminService.bulkCreate(List.of(
				buildRow("EMP101", "a@worksync.com"),
				buildRow("EMP101", "b@worksync.com")));

		// then
		verify(employeeRepository, times(1)).save(any(Employee.class));
		assertThat(result.getResults().get(1).getIsSuccess()).isFalse();
	}

	@DisplayName("등록되지 않은 부서명이면 그 행은 등록하지 않는다")
	@Test
	void bulkCreate_unknownDepartment_rejectsRow() {
		// given
		EmployeeAdminDto.BulkRow row = buildRow("EMP101", "a@worksync.com");
		row.setDepartmentName("없는팀");
		given(departmentRepository.findByName("없는팀")).willReturn(Optional.empty());

		// when
		EmployeeAdminDto.BulkResponse result = employeeAdminService.bulkCreate(List.of(row));

		// then
		verify(employeeRepository, never()).save(any());
		assertThat(result.getResults().get(0).getMessage()).contains("부서");
	}
}
