package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {

    Page<Post> findByBoardId(Long boardId, Pageable pageable);
    Page<Post> findByBoardIdAndTitleContaining(Long boardId,String keyword,Pageable pageable);
}
