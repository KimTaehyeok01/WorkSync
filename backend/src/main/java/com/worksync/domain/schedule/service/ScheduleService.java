// 캘린더 일정 조회·등록·수정·삭제 서비스
package com.worksync.domain.schedule.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.domain.schedule.dto.ScheduleDto;
import com.worksync.domain.schedule.entity.RepeatType;
import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleAttendee;
import com.worksync.domain.schedule.entity.ScheduleScope;
import com.worksync.domain.schedule.repository.ScheduleAttendeeRepository;
import com.worksync.domain.schedule.repository.ScheduleRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 캘린더 일정 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

	private static final String TARGET_TYPE = "SCHEDULE";
	// 반복 회차 전개 상한 — 종료일 없는 오래된 반복 일정이 끝없이 돌지 않도록 한다
	private static final int MAX_OCCURRENCES = 5000;

	private final ScheduleRepository scheduleRepository;
	private final ScheduleAttendeeRepository scheduleAttendeeRepository;
	private final EmployeeRepository employeeRepository;
	private final NotificationService notificationService;

	// 기간(from~to, 양끝 포함)에 걸치는 일정 중 내가 볼 수 있는 것 조회 — 반복 일정은 회차별로 펼쳐서 반환
	public List<ScheduleDto.Response> getSchedules(Long employeeId, LocalDate from, LocalDate to) {
		Employee me = findEmployee(employeeId);
		Long departmentId = me.getDepartment() != null ? me.getDepartment().getId() : null;
		LocalDateTime rangeStart = from.atStartOfDay();
		LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();

		List<Schedule> schedules = scheduleRepository.findVisible(
				employeeId, departmentId, rangeStart, from, rangeEnd, RepeatType.NONE,
				ScheduleScope.PERSONAL, ScheduleScope.DEPARTMENT, ScheduleScope.COMPANY);
		Map<Long, List<ScheduleDto.AttendeeResponse>> attendeesBySchedule = loadAttendees(schedules);

		List<ScheduleDto.Response> result = new ArrayList<>();
		for (Schedule schedule : schedules) {
			List<ScheduleDto.AttendeeResponse> attendees =
					attendeesBySchedule.getOrDefault(schedule.getId(), List.of());
			boolean editable = canEdit(schedule, me);
			for (LocalDateTime start : expandStarts(schedule, rangeStart, rangeEnd)) {
				result.add(ScheduleDto.Response.of(
						schedule, start, start.plus(durationOf(schedule)), attendees, editable));
			}
		}
		result.sort(Comparator.comparing(ScheduleDto.Response::getStartAt));
		return result;
	}

	// 일정 등록
	@Transactional
	public ScheduleDto.Response create(Long employeeId, ScheduleDto.CreateRequest request) {
		Employee me = findEmployee(employeeId);
		validate(request, me);

		Schedule schedule = scheduleRepository.save(Schedule.builder()
				.owner(me)
				.department(resolveDepartment(request.getScope(), me))
				.title(request.getTitle())
				.description(request.getDescription())
				.location(request.getLocation())
				.startAt(request.getStartAt())
				.endAt(request.getEndAt())
				.isAllDay(Boolean.TRUE.equals(request.getIsAllDay()))
				.scope(request.getScope())
				.repeatType(repeatTypeOf(request))
				.repeatUntilDt(repeatUntilOf(request))
				.build());

		List<Employee> attendees = findAttendees(request.getAttendeeIds(), me);
		scheduleAttendeeRepository.saveAll(attendees.stream()
				.map(employee -> ScheduleAttendee.builder().schedule(schedule).employee(employee).build())
				.toList());

		notifyCreated(schedule, me, attendees);
		return toResponse(schedule, attendees);
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
				resolveDepartment(request.getScope(), schedule.getOwner()),
				repeatTypeOf(request),
				repeatUntilOf(request));

		// 참석자 변경분만 반영 — 빠진 사람은 지우고, 새로 들어온 사람만 추가·알림
		List<Employee> attendees = findAttendees(request.getAttendeeIds(), schedule.getOwner());
		Set<Long> newIds = attendees.stream().map(Employee::getId).collect(Collectors.toSet());
		Set<Long> oldIds = scheduleAttendeeRepository.findByScheduleIds(List.of(schedule.getId())).stream()
				.map(attendee -> attendee.getEmployee().getId())
				.collect(Collectors.toSet());

		List<Long> removed = oldIds.stream().filter(id -> !newIds.contains(id)).toList();
		if (!removed.isEmpty()) {
			scheduleAttendeeRepository.deleteByScheduleIdAndEmployeeIds(schedule.getId(), removed);
		}
		List<Employee> added = attendees.stream().filter(employee -> !oldIds.contains(employee.getId())).toList();
		scheduleAttendeeRepository.saveAll(added.stream()
				.map(employee -> ScheduleAttendee.builder().schedule(schedule).employee(employee).build())
				.toList());
		added.forEach(employee -> notifyInvited(schedule, me, employee));

		return toResponse(schedule, attendees);
	}

	// 일정 삭제 — 작성자 또는 ADMIN만 가능 (참석자 행은 DB의 ON DELETE CASCADE 로 함께 지워진다)
	@Transactional
	public void delete(Long employeeId, Long scheduleId) {
		Employee me = findEmployee(employeeId);
		scheduleRepository.delete(findEditable(scheduleId, me));
	}

	// 일정의 회차 중 조회 기간과 겹치는 회차의 시작 시각 목록 (단발 일정은 0~1개)
	private List<LocalDateTime> expandStarts(Schedule schedule, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
		Duration duration = durationOf(schedule);
		List<LocalDateTime> starts = new ArrayList<>();

		for (int i = 0; i < MAX_OCCURRENCES; i++) {
			LocalDateTime start = occurrenceStart(schedule, i);
			if (!start.isBefore(rangeEnd)) {
				break;
			}
			if (schedule.getRepeatUntilDt() != null && start.toLocalDate().isAfter(schedule.getRepeatUntilDt())) {
				break;
			}
			if (!start.plus(duration).isBefore(rangeStart)) {
				starts.add(start);
			}
			if (schedule.getRepeatType() == RepeatType.NONE) {
				break;
			}
		}
		return starts;
	}

	// i번째 회차의 시작 시각 — 매월 반복은 첫 회차 기준으로 더해 31일 일정이 28일로 굳지 않게 한다
	private LocalDateTime occurrenceStart(Schedule schedule, int index) {
		return switch (schedule.getRepeatType()) {
			case DAILY -> schedule.getStartAt().plusDays(index);
			case WEEKLY -> schedule.getStartAt().plusWeeks(index);
			case MONTHLY -> schedule.getStartAt().plusMonths(index);
			case NONE -> schedule.getStartAt();
		};
	}

	private Duration durationOf(Schedule schedule) {
		return Duration.between(schedule.getStartAt(), schedule.getEndAt());
	}

	private ScheduleDto.Response toResponse(Schedule schedule, List<Employee> attendees) {
		return ScheduleDto.Response.of(
				schedule, schedule.getStartAt(), schedule.getEndAt(),
				attendees.stream().map(ScheduleDto.AttendeeResponse::from).toList(), true);
	}

	private Map<Long, List<ScheduleDto.AttendeeResponse>> loadAttendees(List<Schedule> schedules) {
		if (schedules.isEmpty()) {
			return Map.of();
		}
		return scheduleAttendeeRepository.findByScheduleIds(schedules.stream().map(Schedule::getId).toList())
				.stream()
				.collect(Collectors.groupingBy(
						attendee -> attendee.getSchedule().getId(),
						Collectors.mapping(
								attendee -> ScheduleDto.AttendeeResponse.from(attendee.getEmployee()),
								Collectors.toList())));
	}

	// 참석자 조회 — 중복과 작성자 본인은 제외한다
	private List<Employee> findAttendees(List<Long> attendeeIds, Employee owner) {
		if (attendeeIds == null || attendeeIds.isEmpty()) {
			return List.of();
		}
		Set<Long> ids = new LinkedHashSet<>(attendeeIds);
		ids.remove(owner.getId());
		List<Employee> employees = employeeRepository.findAllById(ids);
		if (employees.size() != ids.size()) {
			throw new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND);
		}
		return employees;
	}

	// 등록 알림 — 부서 일정은 부서원, 전사 일정은 재직자 전원, 그리고 참석자 (작성자 제외, 한 사람에게 한 번만)
	private void notifyCreated(Schedule schedule, Employee actor, List<Employee> attendees) {
		Set<Long> attendeeIds = attendees.stream().map(Employee::getId).collect(Collectors.toSet());
		attendees.forEach(employee -> notifyInvited(schedule, actor, employee));

		List<Employee> audience = switch (schedule.getScope()) {
			case DEPARTMENT -> employeeRepository.findByDepartmentIdAndStatusNot(
					schedule.getDepartment().getId(), EmployeeStatus.INACTIVE);
			case COMPANY -> employeeRepository.findByStatusNot(EmployeeStatus.INACTIVE);
			case PERSONAL -> List.of();
		};
		notificationService.sendToMany(
				audience.stream().map(Employee::getId).filter(id -> !attendeeIds.contains(id)).toList(),
				actor.getId(),
				NotificationType.SCHEDULE,
				"새 일정이 등록되었습니다: " + schedule.getTitle(),
				TARGET_TYPE,
				schedule.getId());
	}

	private void notifyInvited(Schedule schedule, Employee actor, Employee attendee) {
		if (attendee.getId().equals(actor.getId())) {
			return;
		}
		notificationService.send(
				attendee.getId(),
				NotificationType.SCHEDULE,
				actor.getName() + "님이 일정에 초대했습니다: " + schedule.getTitle(),
				TARGET_TYPE,
				schedule.getId());
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

	private RepeatType repeatTypeOf(ScheduleDto.CreateRequest request) {
		return request.getRepeatType() != null ? request.getRepeatType() : RepeatType.NONE;
	}

	// 반복하지 않는 일정은 종료일을 저장하지 않는다
	private LocalDate repeatUntilOf(ScheduleDto.CreateRequest request) {
		return repeatTypeOf(request) == RepeatType.NONE ? null : request.getRepeatUntilDt();
	}

	// 기간 역전 금지, 반복 종료일은 시작일 이후, 전사 일정은 ADMIN만 등록·지정 가능
	private void validate(ScheduleDto.CreateRequest request, Employee me) {
		if (request.getEndAt().isBefore(request.getStartAt())) {
			throw new CustomException(ErrorCode.INVALID_SCHEDULE_PERIOD);
		}
		LocalDate repeatUntil = repeatUntilOf(request);
		if (repeatUntil != null && repeatUntil.isBefore(request.getStartAt().toLocalDate())) {
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
