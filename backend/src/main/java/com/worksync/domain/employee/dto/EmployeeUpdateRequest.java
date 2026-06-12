package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.JobGrade;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class EmployeeUpdateRequest {

    private String name;
    private String phone;
    private String password;
    private JobGrade jobGrade;
    private EmployeeRole role;
    private Long departmentId;
    private String profileImage;
    private String email;
    private LocalDate hireDate;
}
