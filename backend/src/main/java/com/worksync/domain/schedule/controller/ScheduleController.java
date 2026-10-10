// 캘린더 일정 API 컨트롤러
package com.worksync.domain.schedule.controller;

import com.worksync.domain.schedule.dto.ScheduleDto;
import com.worksync.domain.schedule.service.ScheduleService;
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

/** 캘린더 일정 API. */
@Tag(name = "Schedule", description = "캘린더 일정 API")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;

	// 기간 내 일정 조회 — 내 개인 일정 + 내 부서 일정 + 전사 일정
	@Operation(summary = "기간 내 일정 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ScheduleDto.Response>>> getSchedules(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

		return ResponseEntity.ok(ApiResponse.ok(
				scheduleService.getSchedules(userDetails.getId(), from, to)));
	}

	// 일정 등록
	@Operation(summary = "일정 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<ScheduleDto.Response>> create(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid ScheduleDto.CreateRequest request) {

		return ResponseEntity.status(201).body(ApiResponse.created(
				scheduleService.create(userDetails.getId(), request)));
	}

	// 일정 수정 — 작성자 또는 ADMIN
	@Operation(summary = "일정 수정")
	@PostMapping("/update")
	public ResponseEntity<ApiResponse<ScheduleDto.Response>> update(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid ScheduleDto.UpdateRequest request) {

		return ResponseEntity.ok(ApiResponse.ok(
				scheduleService.update(userDetails.getId(), request)));
	}

	// 일정 삭제 — 작성자 또는 ADMIN
	@Operation(summary = "일정 삭제")
	@PostMapping("/delete")
	public ResponseEntity<ApiResponse<Void>> delete(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid ScheduleDto.DeleteRequest request) {

		scheduleService.delete(userDetails.getId(), request.getId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
