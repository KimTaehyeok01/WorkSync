package com.worksync.domain.approval.repository;

import com.worksync.domain.approval.entity.ApprovalForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalFormRepository extends JpaRepository<ApprovalForm, Long> {
}
