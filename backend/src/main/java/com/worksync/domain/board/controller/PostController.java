package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.PostCreateRequest;
import com.worksync.domain.board.dto.PostResponse;
import com.worksync.domain.board.dto.PostUpdateRequest;
import com.worksync.domain.board.service.PostService;
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

    //게시글 목록 조회
    @GetMapping("/{boardId}/posts")
    public ResponseEntity<Page<PostResponse>>getPosts(
            @PathVariable Long boardId,
            @RequestParam(required = false)String keyword,
    Pageable pageable)     {
    return ResponseEntity.ok(postService.getPosts(boardId,keyword,pageable));
    }

    //게시글 상세 조회
    @GetMapping("/{boardId}/posts/{postId}")
    public  ResponseEntity<PostResponse>getPost(
            @PathVariable Long boardId,
            @PathVariable Long postId){
        return ResponseEntity.ok(postService.getPost(boardId, postId));
    }

    //게시글 작성
    @PostMapping("/{boardId}/posts")
    public  ResponseEntity<Long>createPost(
            @PathVariable Long boardId,
            @RequestBody @Valid PostCreateRequest req,
            @AuthenticationPrincipal CustomUserDetails user){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(boardId,req,user));
    }

    //게시글 수정
    @PutMapping("/{boardId}/posts/{postId}")
    public  ResponseEntity<PostResponse> updatePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails user){
        return ResponseEntity.ok(postService.updatePost(boardId, postId, req, user));
    }
    //게시글 삭제
    @DeleteMapping ("/{boardId}/posts/{postId}")
    public ResponseEntity<Void>deletePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user){
        postService.deletePost(boardId, postId, user);
        return ResponseEntity.noContent().build();
    }
}
