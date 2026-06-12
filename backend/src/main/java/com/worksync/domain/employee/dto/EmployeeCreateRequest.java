package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.JobGrade;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class EmployeeCreateRequest {

    @NotBlank
    private String empNo;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    @NotNull
    private JobGrade jobGrade;

    private EmployeeRole role;
    private Long departmentId;
    private String profileImage;
    private LocalDate hireDate;
}
