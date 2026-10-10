// 관리자용 직원 기능(CSV 일괄 등록·발령 이력 조회) 서비스
package com.worksync.domain.employee.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.dto.EmployeeAdminDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.employee.repository.EmployeeHistoryRepository;
import com.worksync.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** 관리자용 직원 기능 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeAdminService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final EmployeeRepository employeeRepository;
	private final EmployeeHistoryRepository employeeHistoryRepository;
	private final DepartmentRepository departmentRepository;
	private final PasswordEncoder passwordEncoder;

	// 직원 일괄 등록 — 행마다 따로 검증해, 문제 있는 행만 건너뛰고 나머지는 등록한다
	@Transactional
	public EmployeeAdminDto.BulkResponse bulkCreate(List<EmployeeAdminDto.BulkRow> rows) {
		List<EmployeeAdminDto.BulkRowResult> results = new ArrayList<>();
		// 같은 파일 안에서 사번·이메일이 겹치는 경우도 잡는다
		Set<String> seenEmpNos = new HashSet<>();
		Set<String> seenEmails = new HashSet<>();
		int successCount = 0;

		for (int i = 0; i < rows.size(); i++) {
			EmployeeAdminDto.BulkRow row = rows.get(i);
			String error = validate(row, seenEmpNos, seenEmails);

			Optional<Department> department = Optional.empty();
			if (error == null && StringUtils.hasText(row.getDepartmentName())) {
				department = departmentRepository.findByName(row.getDepartmentName().trim());
				if (department.isEmpty()) {
					error = "존재하지 않는 부서입니다: " + row.getDepartmentName();
				}
			}

			if (error == null) {
				employeeRepository.save(Employee.builder()
						.empNo(row.getEmpNo().trim())
						.name(row.getName().trim())
						.email(row.getEmail().trim())
						.password(passwordEncoder.encode(row.getPassword()))
						.phone(StringUtils.hasText(row.getPhone()) ? row.getPhone().trim() : null)
						.jobGrade(parseJobGrade(row.getJobGrade()))
						.department(department.orElse(null))
						.hireDate(parseDate(row.getHireDate()))
						.build());
				seenEmpNos.add(row.getEmpNo().trim());
				seenEmails.add(row.getEmail().trim());
				successCount++;
			}

			results.add(EmployeeAdminDto.BulkRowResult.builder()
					.rowNumber(i + 1)
					.empNo(row.getEmpNo())
					.isSuccess(error == null)
					.message(error)
					.build());
		}

		return EmployeeAdminDto.BulkResponse.builder()
				.successCount(successCount)
				.failCount(rows.size() - successCount)
				.results(results)
				.build();
	}

	// 인사 발령 이력 조회 — 최신순
	public List<EmployeeAdminDto.HistoryResponse> getHistory(Long employeeId) {
		return employeeHistoryRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
				.map(EmployeeAdminDto.HistoryResponse::from)
				.toList();
	}

	// 한 행 검증 — 문제가 없으면 null, 있으면 사유
	private String validate(EmployeeAdminDto.BulkRow row, Set<String> seenEmpNos, Set<String> seenEmails) {
		if (!StringUtils.hasText(row.getEmpNo()) || !StringUtils.hasText(row.getName())
				|| !StringUtils.hasText(row.getEmail()) || !StringUtils.hasText(row.getPassword())) {
			return "사번·이름·이메일·비밀번호는 필수입니다.";
		}
		String empNo = row.getEmpNo().trim();
		String email = row.getEmail().trim();

		if (!EMAIL_PATTERN.matcher(email).matches()) {
			return "이메일 형식이 올바르지 않습니다.";
		}
		if (StringUtils.hasText(row.getJobGrade()) && parseJobGrade(row.getJobGrade()) == null) {
			return "직급 값이 올바르지 않습니다: " + row.getJobGrade();
		}
		if (StringUtils.hasText(row.getHireDate()) && parseDate(row.getHireDate()) == null) {
			return "입사일은 yyyy-MM-dd 형식이어야 합니다.";
		}
		if (seenEmpNos.contains(empNo) || employeeRepository.existsByEmpNo(empNo)) {
			return "이미 사용 중인 사번입니다.";
		}
		if (seenEmails.contains(email) || employeeRepository.existsByEmail(email)) {
			return "이미 사용 중인 이메일입니다.";
		}
		return null;
	}

	// 직급 — 비어 있으면 기본값(STAFF), 잘못된 값이면 null
	private JobGrade parseJobGrade(String value) {
		if (!StringUtils.hasText(value)) {
			return JobGrade.STAFF;
		}
		try {
			return JobGrade.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private LocalDate parseDate(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
