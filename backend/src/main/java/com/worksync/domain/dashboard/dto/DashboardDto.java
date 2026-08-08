// 대시보드 요약 응답 DTO 모음
package com.worksync.domain.dashboard.dto;

import com.worksync.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

public class DashboardDto {

    @Getter @Builder
    @Schema(name = "DashboardDto.Response")
    public static class Response {

        @Schema(description = "오늘 근태 상태 (NORMAL, LATE, EARLY_LEAVE, ABSENT) — 출근 기록 없으면 null", example = "NORMAL")
        private AttendanceStatus todayAttendanceStatus;

        @Schema(description = "오늘 출근 여부", example = "true")
        private boolean checkedIn;

        @Schema(description = "오늘 퇴근 여부", example = "false")
        private boolean checkedOut;

        @Schema(description = "내가 결재 대기 중인 문서 수", example = "3")
        private long pendingApprovalCount;

        @Schema(description = "내가 상신한 결재 문서 수", example = "2")
        private long myRequestedApprovalCount;

        @Schema(description = "안읽은 알림 수", example = "5")
        private long unreadNotificationCount;

        @Schema(description = "할 일(TODO) 상태 업무 수", example = "4")
        private long todoTaskCount;

        @Schema(description = "진행중(IN_PROGRESS) 상태 업무 수", example = "2")
        private long inProgressTaskCount;

        @Schema(description = "완료(DONE) 상태 업무 수", example = "7")
        private long doneTaskCount;

        @Schema(description = "올해 잔여 연차 일수", example = "12.5")
        private BigDecimal remainingLeaveDays;
    }
}
