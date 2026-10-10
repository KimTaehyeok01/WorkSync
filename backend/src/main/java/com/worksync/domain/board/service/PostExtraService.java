// 게시글 부가 기능(상단 고정·읽음 확인·댓글) 서비스
package com.worksync.domain.board.service;

import com.worksync.domain.board.dto.PostDto;
import com.worksync.domain.board.dto.PostExtraDto;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.entity.PostComment;
import com.worksync.domain.board.entity.PostPin;
import com.worksync.domain.board.entity.PostRead;
import com.worksync.domain.board.repository.PostCommentRepository;
import com.worksync.domain.board.repository.PostPinRepository;
import com.worksync.domain.board.repository.PostReadRepository;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 게시글 부가 기능 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostExtraService {

	private final PostRepository postRepository;
	private final PostPinRepository postPinRepository;
	private final PostReadRepository postReadRepository;
	private final PostCommentRepository postCommentRepository;
	private final EmployeeRepository employeeRepository;

	// 게시판의 상단 고정 글 목록
	public List<PostDto.Response> getPinned(Long boardId) {
		return postPinRepository.findByBoardId(boardId).stream()
				.map(pin -> PostDto.Response.from(pin.getPost()))
				.toList();
	}

	// 상단 고정 설정·해제 (ADMIN)
	@Transactional
	public void setPinned(Long myId, Long postId, boolean pinned) {
		if (findEmployee(myId).getRole() != EmployeeRole.ADMIN) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
		Post post = findPost(postId);

		postPinRepository.findByPostId(postId).ifPresentOrElse(
				pin -> {
					if (!pinned) {
						postPinRepository.delete(pin);
					}
				},
				() -> {
					if (pinned) {
						postPinRepository.save(PostPin.builder().post(post).build());
					}
				});
	}

	// 읽음 기록 — 이미 읽은 글이면 아무것도 하지 않는다
	@Transactional
	public void markRead(Long myId, Long postId) {
		if (postReadRepository.existsByPostIdAndEmployeeId(postId, myId)) {
			return;
		}
		postReadRepository.save(PostRead.builder()
				.post(findPost(postId))
				.employee(findEmployee(myId))
				.build());
	}

	// 고정 여부·읽은 수 조회 — 읽은 사람 목록은 작성자와 ADMIN 에게만
	public PostExtraDto.StatusResponse getStatus(Long myId, Long postId) {
		Post post = findPost(postId);
		boolean isAdmin = findEmployee(myId).getRole() == EmployeeRole.ADMIN;
		boolean canSeeReaders = isAdmin || post.getAuthor().getId().equals(myId);

		return PostExtraDto.StatusResponse.builder()
				.isPinned(postPinRepository.findByPostId(postId).isPresent())
				.readCount(postReadRepository.countByPostId(postId))
				.isPinnable(isAdmin)
				.readers(canSeeReaders
						? postReadRepository.findReaders(postId).stream()
								.map(PostExtraDto.ReaderResponse::from)
								.toList()
						: List.of())
				.build();
	}

	// 댓글 목록
	public List<PostExtraDto.CommentResponse> getComments(Long myId, Long postId) {
		boolean isAdmin = findEmployee(myId).getRole() == EmployeeRole.ADMIN;
		return postCommentRepository.findByPostId(postId).stream()
				.map(comment -> PostExtraDto.CommentResponse.of(
						comment, isAdmin || comment.getAuthor().getId().equals(myId)))
				.toList();
	}

	// 댓글 작성
	@Transactional
	public PostExtraDto.CommentResponse createComment(Long myId, PostExtraDto.CommentCreateRequest request) {
		PostComment comment = postCommentRepository.save(PostComment.builder()
				.post(findPost(request.getPostId()))
				.author(findEmployee(myId))
				.content(request.getContent().trim())
				.build());
		return PostExtraDto.CommentResponse.of(comment, true);
	}

	// 댓글 삭제 — 작성자 또는 ADMIN
	@Transactional
	public void deleteComment(Long myId, Long commentId) {
		PostComment comment = postCommentRepository.findById(commentId)
				.orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
		boolean isAdmin = findEmployee(myId).getRole() == EmployeeRole.ADMIN;
		if (!isAdmin && !comment.getAuthor().getId().equals(myId)) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
		postCommentRepository.delete(comment);
	}

	private Post findPost(Long postId) {
		return postRepository.findById(postId)
				.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
	}

	private Employee findEmployee(Long employeeId) {
		return employeeRepository.findById(employeeId)
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
	}
}
