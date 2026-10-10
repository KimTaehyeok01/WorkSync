// 관리자용 근태 조회·수정 API 컨트롤러
package com.worksync.domain.attendance.controller;

import com.worksync.domain.attendance.dto.AttendanceDto;
import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 관리자용 근태 API. */
@Tag(name = "AttendanceAdmin", description = "관리자 근태 API")
@RestController
@RequestMapping("/api/attendance/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AttendanceAdminController {

	private final AttendanceService attendanceService;

	// 일자별 재직자 전원 근태 — 미출근자는 결근으로 표시
	@Operation(summary = "일자별 전 직원 근태 조회 (ADMIN 전용)")
	@GetMapping
	public ResponseEntity<ApiResponse<List<AttendanceDto.AdminResponse>>> getAllEmployeesByDate(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAllEmployeesByDate(date)));
	}

	// 근태 기록 수정 — 기록이 없으면 새로 만든다
	@Operation(summary = "근태 기록 수정 (ADMIN 전용)")
	@PostMapping("/update")
	public ResponseEntity<ApiResponse<AttendanceDto.AdminResponse>> adminUpdate(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid AttendanceDto.AdminUpdateRequest request) {

		return ResponseEntity.ok(ApiResponse.ok(
				attendanceService.adminUpdate(userDetails.getId(), request)));
	}
}
