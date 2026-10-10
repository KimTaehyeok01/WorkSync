// 캘린더 일정 도메인 요청/응답 DTO 모음
package com.worksync.domain.schedule.dto;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.schedule.entity.RepeatType;
import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 캘린더 일정 DTO 모음. */
public class ScheduleDto {

	@Getter @Setter
	@Schema(name = "ScheduleDto.CreateRequest")
	public static class CreateRequest {

		@NotBlank
		@Size(max = 100)
		@Schema(description = "제목", example = "주간 회의")
		private String title;

		@Schema(description = "설명")
		private String description;

		@Size(max = 100)
		@Schema(description = "장소", example = "3층 회의실")
		private String location;

		@NotNull
		@Schema(description = "시작 일시 (반복 일정이면 첫 회차)", example = "2026-10-12T10:00:00")
		private LocalDateTime startAt;

		@NotNull
		@Schema(description = "종료 일시 (반복 일정이면 첫 회차)", example = "2026-10-12T11:00:00")
		private LocalDateTime endAt;

		@Schema(description = "종일 여부", example = "false")
		private Boolean isAllDay;

		@NotNull
		@Schema(description = "공개 범위 (PERSONAL, DEPARTMENT, COMPANY)", example = "PERSONAL")
		private ScheduleScope scope;

		@Schema(description = "반복 주기 (NONE, DAILY, WEEKLY, MONTHLY) — 생략 시 NONE", example = "NONE")
		private RepeatType repeatType;

		@Schema(description = "반복 종료일 — 생략 시 계속 반복", example = "2026-12-31")
		private LocalDate repeatUntilDt;

		@Schema(description = "참석자 직원 ID 목록", example = "[2, 3]")
		private List<Long> attendeeIds;
	}

	@Getter @Setter
	@Schema(name = "ScheduleDto.UpdateRequest")
	public static class UpdateRequest extends CreateRequest {

		@NotNull
		@Schema(description = "일정 ID", example = "1")
		private Long id;
	}

	@Getter @Setter
	@Schema(name = "ScheduleDto.DeleteRequest")
	public static class DeleteRequest {

		@NotNull
		@Schema(description = "일정 ID", example = "1")
		private Long id;
	}

	@Getter @Builder
	@Schema(name = "ScheduleDto.AttendeeResponse")
	public static class AttendeeResponse {

		@Schema(description = "직원 ID", example = "2")
		private Long id;

		@Schema(description = "이름", example = "홍길동")
		private String name;

		public static AttendeeResponse from(Employee employee) {
			return AttendeeResponse.builder().id(employee.getId()).name(employee.getName()).build();
		}
	}

	@Getter @Builder
	@Schema(name = "ScheduleDto.Response")
	public static class Response {

		@Schema(description = "일정 ID", example = "1")
		private Long id;

		@Schema(description = "작성자 ID", example = "1")
		private Long ownerId;

		@Schema(description = "작성자 이름", example = "홍길동")
		private String ownerName;

		@Schema(description = "제목", example = "주간 회의")
		private String title;

		@Schema(description = "설명")
		private String description;

		@Schema(description = "장소", example = "3층 회의실")
		private String location;

		@Schema(description = "이 회차의 시작 일시")
		private LocalDateTime startAt;

		@Schema(description = "이 회차의 종료 일시")
		private LocalDateTime endAt;

		@Schema(description = "일정 원본(첫 회차)의 시작 일시 — 수정 화면용")
		private LocalDateTime baseStartAt;

		@Schema(description = "일정 원본(첫 회차)의 종료 일시 — 수정 화면용")
		private LocalDateTime baseEndAt;

		@Schema(description = "종일 여부", example = "false")
		private Boolean isAllDay;

		@Schema(description = "공개 범위 (PERSONAL, DEPARTMENT, COMPANY)", example = "PERSONAL")
		private ScheduleScope scope;

		@Schema(description = "반복 주기 (NONE, DAILY, WEEKLY, MONTHLY)", example = "NONE")
		private RepeatType repeatType;

		@Schema(description = "반복 종료일")
		private LocalDate repeatUntilDt;

		@Schema(description = "참석자 목록")
		private List<AttendeeResponse> attendees;

		@Schema(description = "조회한 사용자가 수정·삭제할 수 있는지 여부", example = "true")
		private Boolean isEditable;

		// 반복 일정은 회차마다 시작·종료가 다르므로 회차의 시각을 따로 받는다
		public static Response of(Schedule schedule, LocalDateTime startAt, LocalDateTime endAt,
				List<AttendeeResponse> attendees, boolean editable) {
			return Response.builder()
					.id(schedule.getId())
					.ownerId(schedule.getOwner().getId())
					.ownerName(schedule.getOwner().getName())
					.title(schedule.getTitle())
					.description(schedule.getDescription())
					.location(schedule.getLocation())
					.startAt(startAt)
					.endAt(endAt)
					.baseStartAt(schedule.getStartAt())
					.baseEndAt(schedule.getEndAt())
					.isAllDay(schedule.getIsAllDay())
					.scope(schedule.getScope())
					.repeatType(schedule.getRepeatType())
					.repeatUntilDt(schedule.getRepeatUntilDt())
					.attendees(attendees)
					.isEditable(editable)
					.build();
		}
	}
}
