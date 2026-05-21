package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeUpdateRequest {

    private String name;
    private String phone;
    private Employee.JobGrade jobGrade;
    private Long departmentId;
    private Employee.Status status;
}
