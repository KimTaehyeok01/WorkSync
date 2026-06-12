package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class LeaveBalanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Short year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;

    public static LeaveBalanceResponse from(AnnualLeaveBalance balance) {
        return LeaveBalanceResponse.builder()
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
