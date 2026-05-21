package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String empNo;
    private String name;
    private String email;
    private String phone;
    private Employee.Role role;
    private Employee.Status status;
    private Employee.JobGrade jobGrade;
    private Long departmentId;
    private String departmentName;
    private String profileImage;
    private LocalDate hireDate;
    private LocalDateTime createdAt;

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getEmpNo(),
                e.getName(),
                e.getEmail(),
                e.getPhone(),
                e.getRole(),
                e.getStatus(),
                e.getJobGrade(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getProfileImage(),
                e.getHireDate(),
                e.getCreatedAt()
        );
    }
}
