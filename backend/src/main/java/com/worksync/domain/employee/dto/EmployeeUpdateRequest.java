package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.entity.JobGrade;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmployeeUpdateRequest {

    private String name;
    private String phone;
    private String password;
    private JobGrade jobGrade;
    private EmployeeStatus status;
    private Long departmentId;
    private String profileImage;
}
