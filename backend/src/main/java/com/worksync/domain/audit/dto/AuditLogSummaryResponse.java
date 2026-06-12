package com.worksync.domain.audit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AuditLogSummaryResponse {

    private long totalCount;      // 전체 로그 수
    private long todayCount;      // 오늘 활동 수
    private long loginFailCount;  // 로그인 실패 수
    private long approvalCount;   // 전자결재 처리 수
}
