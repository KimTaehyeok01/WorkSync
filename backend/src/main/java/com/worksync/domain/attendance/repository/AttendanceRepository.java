package com.worksync.domain.attendance.repository;

import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {

  // 오늘 출근 기록조회
  Optional<Attendance> findByEmployeeIdAndWorkDt(Long employeeId, LocalDate workDt); // workDt 오늘 있으면 이미 출근완

  // 내 근태 목록 조회
  List<Attendance> findByEmployeeIdAndWorkDtBetween(Long employeeId, LocalDate start, LocalDate end);

  // 전체 근태 목록 - ADMIN용
  List<Attendance> findByWorkDt(LocalDate workDt);

  // 특정 부서 직원들의 특정 날짜 출근 기록 (출근/지각한 사람만)
  @Query("SELECT a FROM Attendance a WHERE a.employee.department.id = :deptId AND a.workDt = :date")
  List<Attendance> findByDepartmentAndWorkDt(@Param("deptId") Long deptId, @Param("date") LocalDate date);

  // 특정 부서의 전체 직원 목록 (결근 직원 포함)
  @Query("SELECT e FROM Employee e WHERE e.department.id = :deptId ORDER BY e.id")
  List<Employee> findEmployeesByDepartment(@Param("deptId") Long deptId);
}
