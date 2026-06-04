package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class PostResponse {

    private Long id;
    private Long boardId;
    private String boardName;
    private Long authorId;
    private String authorName;
    private String authorDepartmentName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
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
