package com.worksync.domain.approval.repository;

import com.worksync.domain.approval.entity.ApprovalForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalFormRepository extends JpaRepository<ApprovalForm, Long> {

    // 결재 양식 유형으로 조회 (예: "LEAVE" - 휴가 신청 양식)
    Optional<ApprovalForm> findByFormType(String formType);
}
