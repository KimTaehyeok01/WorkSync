package com.worksync.domain.auth.dto;

import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LoginResponse {

    private Long employeeId;
    private String empNo;
    private String name;
    private String email;
    private EmployeeRole role;
    private EmployeeStatus status;
    private String departmentName;
    private String profileImage;
    private String accessToken;
    private String refreshToken;
}
