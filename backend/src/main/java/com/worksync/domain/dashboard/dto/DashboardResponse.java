package com.worksync.domain.dashboard.dto;

import com.worksync.domain.attendance.entity.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class DashboardResponse {

    private AttendanceStatus todayAttendanceStatus;
    private boolean checkedIn;
    private boolean checkedOut;

    private long pendingApprovalCount;
    private long myRequestedApprovalCount;

    private long unreadNotificationCount;

    private long todoTaskCount;
    private long inProgressTaskCount;
    private long doneTaskCount;

    private BigDecimal remainingLeaveDays;
}
