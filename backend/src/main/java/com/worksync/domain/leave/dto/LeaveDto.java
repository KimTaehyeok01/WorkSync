// 휴가 도메인 요청/응답 DTO 모음
package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveStatus;
import com.worksync.domain.leave.entity.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveDto {

    @Getter @Builder
    @Schema(name = "LeaveDto.BalanceResponse")
    public static class BalanceResponse {

        @Schema(description = "연차 잔여 ID", example = "1")
        private Long id;

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String employeeName;

        @Schema(description = "귀속 연도", example = "2026")
        private Short year;

        @Schema(description = "총 연차 일수", example = "15")
        private BigDecimal totalDays;

        @Schema(description = "사용 연차 일수", example = "3.5")
        private BigDecimal usedDays;

        @Schema(description = "잔여 연차 일수", example = "11.5")
        private BigDecimal remainingDays;

        public static BalanceResponse from(AnnualLeaveBalance balance) {
            return BalanceResponse.builder()
                    .id(balance.getId())
                    .employeeId(balance.getEmployee().getId())
                    .employeeName(balance.getEmployee().getName())
                    .year(balance.getYear())
                    .totalDays(balance.getTotalDays())
                    .usedDays(balance.getUsedDays())
                    .remainingDays(balance.getRemainingDays())
                    .build();
        }
    }

    @Getter @Setter
    @Schema(name = "LeaveDto.CreateRequest")
    public static class CreateRequest {

        @NotNull
        @Schema(description = "휴가 유형 (ANNUAL, HALF, SICK, FAMILY, OTHER)", example = "ANNUAL")
        private LeaveType leaveType;

        @NotNull
        @Schema(description = "휴가 시작일", format = "yyyy-MM-dd", example = "2026-08-10")
        private LocalDate startDate;

        @NotNull
        @Schema(description = "휴가 종료일", format = "yyyy-MM-dd", example = "2026-08-11")
        private LocalDate endDate;

        @Schema(description = "휴가 사유", example = "개인 사유")
        private String reason;

        @NotNull
        @Schema(description = "사용 일수", example = "2")
        private BigDecimal dayCount;

        @NotNull
        @Schema(description = "결재자 ID", example = "1")
        private Long approverId;

        @Schema(description = "연결된 결재 문서 ID", example = "1")
        private Long approvalDocId;
    }

    @Getter @Builder
    @Schema(name = "LeaveDto.Response")
    public static class Response {

        @Schema(description = "휴가 신청 ID", example = "1")
        private Long id;

        @Schema(description = "신청 직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "신청 직원 이름", example = "홍길동")
        private String employeeName;

        @Schema(description = "연결된 결재 문서 ID (없으면 null)", example = "1")
        private Long approvalDocId;

        @Schema(description = "휴가 유형 (ANNUAL, HALF, SICK, FAMILY, OTHER)", example = "ANNUAL")
        private LeaveType leaveType;

        @Schema(description = "휴가 시작일", format = "yyyy-MM-dd", example = "2026-08-10")
        private LocalDate startDate;

        @Schema(description = "휴가 종료일", format = "yyyy-MM-dd", example = "2026-08-11")
        private LocalDate endDate;

        @Schema(description = "사용 일수", example = "2")
        private BigDecimal daysCount;

        @Schema(description = "휴가 사유", example = "개인 사유")
        private String reason;

        @Schema(description = "처리 상태 (PENDING, APPROVED, REJECTED)", example = "PENDING")
        private LeaveStatus status;

        @Schema(description = "결재자 ID (없으면 null)", example = "1")
        private Long approverId;

        @Schema(description = "결재자 이름 (없으면 null)", example = "김부장")
        private String approverName;

        @Schema(description = "신청 일시")
        private LocalDateTime createdAt;

        public static Response from(LeaveRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .employeeId(request.getEmployee().getId())
                    .employeeName(request.getEmployee().getName())
                    .approvalDocId(request.getApprovalDoc() != null ? request.getApprovalDoc().getId() : null)
                    .leaveType(request.getLeaveType())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .daysCount(request.getDaysCount())
                    .reason(request.getReason())
                    .status(request.getStatus())
                    .approverId(request.getApprover() != null ? request.getApprover().getId() : null)
                    .approverName(request.getApprover() != null ? request.getApprover().getName() : null)
                    .createdAt(request.getCreatedAt())
                    .build();
        }
    }
}
