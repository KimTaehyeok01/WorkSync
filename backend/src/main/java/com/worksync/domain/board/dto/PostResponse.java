package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Board;
import com.worksync.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long id;
    private Long boardId;
    private String boardName;
    private Board.BoardType boardType;
    private Long authorId;
    private String authorName;
    private String authorProfileImage;
    private Employee.JobGrade authorJobGrade;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
