package com.worksync.domain.board.controller;

import com.worksync.domain.board.dto.BoardResponse;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    @GetMapping
    public  ResponseEntity<List<BoardResponse>>getBoards(
            @RequestParam(required=false)String boardType){
        BoardType type=null;
        if(boardType!=null){
            type=BoardType.valueOf(boardType);
        }
        return ResponseEntity.ok(boardService.getBoards(type));
    }
}
