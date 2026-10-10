// 자원 예약 API 컨트롤러
package com.worksync.domain.reservation.controller;

import com.worksync.domain.reservation.dto.ReservationDto;
import com.worksync.domain.reservation.service.ReservationService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 자원 예약 API. */
@Tag(name = "Reservation", description = "자원 예약 API")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	// 일자별 전체 예약 조회 (시간표용)
	@Operation(summary = "일자별 전체 예약 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ReservationDto.Response>>> getReservations(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		return ResponseEntity.ok(ApiResponse.ok(
				reservationService.getReservations(userDetails.getId(), date)));
	}

	// 내 예정 예약 조회
	@Operation(summary = "내 예정 예약 조회")
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<List<ReservationDto.Response>>> getMyReservations(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		return ResponseEntity.ok(ApiResponse.ok(
				reservationService.getMyReservations(userDetails.getId())));
	}

	// 예약
	@Operation(summary = "자원 예약")
	@PostMapping
	public ResponseEntity<ApiResponse<ReservationDto.Response>> reserve(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid ReservationDto.CreateRequest request) {

		return ResponseEntity.status(201).body(ApiResponse.created(
				reservationService.reserve(userDetails.getId(), request)));
	}

	// 예약 취소 — 예약자 또는 ADMIN
	@Operation(summary = "예약 취소")
	@PostMapping("/cancel")
	public ResponseEntity<ApiResponse<Void>> cancel(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid ReservationDto.CancelRequest request) {

		reservationService.cancel(userDetails.getId(), request.getId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
