// BoardService 단위 테스트
package com.worksync.domain.board.service;

import com.worksync.domain.board.dto.BoardDto;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.repository.BoardRepository;
import com.worksync.domain.department.entity.Department;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** BoardService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    private Department buildDepartment(Long id, String name) {
        return Department.builder().id(id).name(name).build();
    }

    private Board buildBoard(Long id, BoardType type, String name, Department department) {
        return Board.builder().id(id).boardType(type).name(name).department(department).build();
    }

    @DisplayName("게시판 유형과 부서를 모두 지정하면 두 조건으로 필터링한 목록을 반환한다")
    @Test
    void getBoards_bothFilters_returnsFilteredList() {
        // given
        Department dept = buildDepartment(1L, "개발팀");
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "개발팀 게시판", dept);
        given(boardRepository.findByBoardTypeAndDepartmentId(BoardType.DEPARTMENT, 1L))
                .willReturn(List.of(board));

        // when
        List<BoardDto.Response> result = boardService.getBoards(BoardType.DEPARTMENT, 1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(boardRepository).findByBoardTypeAndDepartmentId(BoardType.DEPARTMENT, 1L);
        verify(boardRepository, never()).findAll();
    }

    @DisplayName("게시판 유형만 지정하면 유형으로 필터링한 목록을 반환한다")
    @Test
    void getBoards_boardTypeOnly_returnsFilteredList() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        given(boardRepository.findByBoardType(BoardType.FREE)).willReturn(List.of(board));

        // when
        List<BoardDto.Response> result = boardService.getBoards(BoardType.FREE, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoardType()).isEqualTo(BoardType.FREE);
        verify(boardRepository).findByBoardType(BoardType.FREE);
    }

    @DisplayName("부서만 지정하면 부서로 필터링한 목록을 반환한다")
    @Test
    void getBoards_departmentIdOnly_returnsFilteredList() {
        // given
        Department dept = buildDepartment(1L, "개발팀");
        Board board = buildBoard(3L, BoardType.DEPARTMENT, "개발팀 게시판", dept);
        given(boardRepository.findByDepartmentId(1L)).willReturn(List.of(board));

        // when
        List<BoardDto.Response> result = boardService.getBoards(null, 1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartmentId()).isEqualTo(1L);
        verify(boardRepository).findByDepartmentId(1L);
    }

    @DisplayName("조건이 없으면 전체 게시판 목록을 반환한다")
    @Test
    void getBoards_noFilters_returnsAll() {
        // given
        Board board1 = buildBoard(1L, BoardType.NOTICE, "공지사항", null);
        Board board2 = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        given(boardRepository.findAll()).willReturn(List.of(board1, board2));

        // when
        List<BoardDto.Response> result = boardService.getBoards(null, null);

        // then
        assertThat(result).hasSize(2);
        verify(boardRepository).findAll();
    }

    @DisplayName("게시판을 단건 조회하면 정상적으로 응답한다")
    @Test
    void getBoard_success() {
        // given
        Department dept = buildDepartment(1L, "개발팀");
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "개발팀 게시판", dept);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardDto.Response result = boardService.getBoard(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("개발팀 게시판");
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        assertThat(result.getDepartmentName()).isEqualTo("개발팀");
    }

    @DisplayName("존재하지 않는 게시판을 조회하면 예외가 발생한다")
    @Test
    void getBoard_notFound_throwsBoardNotFound() {
        // given
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.getBoard(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @DisplayName("소속 부서가 없는 게시판을 조회해도 NPE 없이 부서 정보가 null로 응답된다")
    @Test
    void getBoard_withNullDepartment_dtoSafe() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        given(boardRepository.findById(2L)).willReturn(Optional.of(board));

        // when
        BoardDto.Response result = boardService.getBoard(2L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getDepartmentName()).isNull();
    }
}
