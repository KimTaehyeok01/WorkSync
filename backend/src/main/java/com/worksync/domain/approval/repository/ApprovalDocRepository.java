package com.worksync.domain.approval.repository;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalDocRepository extends JpaRepository<ApprovalDoc, Long> {

    // 내가 상신한 문서 목록
    @Query("SELECT d FROM ApprovalDoc d JOIN FETCH d.drafter JOIN FETCH d.form WHERE d.drafter.id = :drafterId ORDER BY d.createdAt DESC")
    List<ApprovalDoc> findByDrafterId(@Param("drafterId") Long drafterId);

    // 내가 상신한 문서 목록
    @Query("SELECT d FROM ApprovalDoc d JOIN FETCH d.drafter JOIN FETCH d.form WHERE d.drafter.id = :drafterId AND d.status = :status ORDER BY d.createdAt DESC")
    List<ApprovalDoc> findByDrafterIdAndStatus(@Param("drafterId") Long drafterId, @Param("status") ApprovalDocStatus status);

    // 상세 조회 (결재선 + 문서 항목 포함)
    @Query("SELECT DISTINCT d FROM ApprovalDoc d " +
            "JOIN FETCH d.drafter " +
            "JOIN FETCH d.form " +
            "LEFT JOIN FETCH d.approvalLines al " +
            "LEFT JOIN FETCH al.approver " +
            "LEFT JOIN FETCH d.approvalDocItems " +
            "WHERE d.id = :id")
    Optional<ApprovalDoc> findWithDetailsById(@Param("id") Long id);
}
