// 근태 도메인 요청/응답 DTO 모음
package com.worksync.domain.attendance.dto;

import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.JobGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

        @Schema(description = "근무 일자", example = "2026-08-09")
        private LocalDate workDate;

        @Schema(description = "출근 시각")
        private LocalDateTime checkInTime;

        @Schema(description = "퇴근 시각")
        private LocalDateTime checkOutTime;

        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(Attendance attendance) {
            return Response.builder()
                    .id(attendance.getId())
                    .employeeId(attendance.getEmployee().getId())
                    .employeeName(attendance.getEmployee().getName())
                    .workDate(attendance.getWorkDate())
                    .checkInTime(attendance.getCheckInTime())
                    .checkOutTime(attendance.getCheckOutTime())
                    .status(attendance.getStatus())
                    .createdAt(attendance.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(name = "AttendanceDto.DepartmentResponse")
    public static class DepartmentResponse {

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String employeeName;

        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "프로필 이미지 경로")
        private String profileImage;

        @Schema(description = "근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT)", example = "NORMAL")
        private AttendanceStatus status;     // 출근 기록 없으면 ABSENT

        @Schema(description = "출근 시각 (결근이면 null)")
        private LocalDateTime checkInTime;   // 결근이면 null

        @Schema(description = "퇴근 시각 (미퇴근/결근이면 null)")
        private LocalDateTime checkOutTime;  // 미퇴근/결근이면 null

        public static DepartmentResponse of(Employee employee, Attendance attendance) {
            return DepartmentResponse.builder()
                    .employeeId(employee.getId())
                    .employeeName(employee.getName())
                    .jobGrade(employee.getJobGrade())
                    .profileImage(employee.getProfileImage())
                    .status(attendance != null ? attendance.getStatus() : AttendanceStatus.ABSENT)
                    .checkInTime(attendance != null ? attendance.getCheckInTime() : null)
                    .checkOutTime(attendance != null ? attendance.getCheckOutTime() : null)
                    .build();
        }
    }
}
