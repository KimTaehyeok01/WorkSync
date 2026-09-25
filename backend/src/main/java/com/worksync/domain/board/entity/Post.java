package com.worksync.domain.board.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Employee author;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public void update(String title,String content){
        if(title !=null){this.title=title;
        }
        if(content !=null){this.content=content;
        }
    }
}
