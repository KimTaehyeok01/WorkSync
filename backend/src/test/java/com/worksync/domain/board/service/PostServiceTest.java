// PostService 단위 테스트
package com.worksync.domain.board.service;

import com.worksync.domain.board.dto.PostDto;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.repository.BoardRepository;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** PostService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PostService postService;

    private final Pageable pageable = PageRequest.of(0, 10);

    private Department buildDepartment(Long id, String name) {
        return Department.builder().id(id).name(name).build();
    }

    private Employee buildEmployee(Long id, String name, EmployeeRole role, Department department) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .role(role)
                .department(department)
                .build();
    }

    private Board buildBoard(Long id, BoardType type, String name, Department department) {
        return Board.builder().id(id).boardType(type).name(name).department(department).build();
    }

    private Post buildPost(Long id, Board board, Employee author, String title, String content) {
        return Post.builder().id(id).board(board).author(author).title(title).content(content).build();
    }

    // ==================== getPosts ====================

    @DisplayName("존재하지 않는 게시판의 게시글 목록을 조회하면 예외가 발생한다")
    @Test
    void getPosts_boardNotFound_throwsBoardNotFound() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPosts(1L, null, null, pageable, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @DisplayName("ADMIN이 부서게시판에서 부서와 검색어를 지정하면 부서+제목으로 검색한다")
    @Test
    void getPosts_departmentBoard_adminWithDepartmentIdAndKeyword_usesAdminFilteredSearch() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(9L, "인사팀"));
        Employee admin = buildEmployee(10L, "관리자", EmployeeRole.ADMIN, null);
        CustomUserDetails user = new CustomUserDetails(admin);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardBoardTypeAndTitleContainingAndAuthorDepartmentId(
                BoardType.DEPARTMENT, "공지", 2L, pageable)).willReturn(page);

        // when
        postService.getPosts(1L, "공지", 2L, pageable, user);

        // then
        verify(postRepository).findByBoardBoardTypeAndTitleContainingAndAuthorDepartmentId(
                BoardType.DEPARTMENT, "공지", 2L, pageable);
    }

    @DisplayName("ADMIN이 부서게시판에서 부서만 지정하면 부서 전체 글을 조회한다")
    @Test
    void getPosts_departmentBoard_adminWithDepartmentIdNoKeyword_usesAdminFilteredList() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(9L, "인사팀"));
        Employee admin = buildEmployee(10L, "관리자", EmployeeRole.ADMIN, null);
        CustomUserDetails user = new CustomUserDetails(admin);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardBoardTypeAndAuthorDepartmentId(BoardType.DEPARTMENT, 2L, pageable))
                .willReturn(page);

        // when
        postService.getPosts(1L, null, 2L, pageable, user);

        // then
        verify(postRepository).findByBoardBoardTypeAndAuthorDepartmentId(BoardType.DEPARTMENT, 2L, pageable);
    }

    @DisplayName("ADMIN이 부서 지정 없이 검색어만 지정하면 전체 부서게시판 대상으로 제목 검색한다")
    @Test
    void getPosts_departmentBoard_adminNoDepartmentIdWithKeyword_usesAdminAllSearch() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(9L, "인사팀"));
        Employee admin = buildEmployee(10L, "관리자", EmployeeRole.ADMIN, null);
        CustomUserDetails user = new CustomUserDetails(admin);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardBoardTypeAndTitleContaining(BoardType.DEPARTMENT, "공지", pageable))
                .willReturn(page);

        // when
        postService.getPosts(1L, "공지", null, pageable, user);

        // then
        verify(postRepository).findByBoardBoardTypeAndTitleContaining(BoardType.DEPARTMENT, "공지", pageable);
    }

    @DisplayName("ADMIN이 아무 조건도 지정하지 않으면 전체 부서게시판 글을 조회한다")
    @Test
    void getPosts_departmentBoard_adminNoDepartmentIdNoKeyword_usesAdminAllList() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(9L, "인사팀"));
        Employee admin = buildEmployee(10L, "관리자", EmployeeRole.ADMIN, null);
        CustomUserDetails user = new CustomUserDetails(admin);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardBoardType(BoardType.DEPARTMENT, pageable)).willReturn(page);

        // when
        postService.getPosts(1L, null, null, pageable, user);

        // then
        verify(postRepository).findByBoardBoardType(BoardType.DEPARTMENT, pageable);
    }

    @DisplayName("일반 사용자가 부서게시판에서 검색어를 지정하면 본인 부서+제목으로 검색한다")
    @Test
    void getPosts_departmentBoard_userWithKeyword_usesUserFilteredSearch() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(3L, "개발팀"));
        Employee normalUser = buildEmployee(11L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(normalUser);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardIdAndTitleContainingAndAuthorDepartmentId(1L, "공지", 3L, pageable))
                .willReturn(page);

        // when
        postService.getPosts(1L, "공지", null, pageable, user);

        // then
        verify(postRepository).findByBoardIdAndTitleContainingAndAuthorDepartmentId(1L, "공지", 3L, pageable);
    }

    @DisplayName("일반 사용자가 부서게시판에서 검색어 없이 조회하면 본인 부서 글 전체를 조회한다")
    @Test
    void getPosts_departmentBoard_userNoKeyword_usesUserFilteredList() {
        // given
        Board board = buildBoard(1L, BoardType.DEPARTMENT, "부서게시판", buildDepartment(3L, "개발팀"));
        Employee normalUser = buildEmployee(11L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(normalUser);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardIdAndAuthorDepartmentId(1L, 3L, pageable)).willReturn(page);

        // when
        postService.getPosts(1L, null, null, pageable, user);

        // then
        verify(postRepository).findByBoardIdAndAuthorDepartmentId(1L, 3L, pageable);
    }

    @DisplayName("부서게시판이 아니면 검색어로 제목 검색을 수행한다")
    @Test
    void getPosts_nonDepartmentBoard_withKeyword_usesTitleSearch() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(2L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardIdAndTitleContaining(2L, "검색어", pageable)).willReturn(page);

        // when
        postService.getPosts(2L, "검색어", null, pageable, null);

        // then
        verify(postRepository).findByBoardIdAndTitleContaining(2L, "검색어", pageable);
    }

    @DisplayName("부서게시판이 아니고 검색어도 없으면 게시판 전체 글을 조회한다")
    @Test
    void getPosts_nonDepartmentBoard_noKeyword_usesBoardIdList() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        Page<Post> page = new PageImpl<>(List.of());

        given(boardRepository.findById(2L)).willReturn(Optional.of(board));
        given(postRepository.findByBoardId(2L, pageable)).willReturn(page);

        // when
        postService.getPosts(2L, null, null, pageable, null);

        // then
        verify(postRepository).findByBoardId(2L, pageable);
    }

    // ==================== getPost ====================

    @DisplayName("게시글을 단건 조회하면 정상적으로 응답하고, 작성자 부서가 없어도 NPE가 발생하지 않는다")
    @Test
    void getPost_success() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when
        PostDto.Response result = postService.getPost(1L, 10L);

        // then
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getAuthorDepartmentName()).isNull();
    }

    @DisplayName("존재하지 않는 게시글을 조회하면 예외가 발생한다")
    @Test
    void getPost_postNotFound_throwsPostNotFound() {
        // given
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @DisplayName("게시글이 속한 게시판과 요청한 게시판이 다르면 예외가 발생한다")
    @Test
    void getPost_boardMismatch_throwsBoardNotFound() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.getPost(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    // ==================== createPost ====================

    @DisplayName("존재하지 않는 게시판에 게시글을 작성하면 예외가 발생한다")
    @Test
    void createPost_boardNotFound_throwsBoardNotFound() {
        // given
        Employee author = buildEmployee(5L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(author);
        PostDto.CreateRequest req = new PostDto.CreateRequest();
        req.setBoardId(1L);
        req.setTitle("제목");
        req.setContent("내용");

        given(boardRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.createPost(1L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @DisplayName("일반 사용자가 공지 게시판에 글을 작성하면 예외가 발생한다")
    @Test
    void createPost_noticeBoardNonAdmin_throwsForbidden() {
        // given
        Board board = buildBoard(1L, BoardType.NOTICE, "공지사항", null);
        Employee normalUser = buildEmployee(5L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(normalUser);
        PostDto.CreateRequest req = new PostDto.CreateRequest();
        req.setBoardId(1L);
        req.setTitle("제목");
        req.setContent("내용");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatThrownBy(() -> postService.createPost(1L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @DisplayName("ADMIN이 공지 게시판에 글을 작성하면 저장되고 실시간 알림이 발행된다")
    @Test
    void createPost_noticeBoardAdmin_success_broadcastsWebSocket() {
        // given
        Board board = buildBoard(1L, BoardType.NOTICE, "공지사항", null);
        Employee admin = buildEmployee(5L, "관리자", EmployeeRole.ADMIN, null);
        CustomUserDetails user = new CustomUserDetails(admin);
        PostDto.CreateRequest req = new PostDto.CreateRequest();
        req.setBoardId(1L);
        req.setTitle("공지 제목");
        req.setContent("공지 내용");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(employeeRepository.findById(5L)).willReturn(Optional.of(admin));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post arg = invocation.getArgument(0);
            return Post.builder()
                    .id(100L)
                    .board(arg.getBoard())
                    .author(arg.getAuthor())
                    .title(arg.getTitle())
                    .content(arg.getContent())
                    .build();
        });

        // when
        Long result = postService.createPost(1L, req, user);

        // then
        assertThat(result).isEqualTo(100L);
        verify(messagingTemplate).convertAndSend(eq("/topic/board/notice"), any(PostDto.Response.class));
    }

    @DisplayName("공지 게시판이 아니면 저장만 되고 실시간 알림은 발행되지 않는다")
    @Test
    void createPost_nonNoticeBoard_success_noBroadcast() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        Employee normalUser = buildEmployee(5L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(normalUser);
        PostDto.CreateRequest req = new PostDto.CreateRequest();
        req.setBoardId(2L);
        req.setTitle("제목");
        req.setContent("내용");

        given(boardRepository.findById(2L)).willReturn(Optional.of(board));
        given(employeeRepository.findById(5L)).willReturn(Optional.of(normalUser));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post arg = invocation.getArgument(0);
            return Post.builder()
                    .id(101L)
                    .board(arg.getBoard())
                    .author(arg.getAuthor())
                    .title(arg.getTitle())
                    .content(arg.getContent())
                    .build();
        });

        // when
        Long result = postService.createPost(2L, req, user);

        // then
        assertThat(result).isEqualTo(101L);
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("작성자 정보가 존재하지 않으면 예외가 발생한다")
    @Test
    void createPost_authorNotFound_throwsEmployeeNotFound() {
        // given
        Board board = buildBoard(2L, BoardType.FREE, "자유게시판", null);
        Employee normalUser = buildEmployee(5L, "사용자", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(normalUser);
        PostDto.CreateRequest req = new PostDto.CreateRequest();
        req.setBoardId(2L);
        req.setTitle("제목");
        req.setContent("내용");

        given(boardRepository.findById(2L)).willReturn(Optional.of(board));
        given(employeeRepository.findById(5L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.createPost(2L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    // ==================== updatePost ====================

    @DisplayName("본인이 작성한 게시글을 수정하면 정상적으로 반영된다")
    @Test
    void updatePost_success() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(author);
        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("수정 제목");
        req.setContent("수정 내용");

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when
        PostDto.Response result = postService.updatePost(1L, 10L, req, user);

        // then
        assertThat(result.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getTitle()).isEqualTo("수정 제목");
        assertThat(post.getContent()).isEqualTo("수정 내용");
    }

    @DisplayName("존재하지 않는 게시글을 수정하면 예외가 발생한다")
    @Test
    void updatePost_postNotFound_throwsPostNotFound() {
        // given
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(author);
        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("수정 제목");
        req.setContent("수정 내용");

        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.updatePost(1L, 999L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @DisplayName("본인이 작성하지 않은 게시글을 수정하면 예외가 발생한다")
    @Test
    void updatePost_notAuthor_throwsForbidden() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Employee other = buildEmployee(6L, "다른사람", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(other);
        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("수정 제목");
        req.setContent("수정 내용");

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(1L, 10L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @DisplayName("게시글이 속한 게시판과 요청한 게시판이 다르면 수정 시 예외가 발생한다")
    @Test
    void updatePost_boardMismatch_throwsBoardNotFound() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(author);
        PostDto.UpdateRequest req = new PostDto.UpdateRequest();
        req.setTitle("수정 제목");
        req.setContent("수정 내용");

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(2L, 10L, req, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    // ==================== deletePost ====================

    @DisplayName("본인이 작성한 게시글을 삭제하면 정상적으로 삭제된다")
    @Test
    void deletePost_success() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when
        postService.deletePost(1L, 10L, user);

        // then
        verify(postRepository).delete(post);
    }

    @DisplayName("존재하지 않는 게시글을 삭제하면 예외가 발생한다")
    @Test
    void deletePost_postNotFound_throwsPostNotFound() {
        // given
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        CustomUserDetails user = new CustomUserDetails(author);

        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(1L, 999L, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
        verify(postRepository, never()).delete(any(Post.class));
    }

    @DisplayName("본인이 작성하지 않은 게시글을 삭제하면 예외가 발생한다")
    @Test
    void deletePost_notAuthor_throwsForbidden() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Employee other = buildEmployee(6L, "다른사람", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(other);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(1L, 10L, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(postRepository, never()).delete(any(Post.class));
    }

    @DisplayName("게시글이 속한 게시판과 요청한 게시판이 다르면 삭제 시 예외가 발생한다")
    @Test
    void deletePost_boardMismatch_throwsBoardNotFound() {
        // given
        Board board = buildBoard(1L, BoardType.FREE, "자유게시판", null);
        Employee author = buildEmployee(5L, "홍길동", EmployeeRole.USER, null);
        Post post = buildPost(10L, board, author, "제목", "내용");
        CustomUserDetails user = new CustomUserDetails(author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.deletePost(2L, 10L, user))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
        verify(postRepository, never()).delete(any(Post.class));
    }
}
