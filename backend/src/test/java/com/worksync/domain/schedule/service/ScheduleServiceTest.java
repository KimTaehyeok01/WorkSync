// ScheduleService 단위 테스트
package com.worksync.domain.schedule.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.schedule.dto.ScheduleDto;
import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleScope;
import com.worksync.domain.schedule.repository.ScheduleRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** ScheduleService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

	@Mock
	private ScheduleRepository scheduleRepository;
	@Mock
	private EmployeeRepository employeeRepository;

	@InjectMocks
	private ScheduleService scheduleService;

	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 12, 10, 0);
	private static final LocalDateTime END = LocalDateTime.of(2026, 10, 12, 11, 0);

	private Employee buildEmployee(Long id, EmployeeRole role, Department department) {
		return Employee.builder()
				.id(id)
				.empNo("EMP" + id)
				.name("직원" + id)
				.email("emp" + id + "@worksync.com")
				.password("encoded-password")
				.role(role)
				.department(department)
				.build();
	}

	private ScheduleDto.CreateRequest buildCreateRequest(ScheduleScope scope) {
		ScheduleDto.CreateRequest request = new ScheduleDto.CreateRequest();
		request.setTitle("주간 회의");
		request.setStartAt(START);
		request.setEndAt(END);
		request.setScope(scope);
		return request;
	}

	private ScheduleDto.UpdateRequest buildUpdateRequest(Long id, ScheduleScope scope) {
		ScheduleDto.UpdateRequest request = new ScheduleDto.UpdateRequest();
		request.setId(id);
		request.setTitle("수정된 회의");
		request.setStartAt(START);
		request.setEndAt(END);
		request.setScope(scope);
		return request;
	}

	private Schedule buildSchedule(Long id, Employee owner, ScheduleScope scope) {
		return Schedule.builder()
				.id(id).owner(owner).title("주간 회의")
				.startAt(START).endAt(END).isAllDay(false).scope(scope)
				.build();
	}

	@DisplayName("일정을 조회하면 종료일 다음 날 0시를 상한으로 조회하고, 타인의 일정은 수정 불가로 표시한다")
	@Test
	void getSchedules_success_marksEditableByOwner() {
		// given
		Department department = Department.builder().id(10L).name("개발팀").build();
		Employee me = buildEmployee(1L, EmployeeRole.USER, department);
		Employee other = buildEmployee(2L, EmployeeRole.USER, department);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(scheduleRepository.findVisible(
				1L, 10L,
				LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0),
				ScheduleScope.PERSONAL, ScheduleScope.DEPARTMENT, ScheduleScope.COMPANY))
				.willReturn(List.of(
						buildSchedule(1L, me, ScheduleScope.PERSONAL),
						buildSchedule(2L, other, ScheduleScope.DEPARTMENT)));

		// when
		List<ScheduleDto.Response> result = scheduleService.getSchedules(
				1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getIsEditable()).isTrue();
		assertThat(result.get(1).getIsEditable()).isFalse();
	}

	@DisplayName("부서 일정을 등록하면 작성자의 부서가 공유 대상으로 저장된다")
	@Test
	void create_departmentScope_setsOwnerDepartment() {
		// given
		Department department = Department.builder().id(10L).name("개발팀").build();
		Employee me = buildEmployee(1L, EmployeeRole.USER, department);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ScheduleDto.Response result = scheduleService.create(1L, buildCreateRequest(ScheduleScope.DEPARTMENT));

		// then
		ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
		verify(scheduleRepository).save(captor.capture());
		assertThat(captor.getValue().getDepartment()).isEqualTo(department);
		assertThat(captor.getValue().getIsAllDay()).isFalse();
		assertThat(result.getScope()).isEqualTo(ScheduleScope.DEPARTMENT);
	}

	@DisplayName("개인 일정을 등록하면 공유 대상 부서는 비워 둔다")
	@Test
	void create_personalScope_hasNoDepartment() {
		// given
		Department department = Department.builder().id(10L).name("개발팀").build();
		Employee me = buildEmployee(1L, EmployeeRole.USER, department);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		scheduleService.create(1L, buildCreateRequest(ScheduleScope.PERSONAL));

		// then
		ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
		verify(scheduleRepository).save(captor.capture());
		assertThat(captor.getValue().getDepartment()).isNull();
	}

	@DisplayName("종료 일시가 시작 일시보다 빠르면 INVALID_SCHEDULE_PERIOD 예외가 발생한다")
	@Test
	void create_endBeforeStart_throwsInvalidPeriod() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER, null);
		ScheduleDto.CreateRequest request = buildCreateRequest(ScheduleScope.PERSONAL);
		request.setEndAt(START.minusHours(1));
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));

		// when & then
		assertThatThrownBy(() -> scheduleService.create(1L, request))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_SCHEDULE_PERIOD);
		verify(scheduleRepository, never()).save(any());
	}

	@DisplayName("일반 사용자가 전사 일정을 등록하면 FORBIDDEN 예외가 발생한다")
	@Test
	void create_companyScopeByUser_throwsForbidden() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER, null);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));

		// when & then
		assertThatThrownBy(() -> scheduleService.create(1L, buildCreateRequest(ScheduleScope.COMPANY)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(scheduleRepository, never()).save(any());
	}

	@DisplayName("부서가 없는 사용자가 부서 일정을 등록하면 DEPARTMENT_NOT_FOUND 예외가 발생한다")
	@Test
	void create_departmentScopeWithoutDepartment_throwsDepartmentNotFound() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER, null);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));

		// when & then
		assertThatThrownBy(() -> scheduleService.create(1L, buildCreateRequest(ScheduleScope.DEPARTMENT)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
	}

	@DisplayName("작성자가 일정을 수정하면 내용이 변경된다")
	@Test
	void update_byOwner_updatesFields() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER, null);
		Schedule schedule = buildSchedule(5L, me, ScheduleScope.PERSONAL);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(scheduleRepository.findById(5L)).willReturn(Optional.of(schedule));

		// when
		ScheduleDto.Response result = scheduleService.update(1L, buildUpdateRequest(5L, ScheduleScope.PERSONAL));

		// then
		assertThat(result.getTitle()).isEqualTo("수정된 회의");
		assertThat(schedule.getTitle()).isEqualTo("수정된 회의");
	}

	@DisplayName("작성자가 아닌 일반 사용자가 일정을 수정하면 FORBIDDEN 예외가 발생한다")
	@Test
	void update_byOtherUser_throwsForbidden() {
		// given
		Employee owner = buildEmployee(1L, EmployeeRole.USER, null);
		Employee other = buildEmployee(2L, EmployeeRole.USER, null);
		Schedule schedule = buildSchedule(5L, owner, ScheduleScope.PERSONAL);
		given(employeeRepository.findById(2L)).willReturn(Optional.of(other));
		given(scheduleRepository.findById(5L)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> scheduleService.update(2L, buildUpdateRequest(5L, ScheduleScope.PERSONAL)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.FORBIDDEN);
		assertThat(schedule.getTitle()).isEqualTo("주간 회의");
	}

	@DisplayName("존재하지 않는 일정을 삭제하면 SCHEDULE_NOT_FOUND 예외가 발생한다")
	@Test
	void delete_notFound_throwsScheduleNotFound() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER, null);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(scheduleRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scheduleService.delete(1L, 999L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@DisplayName("ADMIN은 타인의 일정도 삭제할 수 있다")
	@Test
	void delete_byAdmin_deletesOthersSchedule() {
		// given
		Employee owner = buildEmployee(1L, EmployeeRole.USER, null);
		Employee admin = buildEmployee(9L, EmployeeRole.ADMIN, null);
		Schedule schedule = buildSchedule(5L, owner, ScheduleScope.PERSONAL);
		given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
		given(scheduleRepository.findById(5L)).willReturn(Optional.of(schedule));

		// when
		scheduleService.delete(9L, 5L);

		// then
		verify(scheduleRepository).delete(schedule);
	}

	@DisplayName("작성자가 아닌 일반 사용자가 일정을 삭제하면 FORBIDDEN 예외가 발생한다")
	@Test
	void delete_byOtherUser_throwsForbidden() {
		// given
		Employee owner = buildEmployee(1L, EmployeeRole.USER, null);
		Employee other = buildEmployee(2L, EmployeeRole.USER, null);
		Schedule schedule = buildSchedule(5L, owner, ScheduleScope.PERSONAL);
		given(employeeRepository.findById(2L)).willReturn(Optional.of(other));
		given(scheduleRepository.findById(5L)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> scheduleService.delete(2L, 5L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(scheduleRepository, never()).delete(any());
	}
}
