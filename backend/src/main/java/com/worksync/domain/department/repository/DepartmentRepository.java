package com.worksync.domain.department.repository;

import com.worksync.domain.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
  boolean existsByName(String name);
}
