package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.entity.JobGrade;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class EmployeeResponse {

    private Long id;
    private String empNo;
    private String name;
    private String email;
    private String phone;
    private EmployeeRole role;
    private EmployeeStatus status;
    private JobGrade jobGrade;
    private Long departmentId;
    private String departmentName;
    private String profileImage;
    private LocalDate hireDate;
    private LocalDateTime createdAt;

    public static EmployeeResponse from(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .empNo(employee.getEmpNo())
                .name(employee.getName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .role(employee.getRole())
                .status(employee.getStatus())
                .jobGrade(employee.getJobGrade())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .profileImage(employee.getProfileImage())
                .hireDate(employee.getHireDate())
                .createdAt(employee.getCreatedAt())
                .build();
    }
}
