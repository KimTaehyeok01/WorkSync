package com.worksync.domain.department.dto;

import com.worksync.domain.department.entity.Department;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class DepartmentResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;

    public static DepartmentResponse from(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .createdAt(department.getCreatedAt())
                .build();
    }
}
