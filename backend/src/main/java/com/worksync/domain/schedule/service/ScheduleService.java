// 캘린더 일정 조회·등록·수정·삭제 서비스
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 캘린더 일정 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final EmployeeRepository employeeRepository;

	// 기간(from~to, 양끝 포함)과 겹치는 일정 중 내가 볼 수 있는 것 조회
	public List<ScheduleDto.Response> getSchedules(Long employeeId, LocalDate from, LocalDate to) {
		Employee me = findEmployee(employeeId);
		Long departmentId = me.getDepartment() != null ? me.getDepartment().getId() : null;

		return scheduleRepository.findVisible(
						employeeId, departmentId,
						from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
						ScheduleScope.PERSONAL, ScheduleScope.DEPARTMENT, ScheduleScope.COMPANY)
				.stream()
				.map(schedule -> ScheduleDto.Response.of(schedule, canEdit(schedule, me)))
				.toList();
	}

	// 일정 등록
	@Transactional
	public ScheduleDto.Response create(Long employeeId, ScheduleDto.CreateRequest request) {
		Employee me = findEmployee(employeeId);
		validate(request, me);

		Schedule schedule = Schedule.builder()
				.owner(me)
				.department(resolveDepartment(request.getScope(), me))
				.title(request.getTitle())
				.description(request.getDescription())
				.location(request.getLocation())
				.startAt(request.getStartAt())
				.endAt(request.getEndAt())
				.isAllDay(Boolean.TRUE.equals(request.getIsAllDay()))
				.scope(request.getScope())
				.build();

		return ScheduleDto.Response.of(scheduleRepository.save(schedule), true);
	}

	// 일정 수정 — 작성자 또는 ADMIN만 가능
	@Transactional
	public ScheduleDto.Response update(Long employeeId, ScheduleDto.UpdateRequest request) {
		Employee me = findEmployee(employeeId);
		Schedule schedule = findEditable(request.getId(), me);
		validate(request, me);

		// 부서 일정의 공유 대상은 수정한 사람이 아니라 작성자의 부서 기준
		schedule.update(
				request.getTitle(),
				request.getDescription(),
				request.getLocation(),
				request.getStartAt(),
				request.getEndAt(),
				Boolean.TRUE.equals(request.getIsAllDay()),
				request.getScope(),
				resolveDepartment(request.getScope(), schedule.getOwner()));

		return ScheduleDto.Response.of(schedule, true);
	}

	// 일정 삭제 — 작성자 또는 ADMIN만 가능
	@Transactional
	public void delete(Long employeeId, Long scheduleId) {
		Employee me = findEmployee(employeeId);
		scheduleRepository.delete(findEditable(scheduleId, me));
	}

	private Employee findEmployee(Long employeeId) {
		return employeeRepository.findById(employeeId)
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
	}

	private Schedule findEditable(Long scheduleId, Employee me) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
		if (!canEdit(schedule, me)) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
		return schedule;
	}

	private boolean canEdit(Schedule schedule, Employee me) {
		return schedule.getOwner().getId().equals(me.getId()) || me.getRole() == EmployeeRole.ADMIN;
	}

	// 기간 역전 금지, 전사 일정은 ADMIN만 등록·지정 가능
	private void validate(ScheduleDto.CreateRequest request, Employee me) {
		if (request.getEndAt().isBefore(request.getStartAt())) {
			throw new CustomException(ErrorCode.INVALID_SCHEDULE_PERIOD);
		}
		if (request.getScope() == ScheduleScope.COMPANY && me.getRole() != EmployeeRole.ADMIN) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
	}

	// 부서 일정이면 작성자의 부서를 공유 대상으로 지정 (부서 미배정이면 등록 불가)
	private Department resolveDepartment(ScheduleScope scope, Employee owner) {
		if (scope != ScheduleScope.DEPARTMENT) {
			return null;
		}
		if (owner.getDepartment() == null) {
			throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
		}
		return owner.getDepartment();
	}
}
