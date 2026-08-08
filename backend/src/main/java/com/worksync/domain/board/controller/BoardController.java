package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.BoardDto;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.service.BoardService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board", description = "게시판 API")
@Slf4j
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    //게시판 목록조회
    @Operation(summary = "게시판 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardDto.Response>>> getBoards(
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
    @Operation(summary = "게시판 단건 조회")
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardDto.Response>> getBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(ApiResponse.ok(boardService.getBoard(boardId)));
    }
}
