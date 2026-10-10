// 자원 예약 도메인 요청/응답 DTO 모음
package com.worksync.domain.reservation.dto;

import com.worksync.domain.reservation.entity.Reservation;
import com.worksync.domain.reservation.entity.Resource;
import com.worksync.domain.reservation.entity.ResourceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 자원 예약 DTO 모음. */
public class ReservationDto {

	@Getter @Setter
	@Schema(name = "ReservationDto.ResourceCreateRequest")
	public static class ResourceCreateRequest {

		@NotBlank
		@Size(max = 50)
		@Schema(description = "자원명", example = "3층 대회의실")
		private String name;

		@NotNull
		@Schema(description = "분류 (MEETING_ROOM, VEHICLE, EQUIPMENT)", example = "MEETING_ROOM")
		private ResourceCategory category;

		@Size(max = 100)
		@Schema(description = "위치", example = "본관 3층")
		private String location;

		@Positive
		@Schema(description = "수용 인원", example = "10")
		private Integer capacity;

		@Size(max = 255)
		@Schema(description = "설명")
		private String description;
	}

	@Getter @Setter
	@Schema(name = "ReservationDto.ResourceUpdateRequest")
	public static class ResourceUpdateRequest extends ResourceCreateRequest {

		@NotNull
		@Schema(description = "자원 ID", example = "1")
		private Long id;

		@NotNull
		@Schema(description = "사용 여부 (false면 신규 예약 불가)", example = "true")
		private Boolean isActive;
	}

	@Getter @Builder
	@Schema(name = "ReservationDto.ResourceResponse")
	public static class ResourceResponse {

		@Schema(description = "자원 ID", example = "1")
		private Long id;

		@Schema(description = "자원명", example = "3층 대회의실")
		private String name;

		@Schema(description = "분류 (MEETING_ROOM, VEHICLE, EQUIPMENT)", example = "MEETING_ROOM")
		private ResourceCategory category;

		@Schema(description = "위치", example = "본관 3층")
		private String location;

		@Schema(description = "수용 인원", example = "10")
		private Integer capacity;

		@Schema(description = "설명")
		private String description;

		@Schema(description = "사용 여부", example = "true")
		private Boolean isActive;

		public static ResourceResponse from(Resource resource) {
			return ResourceResponse.builder()
					.id(resource.getId())
					.name(resource.getName())
					.category(resource.getCategory())
					.location(resource.getLocation())
					.capacity(resource.getCapacity())
					.description(resource.getDescription())
					.isActive(resource.getIsActive())
					.build();
		}
	}

	@Getter @Setter
	@Schema(name = "ReservationDto.CreateRequest")
	public static class CreateRequest {

		@NotNull
		@Schema(description = "자원 ID", example = "1")
		private Long resourceId;

		@NotBlank
		@Size(max = 100)
		@Schema(description = "예약 목적", example = "주간 회의")
		private String title;

		@NotNull
		@Schema(description = "시작 일시", example = "2026-10-12T10:00:00")
		private LocalDateTime startAt;

		@NotNull
		@Schema(description = "종료 일시", example = "2026-10-12T11:00:00")
		private LocalDateTime endAt;
	}

	@Getter @Setter
	@Schema(name = "ReservationDto.CancelRequest")
	public static class CancelRequest {

		@NotNull
		@Schema(description = "예약 ID", example = "1")
		private Long id;
	}

	@Getter @Builder
	@Schema(name = "ReservationDto.Response")
	public static class Response {

		@Schema(description = "예약 ID", example = "1")
		private Long id;

		@Schema(description = "자원 ID", example = "1")
		private Long resourceId;

		@Schema(description = "자원명", example = "3층 대회의실")
		private String resourceName;

		@Schema(description = "예약자 ID", example = "1")
		private Long employeeId;

		@Schema(description = "예약자 이름", example = "홍길동")
		private String employeeName;

		@Schema(description = "예약 목적", example = "주간 회의")
		private String title;

		@Schema(description = "시작 일시")
		private LocalDateTime startAt;

		@Schema(description = "종료 일시")
		private LocalDateTime endAt;

		@Schema(description = "조회한 사용자가 취소할 수 있는지 여부", example = "true")
		private Boolean isCancelable;

		public static Response of(Reservation reservation, boolean cancelable) {
			return Response.builder()
					.id(reservation.getId())
					.resourceId(reservation.getResource().getId())
					.resourceName(reservation.getResource().getName())
					.employeeId(reservation.getEmployee().getId())
					.employeeName(reservation.getEmployee().getName())
					.title(reservation.getTitle())
					.startAt(reservation.getStartAt())
					.endAt(reservation.getEndAt())
					.isCancelable(cancelable)
					.build();
		}
	}
}
