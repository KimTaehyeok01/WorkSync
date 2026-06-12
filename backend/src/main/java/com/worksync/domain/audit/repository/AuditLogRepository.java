package com.worksync.domain.audit.repository;

import com.worksync.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 목록 조회 (카테고리=targetType / 시작일시 / 키워드 동적 필터 + 페이징)
    @Query("SELECT a FROM AuditLog a " +
            "WHERE (CAST(:targetType AS string) IS NULL OR a.targetType = :targetType) " +
            "AND (CAST(:from AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :from) " +
            "AND (CAST(:keyword AS string) IS NULL OR a.action LIKE %:keyword% OR a.actorName LIKE %:keyword%) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("targetType") String targetType,
                          @Param("from") LocalDateTime from,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    // 통계 - 오늘 활동 수
    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    // 통계 - 특정 액션 수 (로그인 실패 등)
    long countByAction(String action);

    // 통계 - 특정 카테고리(targetType) 수 (전자결재 처리 등)
    long countByTargetType(String targetType);
}
