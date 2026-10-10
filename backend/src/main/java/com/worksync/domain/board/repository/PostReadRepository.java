// 게시글 읽음 확인 Repository
package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.PostRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 게시글 읽음 확인 Repository. */
public interface PostReadRepository extends JpaRepository<PostRead, Long> {

	boolean existsByPostIdAndEmployeeId(Long postId, Long employeeId);

	long countByPostId(Long postId);

	// 읽은 사람 목록 — 먼저 읽은 순
	@Query("SELECT r FROM PostRead r JOIN FETCH r.employee e LEFT JOIN FETCH e.department " +
			"WHERE r.post.id = :postId ORDER BY r.createdAt")
	List<PostRead> findReaders(@Param("postId") Long postId);
}
