// 예약 대상 자원 관리 API 컨트롤러
package com.worksync.domain.reservation.controller;

import com.worksync.domain.reservation.dto.ReservationDto;
import com.worksync.domain.reservation.service.ReservationService;
import com.worksync.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 예약 대상 자원 관리 API. */
@Tag(name = "Resource", description = "예약 자원 API")
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

	private final ReservationService reservationService;

	// 자원 목록 조회
	@Operation(summary = "자원 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ReservationDto.ResourceResponse>>> getResources() {
		return ResponseEntity.ok(ApiResponse.ok(reservationService.getResources()));
	}

	// 자원 등록 (관리자 전용)
	@Operation(summary = "자원 등록 (ADMIN 전용)")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ReservationDto.ResourceResponse>> createResource(
			@RequestBody @Valid ReservationDto.ResourceCreateRequest request) {

		return ResponseEntity.status(201).body(ApiResponse.created(
				reservationService.createResource(request)));
	}

	// 자원 수정 — 사용 여부 변경 포함 (관리자 전용)
	@Operation(summary = "자원 수정 (ADMIN 전용)")
	@PostMapping("/update")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ReservationDto.ResourceResponse>> updateResource(
			@RequestBody @Valid ReservationDto.ResourceUpdateRequest request) {

		return ResponseEntity.ok(ApiResponse.ok(reservationService.updateResource(request)));
	}
}
