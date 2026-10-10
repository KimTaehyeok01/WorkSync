// 게시글 부가 기능(상단 고정·읽음 확인·댓글) 요청/응답 DTO 모음
package com.worksync.domain.board.dto;

import com.worksync.domain.board.entity.PostComment;
import com.worksync.domain.board.entity.PostRead;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글 부가 기능 DTO 모음. */
public class PostExtraDto {

	@Getter @Setter
	@Schema(name = "PostExtraDto.PinRequest")
	public static class PinRequest {

		@NotNull
		@Schema(description = "게시글 ID", example = "1")
		private Long postId;

		@NotNull
		@Schema(description = "고정 여부", example = "true")
		private Boolean isPinned;
	}

	@Getter @Setter
	@Schema(name = "PostExtraDto.PostIdRequest")
	public static class PostIdRequest {

		@NotNull
		@Schema(description = "게시글 ID", example = "1")
		private Long postId;
	}

	@Getter @Builder
	@Schema(name = "PostExtraDto.ReaderResponse")
	public static class ReaderResponse {

		@Schema(description = "직원 ID", example = "1")
		private Long employeeId;

		@Schema(description = "이름", example = "홍길동")
		private String name;

		@Schema(description = "부서명", example = "개발팀")
		private String departmentName;

		@Schema(description = "읽은 일시")
		private LocalDateTime readAt;

		public static ReaderResponse from(PostRead read) {
			return ReaderResponse.builder()
					.employeeId(read.getEmployee().getId())
					.name(read.getEmployee().getName())
					.departmentName(read.getEmployee().getDepartment() != null
							? read.getEmployee().getDepartment().getName() : null)
					.readAt(read.getCreatedAt())
					.build();
		}
	}

	@Getter @Builder
	@Schema(name = "PostExtraDto.StatusResponse")
	public static class StatusResponse {

		@Schema(description = "상단 고정 여부", example = "false")
		private Boolean isPinned;

		@Schema(description = "읽은 사람 수", example = "12")
		private Long readCount;

		@Schema(description = "조회한 사용자가 고정을 바꿀 수 있는지 (ADMIN)", example = "false")
		private Boolean isPinnable;

		@Schema(description = "읽은 사람 목록 — 작성자·ADMIN 에게만 내려가고 그 외에는 빈 목록")
		private List<ReaderResponse> readers;
	}

	@Getter @Setter
	@Schema(name = "PostExtraDto.CommentCreateRequest")
	public static class CommentCreateRequest {

		@NotNull
		@Schema(description = "게시글 ID", example = "1")
		private Long postId;

		@NotBlank
		@Size(max = 1000)
		@Schema(description = "댓글 내용")
		private String content;
	}

	@Getter @Setter
	@Schema(name = "PostExtraDto.CommentDeleteRequest")
	public static class CommentDeleteRequest {

		@NotNull
		@Schema(description = "댓글 ID", example = "1")
		private Long id;
	}

	@Getter @Builder
	@Schema(name = "PostExtraDto.CommentResponse")
	public static class CommentResponse {

		@Schema(description = "댓글 ID", example = "1")
		private Long id;

		@Schema(description = "작성자 ID", example = "1")
		private Long authorId;

		@Schema(description = "작성자 이름", example = "홍길동")
		private String authorName;

		@Schema(description = "작성자 프로필 이미지 경로")
		private String authorProfileImage;

		@Schema(description = "내용")
		private String content;

		@Schema(description = "작성 일시")
		private LocalDateTime createdAt;

		@Schema(description = "조회한 사용자가 삭제할 수 있는지 (작성자·ADMIN)", example = "true")
		private Boolean isDeletable;

		public static CommentResponse of(PostComment comment, boolean deletable) {
			return CommentResponse.builder()
					.id(comment.getId())
					.authorId(comment.getAuthor().getId())
					.authorName(comment.getAuthor().getName())
					.authorProfileImage(comment.getAuthor().getProfileImage())
					.content(comment.getContent())
					.createdAt(comment.getCreatedAt())
					.isDeletable(deletable)
					.build();
		}
	}
}
