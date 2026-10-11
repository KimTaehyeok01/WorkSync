package com.worksync.domain.department.entity;

import com.worksync.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "department")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    // 상위 부서 (null = 최상위). 응답에서는 id 만 사용하므로 LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    public void updateName(String name){
        this.name = name;
    }

    public void updateParent(Department parent){
        this.parent = parent;
    }
}
