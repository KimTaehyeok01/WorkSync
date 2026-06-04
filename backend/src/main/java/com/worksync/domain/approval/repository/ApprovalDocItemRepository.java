package com.worksync.domain.approval.repository;

import com.worksync.domain.approval.entity.ApprovalDocItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDocItemRepository extends JpaRepository<ApprovalDocItem, Long> {
}
