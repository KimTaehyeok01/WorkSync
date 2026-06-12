package com.worksync.domain.employee.repository;

import com.worksync.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmpNo(String empNo);
    boolean existsByEmail(String email);
    boolean existsByEmpNo(String empNo);
}
