// 인사 발령 이력 Repository
package com.worksync.domain.employee.repository;

import com.worksync.domain.employee.entity.EmployeeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 인사 발령 이력 Repository. */
public interface EmployeeHistoryRepository extends JpaRepository<EmployeeHistory, Long> {

	List<EmployeeHistory> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
