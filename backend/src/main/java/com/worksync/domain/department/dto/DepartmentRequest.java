package com.worksync.domain.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DepartmentRequest {

    @NotBlank
    @Size(max = 30)
    private String name;
}
