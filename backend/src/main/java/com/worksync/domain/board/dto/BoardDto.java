// 게시판 도메인 요청/응답 DTO 모음
package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class BoardDto {

    @Getter @Builder
    @Schema(name = "BoardDto.Response")
    public static class Response {

        @Schema(description = "게시판 ID", example = "1")
        private Long id;

        @Schema(description = "게시판 유형 (NOTICE, DEPARTMENT, FREE, FILES)", example = "NOTICE")
        private BoardType boardType;

        @Schema(description = "게시판명", example = "공지사항")
        private String name;

        @Schema(description = "소속 부서 ID (부서 게시판이 아니면 null)", example = "1")
        private Long departmentId;

        @Schema(description = "소속 부서명 (부서 게시판이 아니면 null)", example = "개발팀")
        private String departmentName;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(Board board) {
            return Response.builder()
                    .id(board.getId())
                    .boardType(board.getBoardType())
                    .name(board.getName())
                    .departmentId(board.getDepartment() != null ? board.getDepartment().getId() : null)
                    .departmentName(board.getDepartment() != null ? board.getDepartment().getName() : null)
                    .createdAt(board.getCreatedAt())
                    .build();
        }
    }
}
