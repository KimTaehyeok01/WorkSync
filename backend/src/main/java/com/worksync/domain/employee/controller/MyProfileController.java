// 마이페이지용 본인 프로필·비밀번호 변경 API 컨트롤러
package com.worksync.domain.employee.controller;

import com.worksync.domain.employee.dto.EmployeeDto;
import com.worksync.domain.employee.service.EmployeeService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지용 본인 프로필·비밀번호 변경 API. */
@Tag(name = "MyProfile", description = "마이페이지 API")
@RestController
@RequestMapping("/api/employees/me")
@RequiredArgsConstructor
public class MyProfileController {

	private final EmployeeService employeeService;

	// 내 프로필 수정 — 로그인한 본인의 연락처·프로필 이미지 변경
	@Operation(summary = "내 프로필 수정 (연락처·프로필 이미지)")
	@PostMapping("/profile")
	public ResponseEntity<ApiResponse<EmployeeDto.Response>> updateMyProfile(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid EmployeeDto.MyProfileRequest request) {

		return ResponseEntity.ok(ApiResponse.ok(
				employeeService.updateMyProfile(userDetails.getId(), request)));
	}

	// 내 비밀번호 변경 — 현재 비밀번호 확인 후 변경
	@Operation(summary = "내 비밀번호 변경")
	@PostMapping("/password")
	public ResponseEntity<ApiResponse<Void>> changeMyPassword(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid EmployeeDto.PasswordChangeRequest request) {

		employeeService.changeMyPassword(userDetails.getId(), request);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
