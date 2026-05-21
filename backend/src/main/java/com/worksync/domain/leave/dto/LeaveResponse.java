package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveStatus;
import com.worksync.domain.leave.entity.LeaveType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long approvalDocId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal daysCount;
    private String reason;
    private LeaveStatus status;
    private Long approverId;
    private String approverName;
    private LocalDateTime createdAt;

    public static LeaveResponse from(LeaveRequest request) {
        return LeaveResponse.builder()
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
