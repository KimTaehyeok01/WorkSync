package com.worksync.domain.leave.repository;

import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest,Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    //approval_doc_id로 휴가 신청 조회(연차 차감 시 사용)
    Optional<LeaveRequest> findByApprovalDocId(Long approvalDocId);

    // 같은 직원이 동일 기간에 신청한(특정 상태 제외) 휴가가 있는지 — 중복 신청 방지
    // 기간 겹침 조건: 기존.startDate <= 신청.endDate AND 기존.endDate >= 신청.startDate
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l " +
            "WHERE l.employee.id = :employeeId " +
            "AND l.status <> :excludeStatus " +
            "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("excludeStatus") LeaveStatus excludeStatus,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);
}

