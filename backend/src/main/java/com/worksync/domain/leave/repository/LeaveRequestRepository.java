package com.worksync.domain.leave.repository;

import com.worksync.domain.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest,Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    //approval_doc_id로 휴가 신청 조회(연차 차감 시 사용)
    Optional<LeaveRequest> findByApprovalDocId(Long approvalDocId);
}

