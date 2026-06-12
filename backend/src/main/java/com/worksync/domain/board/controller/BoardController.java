package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.BoardResponse;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.service.BoardService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    //게시판 목록조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards(
            @RequestParam(required = false) String boardType,
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BoardType type = null;
        if (boardType != null) {
            type = BoardType.valueOf(boardType);
        }

        // 부서게시판 선택 시 departmentId 미전달이면 로그인한 사용자 부서로 자동 필터링
        if (type == BoardType.DEPARTMENT && departmentId == null) {
            var dept = userDetails.getEmployee().getDepartment();
            if (dept != null) {
                departmentId = dept.getId();
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoards(type, departmentId)));
    }

    //게시판 단건 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoard(boardId)));
    }
}
