// 관리자용 직원 기능(CSV 일괄 등록·발령 이력) API 컨트롤러
package com.worksync.domain.employee.controller;

import com.worksync.domain.employee.dto.EmployeeAdminDto;
import com.worksync.domain.employee.service.EmployeeAdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자용 직원 기능 API. */
@Tag(name = "EmployeeAdmin", description = "직원 일괄 등록·발령 이력 API")
@RestController
@RequestMapping("/api/employees/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EmployeeAdminController {

	private final EmployeeAdminService employeeAdminService;

	// 직원 일괄 등록 — 행별 성공·실패 결과를 돌려준다
	@Operation(summary = "직원 일괄 등록 (ADMIN 전용)")
	@PostMapping("/bulk")
	public ResponseEntity<ApiResponse<EmployeeAdminDto.BulkResponse>> bulkCreate(
			@RequestBody @Valid EmployeeAdminDto.BulkRequest request) {

		return ResponseEntity.ok(ApiResponse.ok(employeeAdminService.bulkCreate(request.getRows())));
	}

	// 인사 발령 이력 조회
	@Operation(summary = "인사 발령 이력 조회 (ADMIN 전용)")
	@GetMapping("/history")
	public ResponseEntity<ApiResponse<List<EmployeeAdminDto.HistoryResponse>>> getHistory(
			@RequestParam("employeeId") Long employeeId) {

		return ResponseEntity.ok(ApiResponse.ok(employeeAdminService.getHistory(employeeId)));
	}
}
