package com.worksync.domain.approval.repository;

import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    // 내가 결재해야 할 라인 목록 (WAITING 상태 + 문서/결재선/기안자/양식 fetch)
    @Query("SELECT DISTINCT al FROM ApprovalLine al " +
            "JOIN FETCH al.doc d " +
            "JOIN FETCH d.drafter " +
            "JOIN FETCH d.form " +
            "JOIN FETCH al.approver " +
            "LEFT JOIN FETCH d.approvalLines dLines " +
            "LEFT JOIN FETCH dLines.approver " +
            "WHERE al.approver.id = :approverId AND al.status = :status")
    List<ApprovalLine> findByApproverIdAndStatus(@Param("approverId") Long approverId,
                                                  @Param("status") ApprovalLineStatus status);

    List<ApprovalLine> findByApproverId(Long approverId);
}
