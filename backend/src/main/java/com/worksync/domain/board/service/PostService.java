package com.worksync.domain.board.service;


import com.worksync.domain.board.dto.PostCreateRequest;
import com.worksync.domain.board.dto.PostResponse;
import com.worksync.domain.board.dto.PostUpdateRequest;
import com.worksync.domain.board.entity.Board;
import com.worksync.domain.board.entity.BoardType;
import com.worksync.domain.board.entity.Post;
import com.worksync.domain.board.repository.BoardRepository;
import com.worksync.domain.board.repository.PostRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.mbeans.SparseUserDatabaseMBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
    private  final PostRepository postRepository;
    private final BoardRepository boardRepository;

    //게시글 목록 조회(페이징+제목검색)
    public Page<PostResponse>getPosts(Long boardId, String keyword, Pageable pageable){
        Page<Post> posts;
        if (keyword != null) {
            posts=postRepository.findByBoardIdAndTitleContaining(boardId,keyword,pageable);
        }
        else {
            posts=postRepository.findByBoardId(boardId,pageable);
        }
        return posts.map(PostResponse::from);
    }

    //게시글 상세 조회
public PostResponse getPost(Long boardId,Long postId){
        Post post=postRepository.findById(postId)
                .orElseThrow(()-> new RuntimeException("게시판 없음"));
        return PostResponse.from(post);
    }

    //게시글 작성
    public Long createPost(Long boardId, PostCreateRequest req, CustomUserDetails user){
        Board board=boardRepository.findById(boardId)
                .orElseThrow(()->new RuntimeException("게시판 없음"));
        //공지글 어드민만 가능하게 권한 제어
        if(board.getBoardType() == BoardType.NOTICE){
            if(user.getEmployee().getRole() != EmployeeRole.ADMIN){
                throw new RuntimeException("공지사항은 관리자만 작성 가능합니다.");
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
    public PostResponse updatePost(Long boardId, Long postId, PostUpdateRequest req,CustomUserDetails user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));

        //본인 게시글 체크 로직
        if (!post.getAuthor().getId().equals(user.getEmployee().getId())) {
            throw new RuntimeException("본인 게시글이 아닙니다.");
        }
        post.update(req.getTitle(), req.getContent());
        return PostResponse.from(post);
    }

        //게시글 삭제(본인만 가능)
        public void deletePost(Long boardId,Long postId,CustomUserDetails user){
            Post post=postRepository.findById(postId)
                    .orElseThrow(()->new RuntimeException("게시글 없음"));

            //본인확인 로직
            if(!post.getAuthor().getId().equals(user.getEmployee().getId())){
                throw  new RuntimeException("본인 게시글만 삭제 가능합니다.");

            }
         postRepository.delete(post);
        }
}
