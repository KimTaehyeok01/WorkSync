package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class LeaveCreateRequest {

    @NotNull
    private LeaveType leaveType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String reason;

    @NotNull
    private BigDecimal dayCount;

    @NotNull
    private Long approverId;

    private Long approvalDocId;
}
