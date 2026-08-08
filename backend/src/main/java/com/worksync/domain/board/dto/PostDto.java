// 게시글 도메인 요청/응답 DTO 모음
package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PostDto {

    @Getter @Setter
    @Schema(name = "PostDto.CreateRequest")
    public static class CreateRequest {

        @NotNull
        @Schema(description = "게시판 ID", example = "1")
        private Long boardId;

        @NotBlank
        @Schema(description = "게시글 제목", example = "주간 회의 안내")
        private String title;

        @NotBlank
        @Schema(description = "게시글 내용")
        private String content;
    }

    @Getter @Builder
    @Schema(name = "PostDto.Response")
    public static class Response {

        @Schema(description = "게시글 ID", example = "1")
        private Long id;

        @Schema(description = "게시판 ID", example = "1")
        private Long boardId;

        @Schema(description = "게시판명", example = "공지사항")
        private String boardName;

        @Schema(description = "작성자 ID", example = "1")
        private Long authorId;

        @Schema(description = "작성자 이름", example = "홍길동")
        private String authorName;

        @Schema(description = "작성자 부서명 (무소속이면 null)", example = "개발팀")
        private String authorDepartmentName;

        @Schema(description = "게시글 제목", example = "주간 회의 안내")
        private String title;

        @Schema(description = "게시글 내용")
        private String content;

        @Schema(description = "작성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시")
        private LocalDateTime updatedAt;

        public static Response from(Post post) {
            return Response.builder()
                    .id(post.getId())
                    .boardId(post.getBoard().getId())
                    .boardName(post.getBoard().getName())
                    .authorId(post.getAuthor().getId())
                    .authorName(post.getAuthor().getName())
                    .authorDepartmentName(post.getAuthor().getDepartment() != null
                            ? post.getAuthor().getDepartment().getName() : null)
                    .title(post.getTitle())
                    .content(post.getContent())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .build();
        }
    }

    @Getter @Setter
    @Schema(name = "PostDto.UpdateRequest")
    public static class UpdateRequest {

        @NotBlank
        @Schema(description = "게시글 제목", example = "주간 회의 안내 (수정)")
        private String title;

        @NotBlank
        @Schema(description = "게시글 내용")
        private String content;
    }
}
