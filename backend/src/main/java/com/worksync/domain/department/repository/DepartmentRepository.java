package com.worksync.domain.department.repository;

import com.worksync.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
  boolean existsByName(String name);

  // 직원 일괄 등록 시 부서명으로 부서 찾기
  Optional<Department> findByName(String name);
}
