package com.worksync.domain.auth.dto;

import com.worksync.domain.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {

    @NotBlank
    private String empNo;

    @NotBlank
    private String password;

    private EmployeeStatus status;
}
