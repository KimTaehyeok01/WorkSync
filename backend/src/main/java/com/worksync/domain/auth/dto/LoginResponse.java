package com.worksync.domain.auth.dto;

import com.worksync.domain.employee.entity.EmployeeRole;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LoginResponse {

    private Long employeeId;
    private String empNo;
    private String name;
    private String email;
    private EmployeeRole role;
    private String departmentName;
    private String profileImage;
    private String accessToken;
    private String refreshToken;
}
