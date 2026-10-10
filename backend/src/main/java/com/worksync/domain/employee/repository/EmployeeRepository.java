package com.worksync.domain.employee.repository;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmpNo(String empNo);
    boolean existsByEmail(String email);
    boolean existsByEmpNo(String empNo);

    // 알림 대상 조회 — 특정 상태(퇴사 등)를 제외한 직원
    List<Employee> findByStatusNot(EmployeeStatus status);

    List<Employee> findByDepartmentIdAndStatusNot(Long departmentId, EmployeeStatus status);

    // 통합 검색 — 이름으로 직원 찾기
    List<Employee> findByNameContaining(String name, Pageable pageable);
}
