// 게시글 상단 고정 Repository
package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.PostPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 게시글 상단 고정 Repository. */
public interface PostPinRepository extends JpaRepository<PostPin, Long> {

	Optional<PostPin> findByPostId(Long postId);

	// 게시판의 고정 글 — 최근에 고정한 순
	@Query("SELECT p FROM PostPin p JOIN FETCH p.post post JOIN FETCH post.author JOIN FETCH post.board " +
			"WHERE post.board.id = :boardId ORDER BY p.createdAt DESC")
	List<PostPin> findByBoardId(@Param("boardId") Long boardId);
}
