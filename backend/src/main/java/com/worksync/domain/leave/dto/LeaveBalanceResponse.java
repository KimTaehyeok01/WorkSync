package com.worksync.domain.leave.dto;

import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {

    private Long id;
    private int year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainDays;

    public static LeaveBalanceResponse from(AnnualLeaveBalance b) {
        return new LeaveBalanceResponse(
                b.getId(),
                b.getYear(),
                b.getTotalDays(),
                b.getUsedDays(),
                b.getRemainDays()
        );
    }
}
