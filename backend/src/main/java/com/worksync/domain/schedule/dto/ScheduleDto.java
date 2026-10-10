// 캘린더 일정 도메인 요청/응답 DTO 모음
package com.worksync.domain.schedule.dto;

import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
		@Schema(description = "시작 일시", example = "2026-10-12T10:00:00")
		private LocalDateTime startAt;

		@NotNull
		@Schema(description = "종료 일시", example = "2026-10-12T11:00:00")
		private LocalDateTime endAt;

		@Schema(description = "종일 여부", example = "false")
		private Boolean isAllDay;

		@NotNull
		@Schema(description = "공개 범위 (PERSONAL, DEPARTMENT, COMPANY)", example = "PERSONAL")
		private ScheduleScope scope;
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

		@Schema(description = "시작 일시")
		private LocalDateTime startAt;

		@Schema(description = "종료 일시")
		private LocalDateTime endAt;

		@Schema(description = "종일 여부", example = "false")
		private Boolean isAllDay;

		@Schema(description = "공개 범위 (PERSONAL, DEPARTMENT, COMPANY)", example = "PERSONAL")
		private ScheduleScope scope;

		@Schema(description = "조회한 사용자가 수정·삭제할 수 있는지 여부", example = "true")
		private Boolean isEditable;

		public static Response of(Schedule schedule, boolean editable) {
			return Response.builder()
					.id(schedule.getId())
					.ownerId(schedule.getOwner().getId())
					.ownerName(schedule.getOwner().getName())
					.title(schedule.getTitle())
					.description(schedule.getDescription())
					.location(schedule.getLocation())
					.startAt(schedule.getStartAt())
					.endAt(schedule.getEndAt())
					.isAllDay(schedule.getIsAllDay())
					.scope(schedule.getScope())
					.isEditable(editable)
					.build();
		}
	}
}
