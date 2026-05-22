package com.worksync.domain.employee.entity;

import com.worksync.domain.department.entity.Department;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee")
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_no", nullable = false, unique = true, length = 20)
    private String empNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "employee_role")
    @Builder.Default
    private EmployeeRole role = EmployeeRole.USER;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "employee_status")
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "job_grade", nullable = false, columnDefinition = "job_grade_type")
    @Builder.Default
    private JobGrade jobGrade = JobGrade.STAFF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "profile_image", length = 512)
    private String profileImage;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
