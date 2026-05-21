package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponse {

    private Long id;
    private Board.BoardType boardType;
    private String name;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
}
