// 사내 메일 API 컨트롤러
package com.worksync.domain.mail.controller;

import com.worksync.domain.mail.dto.MailDto;
import com.worksync.domain.mail.service.MailService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 사내 메일 API. */
@Tag(name = "Mail", description = "사내 메일 API")
@RestController
@RequestMapping("/api/mails")
@RequiredArgsConstructor
public class MailController {

	private static final int MAX_PAGE_SIZE = 50;

	private final MailService mailService;

	// 받은편지함
	@Operation(summary = "받은편지함 조회")
	@GetMapping("/inbox")
	public ResponseEntity<ApiResponse<Page<MailDto.ListResponse>>> getInbox(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		return ResponseEntity.ok(ApiResponse.ok(mailService.getInbox(
				userDetails.getId(), unreadOnly, keyword, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)))));
	}

	// 보낸편지함
	@Operation(summary = "보낸편지함 조회")
	@GetMapping("/sent")
	public ResponseEntity<ApiResponse<Page<MailDto.ListResponse>>> getSent(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		return ResponseEntity.ok(ApiResponse.ok(mailService.getSent(
				userDetails.getId(), keyword, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)))));
	}

	// 안 읽은 메일 수
	@Operation(summary = "안 읽은 메일 수 조회")
	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		return ResponseEntity.ok(ApiResponse.ok(
				Map.of("unreadCount", mailService.getUnreadCount(userDetails.getId()))));
	}

	// 상세 조회 — 수신자가 처음 열면 읽음 처리
	@Operation(summary = "메일 상세 조회 (수신자는 읽음 처리)")
	@GetMapping("/detail")
	public ResponseEntity<ApiResponse<MailDto.DetailResponse>> getDetail(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("id") Long id) {

		return ResponseEntity.ok(ApiResponse.ok(mailService.getDetail(userDetails.getId(), id)));
	}

	// 메일 보내기
	@Operation(summary = "메일 보내기")
	@PostMapping
	public ResponseEntity<ApiResponse<MailDto.DetailResponse>> send(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid MailDto.SendRequest request) {

		return ResponseEntity.status(201).body(ApiResponse.created(
				mailService.send(userDetails.getId(), request)));
	}

	// 삭제 — 내 메일함에서만
	@Operation(summary = "메일 삭제 (내 메일함에서만)")
	@PostMapping("/delete")
	public ResponseEntity<ApiResponse<Void>> delete(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid MailDto.DeleteRequest request) {

		mailService.delete(userDetails.getId(), request.getId(), request.getBox());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
