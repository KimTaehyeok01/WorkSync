package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.LeaveRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveRequest.LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal daysCount;
    private String reason;
    private LeaveRequest.LeaveStatus status;
    private Long approverId;
    private String approverName;
    private Long approvalDocId;
    private LocalDateTime createdAt;
}
