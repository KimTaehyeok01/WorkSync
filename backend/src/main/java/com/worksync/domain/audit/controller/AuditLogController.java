package com.worksync.domain.audit.controller;

import com.worksync.domain.audit.dto.AuditLogResponse;
import com.worksync.domain.audit.dto.AuditLogSummaryResponse;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    // 감사 로그 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                auditLogService.getLogs(category, period, keyword, pageable)));
    }

    // 상단 통계 위젯 (전체/오늘/로그인실패/결재처리 수)
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AuditLogSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getSummary()));
    }
}
