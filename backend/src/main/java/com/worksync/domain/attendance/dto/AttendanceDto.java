// 근태 도메인 요청/응답 DTO 모음
package com.worksync.domain.attendance.dto;

import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.JobGrade;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AttendanceDto {

    @Getter @Builder
    @Schema(name = "AttendanceDto.Response")
    public static class Response {

        @Schema(description = "근태 기록 ID", example = "1")
        private Long id;

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String employeeName;

        @JsonProperty("workDate")
        @Schema(description = "근무 일자", example = "2026-08-09")
        private LocalDate workDt;

        @JsonProperty("checkInTime")
        @Schema(description = "출근 시각")
        private LocalDateTime checkedInAt;

        @JsonProperty("checkOutTime")
        @Schema(description = "퇴근 시각")
        private LocalDateTime checkedOutAt;

        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(Attendance attendance) {
            return Response.builder()
                    .id(attendance.getId())
                    .employeeId(attendance.getEmployee().getId())
                    .employeeName(attendance.getEmployee().getName())
                    .workDt(attendance.getWorkDt())
                    .checkedInAt(attendance.getCheckedInAt())
                    .checkedOutAt(attendance.getCheckedOutAt())
                    .status(attendance.getStatus())
                    .createdAt(attendance.getCreatedAt())
                    .build();
        }
    }

    // 관리자 화면용 — 출근 기록이 없는 직원도 결근으로 한 줄씩 내려간다
    @Getter @Builder
    @Schema(name = "AttendanceDto.AdminResponse")
    public static class AdminResponse {

        @Schema(description = "근태 기록 ID (기록이 없으면 null)", example = "1")
        private Long id;

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String employeeName;

        @Schema(description = "부서명", example = "개발팀")
        private String departmentName;

        @JsonProperty("workDate")
        @Schema(description = "근무 일자", example = "2026-08-09")
        private LocalDate workDt;

        @JsonProperty("checkInTime")
        @Schema(description = "출근 시각 (기록이 없으면 null)")
        private LocalDateTime checkedInAt;

        @JsonProperty("checkOutTime")
        @Schema(description = "퇴근 시각 (미퇴근·결근이면 null)")
        private LocalDateTime checkedOutAt;

        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;

        public static AdminResponse of(Employee employee, LocalDate workDt, Attendance attendance) {
            return AdminResponse.builder()
                    .id(attendance != null ? attendance.getId() : null)
                    .employeeId(employee.getId())
                    .employeeName(employee.getName())
                    .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                    .workDt(workDt)
                    .checkedInAt(attendance != null ? attendance.getCheckedInAt() : null)
                    .checkedOutAt(attendance != null ? attendance.getCheckedOutAt() : null)
                    .status(attendance != null ? attendance.getStatus() : AttendanceStatus.ABSENT)
                    .build();
        }
    }

    @Getter @Setter
    @Schema(name = "AttendanceDto.AdminUpdateRequest")
    public static class AdminUpdateRequest {

        @NotNull
        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @NotNull
        @Schema(description = "근무 일자", example = "2026-08-09")
        private LocalDate workDate;

        @Schema(description = "출근 시각 (HH:mm) — 비우면 기록 없음", example = "09:00")
        private LocalTime checkInTime;

        @Schema(description = "퇴근 시각 (HH:mm) — 비우면 미퇴근", example = "18:00")
        private LocalTime checkOutTime;

        @NotNull
        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;
    }

    @Getter
    @Builder
    @Schema(name = "AttendanceDto.DepartmentResponse")
    public static class DepartmentResponse {

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String employeeName;

        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, VICE_PRESIDENT, PRESIDENT, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;     // 출근 기록 없으면 ABSENT

        @JsonProperty("checkInTime")
        @Schema(description = "출근 시각 (결근이면 null)")
        private LocalDateTime checkedInAt;   // 결근이면 null

        @JsonProperty("checkOutTime")
        @Schema(description = "퇴근 시각 (미퇴근/결근이면 null)")
        private LocalDateTime checkedOutAt;  // 미퇴근/결근이면 null

        public static DepartmentResponse of(Employee employee, Attendance attendance) {
            return DepartmentResponse.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getName())
                    .jobGrade(employee.getJobGrade())
                    .profileImage(employee.getProfileImage())
                    .status(attendance != null ? attendance.getStatus() : AttendanceStatus.ABSENT)
                    .checkedInAt(attendance != null ? attendance.getCheckedInAt() : null)
                    .checkedOutAt(attendance != null ? attendance.getCheckedOutAt() : null)
                    .build();
        }
    }
}
