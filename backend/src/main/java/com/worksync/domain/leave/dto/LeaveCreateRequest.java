package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.LeaveRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveCreateRequest {

    @NotNull
    private LeaveRequest.LeaveType leaveType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin("0.5")
    private BigDecimal daysCount;

    private String reason;

    @NotNull
    private Long approverId;
}
