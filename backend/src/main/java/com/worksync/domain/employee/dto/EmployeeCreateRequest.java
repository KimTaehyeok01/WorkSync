package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreateRequest {

    @NotBlank
    @Size(max = 20)
    private String empNo;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    @NotNull
    private Employee.Role role;

    @NotNull
    private Employee.JobGrade jobGrade;

    @NotNull
    private Long departmentId;

    @NotNull
    private LocalDate hireDate;
}
