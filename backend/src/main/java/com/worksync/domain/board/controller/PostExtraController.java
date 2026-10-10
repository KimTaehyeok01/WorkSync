// 게시글 부가 기능(상단 고정·읽음 확인·댓글) API 컨트롤러
package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.PostDto;
import com.worksync.domain.board.dto.PostExtraDto;
import com.worksync.domain.board.service.PostExtraService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 게시글 부가 기능 API. */
@Tag(name = "PostExtra", description = "게시글 상단 고정·읽음 확인·댓글 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostExtraController {

	private final PostExtraService postExtraService;

	// 게시판의 상단 고정 글
	@Operation(summary = "상단 고정 글 목록 조회")
	@GetMapping("/pinned")
	public ResponseEntity<ApiResponse<List<PostDto.Response>>> getPinned(
			@RequestParam("boardId") Long boardId) {

		return ResponseEntity.ok(ApiResponse.ok(postExtraService.getPinned(boardId)));
	}

	// 상단 고정 설정·해제 (ADMIN)
	@Operation(summary = "상단 고정 설정·해제 (ADMIN)")
	@PostMapping("/pin")
	public ResponseEntity<ApiResponse<Void>> setPinned(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid PostExtraDto.PinRequest request) {

		postExtraService.setPinned(userDetails.getId(), request.getPostId(), request.getIsPinned());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// 읽음 기록
	@Operation(summary = "게시글 읽음 기록")
	@PostMapping("/read")
	public ResponseEntity<ApiResponse<Void>> markRead(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid PostExtraDto.PostIdRequest request) {

		postExtraService.markRead(userDetails.getId(), request.getPostId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	// 고정 여부·읽은 수·읽은 사람
	@Operation(summary = "게시글 고정 여부·읽음 현황 조회")
	@GetMapping("/status")
	public ResponseEntity<ApiResponse<PostExtraDto.StatusResponse>> getStatus(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("postId") Long postId) {

		return ResponseEntity.ok(ApiResponse.ok(postExtraService.getStatus(userDetails.getId(), postId)));
	}

	// 댓글 목록
	@Operation(summary = "댓글 목록 조회")
	@GetMapping("/comments")
	public ResponseEntity<ApiResponse<List<PostExtraDto.CommentResponse>>> getComments(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("postId") Long postId) {

		return ResponseEntity.ok(ApiResponse.ok(postExtraService.getComments(userDetails.getId(), postId)));
	}

	// 댓글 작성
	@Operation(summary = "댓글 작성")
	@PostMapping("/comments")
	public ResponseEntity<ApiResponse<PostExtraDto.CommentResponse>> createComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid PostExtraDto.CommentCreateRequest request) {

		return ResponseEntity.status(201).body(ApiResponse.created(
				postExtraService.createComment(userDetails.getId(), request)));
	}

	// 댓글 삭제 — 작성자 또는 ADMIN
	@Operation(summary = "댓글 삭제")
	@PostMapping("/comments/delete")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid PostExtraDto.CommentDeleteRequest request) {

		postExtraService.deleteComment(userDetails.getId(), request.getId());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
