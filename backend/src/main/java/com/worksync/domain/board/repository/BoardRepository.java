package com.worksync.domain.board.repository;

import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board,Long>  {
    List<Board> findByBoardType (BoardType boardType);
    List<Board> findByDepartmentId(Long departmentId);
    List<Board> findByBoardTypeAndDepartmentId(BoardType boardType,Long departmentId);

}
