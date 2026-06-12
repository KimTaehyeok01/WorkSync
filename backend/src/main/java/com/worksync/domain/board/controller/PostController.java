package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.PostCreateRequest;
import com.worksync.domain.board.dto.PostResponse;
import com.worksync.domain.board.dto.PostUpdateRequest;
import com.worksync.domain.board.service.PostService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class PostController {
    private  final PostService postService;

    //게시글 목록 조회 (departmentId: ADMIN이 부서게시판에서 특정 부서만 필터링할 때)
    @GetMapping("/{boardId}/posts")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPosts(
            @PathVariable Long boardId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPosts(boardId, keyword, departmentId, pageable, user)));
    }

    //게시글 상세 조회
    @GetMapping("/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long boardId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(boardId, postId)));
    }

    //게시글 작성
    @PostMapping("/{boardId}/posts")
    public ResponseEntity<ApiResponse<Long>> createPost(
            @PathVariable Long boardId,
            @RequestBody @Valid PostCreateRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(postService.createPost(boardId, req, user)));
    }

    //게시글 수정
    @PutMapping("/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(postService.updatePost(boardId, postId, req, user)));
    }

    //게시글 삭제
    @DeleteMapping("/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user) {
        postService.deletePost(boardId, postId, user);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
