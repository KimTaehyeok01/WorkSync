package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class BoardResponse {

    private Long id;
    private BoardType boardType;
    private String name;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;

    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .boardType(board.getBoardType())
                .name(board.getName())
                .departmentId(board.getDepartment() != null ? board.getDepartment().getId() : null)
                .departmentName(board.getDepartment() != null ? board.getDepartment().getName() : null)
                .createdAt(board.getCreatedAt())
                .build();
    }
}
