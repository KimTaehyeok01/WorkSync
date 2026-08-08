// 직원 도메인 요청/응답 DTO 모음
package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.entity.JobGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeDto {

    @Getter @Setter
    @Schema(name = "EmployeeDto.CreateRequest")
    public static class CreateRequest {

        @NotBlank
        @Schema(description = "사번", example = "EMP001")
        private String empNo;

        @NotBlank
        @Schema(description = "직원 이름", example = "홍길동")
        private String name;

        @NotBlank
        @Email
        @Schema(description = "이메일", example = "hong@worksync.com")
        private String email;

        @NotBlank
        @Schema(description = "비밀번호")
        private String password;

        @Schema(description = "연락처", example = "010-1234-5678")
        private String phone;

        @NotNull
        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "권한 (USER, ADMIN)", example = "USER")
        private EmployeeRole role;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "입사일", example = "2026-01-01")
        private LocalDate hireDate;
    }

    @Getter @Builder
    @Schema(name = "EmployeeDto.Response")
    public static class Response {

        @Schema(description = "직원 ID", example = "1")
        private Long id;

        @Schema(description = "사번", example = "EMP001")
        private String empNo;

        @Schema(description = "직원 이름", example = "홍길동")
        private String name;

        @Schema(description = "이메일", example = "hong@worksync.com")
        private String email;

        @Schema(description = "연락처", example = "010-1234-5678")
        private String phone;

        @Schema(description = "권한 (USER, ADMIN)", example = "USER")
        private EmployeeRole role;

        @Schema(description = "상태 (ACTIVE, INACTIVE, AWAY)", example = "ACTIVE")
        private EmployeeStatus status;

        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "부서명", example = "개발팀")
        private String departmentName;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "입사일", example = "2026-01-01")
        private LocalDate hireDate;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(Employee employee) {
            return Response.builder()
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

    @Getter @Setter
    @Schema(name = "EmployeeDto.UpdateRequest")
    public static class UpdateRequest {

        @Schema(description = "직원 이름", example = "홍길동")
        private String name;

        @Schema(description = "연락처", example = "010-1234-5678")
        private String phone;

        @Schema(description = "비밀번호")
        private String password;

        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "권한 (USER, ADMIN)", example = "USER")
        private EmployeeRole role;

        @Schema(description = "부서 ID", example = "1")
        private Long departmentId;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "이메일", example = "hong@worksync.com")
        private String email;

        @Schema(description = "입사일", example = "2026-01-01")
        private LocalDate hireDate;
    }
}
