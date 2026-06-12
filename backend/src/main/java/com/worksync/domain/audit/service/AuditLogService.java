package com.worksync.domain.audit.service;

import com.worksync.domain.audit.dto.AuditLogResponse;
import com.worksync.domain.audit.dto.AuditLogSummaryResponse;
import com.worksync.domain.audit.entity.AuditLog;
import com.worksync.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // 로그인 실패 액션명 / 전자결재 카테고리 상수
    private static final String ACTION_LOGIN_FAIL = "로그인 실패";
    private static final String CATEGORY_APPROVAL = "APPROVAL";

    // 페이지네이션 목록 10개 묶음
    // 감사 로그 목록 조회 (카테고리 / 기간 / 키워드 필터)
    // 기간: today | week | month | null(전체)
    public Page<AuditLogResponse> getLogs(String category, String period, String keyword, Pageable pageable) {
        String targetType = (category == null || category.isBlank()) ? null : category;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        LocalDateTime from = resolveFrom(period);

        return auditLogRepository.search(targetType, from, kw, pageable)
                .map(AuditLogResponse::from);
    }

    // 상단 통계 위젯
    public AuditLogSummaryResponse getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        return AuditLogSummaryResponse.builder()
                .totalCount(auditLogRepository.count())
                .todayCount(auditLogRepository.countByCreatedAtGreaterThanEqual(todayStart))
                .loginFailCount(auditLogRepository.countByAction(ACTION_LOGIN_FAIL))
                .approvalCount(auditLogRepository.countByTargetType(CATEGORY_APPROVAL))
                .build();
    }

    // 감사 로그 기록 (내부용 - 다른 서비스에서 호출)
    @Transactional
    public void log(Long actorId, String actorName, String action,
                    String targetType, Long targetId, String clientIp, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(auditLog);
    }

    // 기간 필터 → 시작 일시 변환
    private LocalDateTime resolveFrom(String period) {
        if (period == null || period.isBlank() || period.equals("all")) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "today" -> today.atStartOfDay();
            case "week" -> today.minusDays(6).atStartOfDay();   // 최근 7일
            case "month" -> today.withDayOfMonth(1).atStartOfDay();
            default -> null;
        };
    }
}
