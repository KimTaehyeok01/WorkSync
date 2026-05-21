package com.worksync.domain.board.service;

import com.worksync.domain.board.dto.BoardResponse;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    public List<BoardResponse> getBoards (BoardType boardType){
        List<Board>boards;
        if(boardType !=null){
            boards=boardRepository.findByBoardType(boardType);
        }
        else{
            boards=boardRepository.findAll();
        }
    return boards.stream()
            .map(board->{
                Long departmentId=null;
                String departmentName=null;
                if(board.getDepartment() !=null){
                    departmentId=board.getDepartment().getId();
                    departmentName=board.getDepartment().getName();
                }
                return BoardResponse.builder()
                        .id(board.getId())
                        .boardType(board.getBoardType())
                        .name(board.getName())
                        .departmentId(departmentId)
                        .departmentName(departmentName)
                        .createdAt(board.getCreatedAt())
                        .build();
            })
            .toList();
    }

}
