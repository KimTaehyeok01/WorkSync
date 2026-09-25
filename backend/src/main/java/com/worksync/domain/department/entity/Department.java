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

    public void updateName(String name){
        this.name = name;
    }
}
