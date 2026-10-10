package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {

    Page<Post> findByBoardId(Long boardId, Pageable pageable);
    Page<Post> findByBoardIdAndTitleContaining(Long boardId, String keyword, Pageable pageable);

    // 부서게시판 전용 - 작성자 부서 기준 필터링
    Page<Post> findByBoardIdAndAuthorDepartmentId(Long boardId, Long departmentId, Pageable pageable);
    Page<Post> findByBoardIdAndTitleContainingAndAuthorDepartmentId(Long boardId, String keyword, Long departmentId, Pageable pageable);

    // ADMIN 전용 - 특정 타입(DEPARTMENT)의 모든 게시판 글 조회
    Page<Post> findByBoardBoardType(BoardType boardType, Pageable pageable);
    Page<Post> findByBoardBoardTypeAndTitleContaining(BoardType boardType, String keyword, Pageable pageable);

    // ADMIN 전용 - 특정 타입(DEPARTMENT) + 특정 부서 작성자 글 조회
    Page<Post> findByBoardBoardTypeAndAuthorDepartmentId(BoardType boardType, Long departmentId, Pageable pageable);
    Page<Post> findByBoardBoardTypeAndTitleContainingAndAuthorDepartmentId(BoardType boardType, String keyword, Long departmentId, Pageable pageable);

    // 통합 검색 — 제목이 일치하는 글 중 볼 수 있는 것
    // 부서게시판 글은 관리자이거나 작성자와 같은 부서일 때만 포함한다
    @Query("SELECT p FROM Post p JOIN FETCH p.board b JOIN FETCH p.author a " +
            "WHERE p.title LIKE CONCAT('%', :keyword, '%') " +
            "AND (b.boardType <> :departmentType OR :isAdmin = true OR a.department.id = :departmentId) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchVisible(@Param("keyword") String keyword,
                             @Param("departmentType") BoardType departmentType,
                             @Param("isAdmin") boolean isAdmin,
                             @Param("departmentId") Long departmentId,
                             Pageable pageable);
}
