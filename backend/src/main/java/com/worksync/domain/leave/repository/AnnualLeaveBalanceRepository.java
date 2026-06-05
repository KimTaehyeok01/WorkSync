package com.worksync.domain.leave.repository;

import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnualLeaveBalanceRepository extends JpaRepository <AnnualLeaveBalance,Long> {
    Optional<AnnualLeaveBalance> findByEmployeeIdAndYear(Long employeeId, Short year);
}
