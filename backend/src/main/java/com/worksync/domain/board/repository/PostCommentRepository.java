// 게시글 댓글 Repository
package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 게시글 댓글 Repository. */
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

	// 게시글의 댓글 — 작성 순
	@Query("SELECT c FROM PostComment c JOIN FETCH c.author " +
			"WHERE c.post.id = :postId ORDER BY c.createdAt")
	List<PostComment> findByPostId(@Param("postId") Long postId);
}
