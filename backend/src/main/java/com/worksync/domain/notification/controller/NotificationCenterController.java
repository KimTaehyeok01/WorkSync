// 알림 센터(전체 목록·읽음 처리·수신 설정) API 컨트롤러
package com.worksync.domain.notification.controller;

import com.worksync.domain.notification.dto.NotificationDto;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 알림 센터 API. */
@Tag(name = "NotificationCenter", description = "알림 센터 API")
@RestController
@RequestMapping("/api/notifications/center")
@RequiredArgsConstructor
public class NotificationCenterController {

	private static final int MAX_PAGE_SIZE = 50;

	private final NotificationService notificationService;

	// 알림 목록 — 유형·안 읽음 필터, 최신순 페이징
	@Operation(summary = "알림 센터 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationDto.Response>>> getCenter(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(required = false) NotificationType type,
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		return ResponseEntity.ok(ApiResponse.ok(notificationService.getCenter(
				userDetails.getId(), type, unreadOnly,
				PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("createdAt").descending()))));
	}

	// 알림 1건 읽음 처리
	@Operation(summary = "알림 1건 읽음 처리")
	@PostMapping("/read")
	public ResponseEntity<ApiResponse<Void>> readOne(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid NotificationDto.ReadRequest request) {

		notificationService.readOne(userDetails.getId(), request.getId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// 전체 읽음 처리
	@Operation(summary = "내 알림 전체 읽음 처리")
	@PostMapping("/read-all")
	public ResponseEntity<ApiResponse<Void>> readAll(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		notificationService.readAll(userDetails.getId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// 알림 수신 설정 조회
	@Operation(summary = "알림 수신 설정 조회")
	@GetMapping("/settings")
	public ResponseEntity<ApiResponse<List<NotificationDto.SettingResponse>>> getSettings(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		return ResponseEntity.ok(ApiResponse.ok(notificationService.getSettings(userDetails.getId())));
	}

	// 알림 수신 설정 변경
	@Operation(summary = "알림 수신 설정 변경")
	@PostMapping("/settings")
	public ResponseEntity<ApiResponse<Void>> updateSetting(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid NotificationDto.SettingRequest request) {

		notificationService.updateSetting(userDetails.getId(), request.getType(), request.getIsEnabled());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
