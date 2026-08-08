// 인증 도메인 요청/응답 DTO 모음
package com.worksync.domain.auth.dto;

import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter @Setter
    @Schema(name = "AuthDto.LoginRequest")
    public static class LoginRequest {

        @NotBlank
        @Schema(description = "사번", example = "EMP001")
        private String empNo;

        @NotBlank
        @Schema(description = "비밀번호")
        private String password;

        @Schema(description = "로그인 시 전환할 상태 (ACTIVE, INACTIVE, AWAY)", example = "ACTIVE")
        private EmployeeStatus status;
    }

    @Getter @Builder
    @Schema(name = "AuthDto.LoginResponse")
    public static class LoginResponse {

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "사번", example = "EMP001")
        private String empNo;

        @Schema(description = "직원 이름", example = "홍길동")
        private String name;

        @Schema(description = "이메일", example = "hong@worksync.com")
        private String email;

        @Schema(description = "권한 (USER, ADMIN)", example = "USER")
        private EmployeeRole role;

        @Schema(description = "상태 (ACTIVE, INACTIVE, AWAY)", example = "ACTIVE")
        private EmployeeStatus status;

        @Schema(description = "부서명", example = "개발팀")
        private String departmentName;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "액세스 토큰 (JWT)")
        private String accessToken;

        @Schema(description = "리프레시 토큰 (JWT)")
        private String refreshToken;
    }

    @Getter @Setter
    @Schema(name = "AuthDto.ReissueRequest")
    public static class ReissueRequest {

        @NotBlank
        @Schema(description = "리프레시 토큰 (JWT)")
        private String refreshToken;
    }
}
