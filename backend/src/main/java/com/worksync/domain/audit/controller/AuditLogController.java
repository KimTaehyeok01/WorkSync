package com.worksync.domain.audit.controller;

import com.worksync.domain.audit.dto.AuditLogDto;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AuditLog", description = "감사 로그 API (ADMIN 전용)")
@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    // 감사 로그 목록 조회
    @Operation(summary = "감사 로그 목록 조회 (카테고리/기간/키워드 필터)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogDto.Response>>> getLogs(
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
    @Operation(summary = "감사 로그 통계 조회 (전체/오늘/로그인실패/결재처리 수)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AuditLogDto.Summary>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getSummary()));
    }
}
