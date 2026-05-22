package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.BoardResponse;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    //게시판 목록조회
    @GetMapping
    public  ResponseEntity<List<BoardResponse>>getBoards(
            @RequestParam(required=false)String boardType,
            @RequestParam(required = false)Long departmentId){
        BoardType type=null;
        if(boardType!=null){
            type=BoardType.valueOf(boardType);
        }
        return ResponseEntity.ok(boardService.getBoards(type,departmentId));
    }

    //게시판 단건 조회
    @GetMapping("/{boardId}")
    public  ResponseEntity<BoardResponse>getBoard(@PathVariable Long boardId){
        return ResponseEntity.ok(boardService.getBoard(boardId));
    }
}
