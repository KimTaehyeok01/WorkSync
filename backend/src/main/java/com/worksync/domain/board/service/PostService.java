package com.worksync.domain.board.service;


import com.worksync.domain.board.dto.PostCreateRequest;
import com.worksync.domain.board.dto.PostResponse;
import com.worksync.domain.board.dto.PostUpdateRequest;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.repository.BoardRepository;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class PostService {
    private  final PostRepository postRepository;
    private final BoardRepository boardRepository;

    //게시글 목록 조회(페이징+제목검색)
    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Long boardId, String keyword, Pageable pageable, CustomUserDetails user) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        Page<Post> posts;

        if (board.getBoardType() == BoardType.DEPARTMENT && board.getDepartment() != null) {
            boolean isAdmin = user != null
                    && user.getEmployee().getRole() == EmployeeRole.ADMIN;

            if (isAdmin) {
                // ADMIN: 부서 제한 없이 모든 부서게시판 글 조회
                if (keyword != null) {
                    posts = postRepository.findByBoardBoardTypeAndTitleContaining(BoardType.DEPARTMENT, keyword, pageable);
                } else {
                    posts = postRepository.findByBoardBoardType(BoardType.DEPARTMENT, pageable);
                }
            } else {
                // USER: 해당 부서 작성자 글만 반환
                Long departmentId = board.getDepartment().getId();
                if (keyword != null) {
                    posts = postRepository.findByBoardIdAndTitleContainingAndAuthorDepartmentId(boardId, keyword, departmentId, pageable);
                } else {
                    posts = postRepository.findByBoardIdAndAuthorDepartmentId(boardId, departmentId, pageable);
                }
            }
        } else {
            if (keyword != null) {
                posts = postRepository.findByBoardIdAndTitleContaining(boardId, keyword, pageable);
            } else {
                posts = postRepository.findByBoardId(boardId, pageable);
            }
        }

        return posts.map(PostResponse::from);
    }

    //게시글 상세 조회
    @Transactional(readOnly = true)
    public PostResponse getPost(Long boardId,Long postId){
        Post post=postRepository.findById(postId)
                .orElseThrow(()-> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getBoard().getId().equals(boardId)){
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        return PostResponse.from(post);
    }

    //게시글 작성
    @Transactional
    public Long createPost(Long boardId, PostCreateRequest req,CustomUserDetails user){
        Board board=boardRepository.findById(boardId)
                .orElseThrow(()->new CustomException(ErrorCode.BOARD_NOT_FOUND));
        //공지글 어드민만 가능하게 권한 제어
        if(board.getBoardType() == BoardType.NOTICE){
            if(user.getEmployee().getRole() != EmployeeRole.ADMIN){
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }
        Post post=Post.builder()
                .board(board)
                .author(user.getEmployee())
                .title(req.getTitle())
                .content(req.getContent())
                .build();
        return postRepository.save(post).getId();
    }

    //게시글 수정(본인만 가능)
    @Transactional
    public PostResponse updatePost(Long boardId, Long postId, PostUpdateRequest req,CustomUserDetails user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        //본인 게시글 체크 로직
        if (!post.getAuthor().getId().equals(user.getEmployee().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!post.getBoard().getId().equals(boardId)){
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        post.update(req.getTitle(), req.getContent());
        return PostResponse.from(post);
    }

    //게시글 삭제(본인만 가능)
    @Transactional
    public void deletePost(Long boardId,Long postId,CustomUserDetails user){
        Post post=postRepository.findById(postId)
                    .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND));

        //본인확인 로직
        if(!post.getAuthor().getId().equals(user.getEmployee().getId())){
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!post.getBoard().getId().equals(boardId)){
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
         postRepository.delete(post);
    }
}


