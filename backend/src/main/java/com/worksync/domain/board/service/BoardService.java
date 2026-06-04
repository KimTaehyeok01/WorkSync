package com.worksync.domain.board.service;

import com.worksync.domain.board.dto.BoardResponse;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.repository.BoardRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;

    public List<BoardResponse> getBoards (BoardType boardType,Long departmentId){
        List<Board>boards;
        //boardType과 departmentId 둘다 있으면 둘다 필터링
        if(boardType !=null && departmentId !=null){
            boards=boardRepository.findByBoardTypeAndDepartmentId(boardType,departmentId);
            //boardType만 있으면 게시판 타입으로 필터링
        } else if (boardType !=null ){
            boards=boardRepository.findByBoardType(boardType);
            //departmentId 만 있으면 부서로 필터링
        } else if (departmentId !=null) {
            boards=boardRepository.findByDepartmentId(departmentId);
            //둘다 없으면 전체조회
        } else{
            boards=boardRepository.findAll();
        }

    return boards.stream()
            .map(BoardResponse::from)
            .toList();
    }

    //게시판 단건 조회
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long boardId){
        Board board=boardRepository.findById(boardId)
                .orElseThrow(()->new CustomException(ErrorCode.BOARD_NOT_FOUND));
        return BoardResponse.from(board);
    }
}





