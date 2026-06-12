package com.worksync.domain.attendance.repository;

import com.worksync.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {

  // 오늘 출근 기록조회
  Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate); // workDate 오늘 있으면 이미 출근완

  // 내 근태 목록 조회
  List<Attendance> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate start, LocalDate end);

  // 전체 근태 목록 - ADMIN용
  List<Attendance> findByWorkDate(LocalDate workDate);

  // 특정 부서 직원들의 특정 날짜 근태 목록 (대시보드 - 우리 부서 출퇴근 현황)
  @Query("SELECT a FROM Attendance a WHERE a.employee.department.id = :deptId AND a.workDate = :date")
  List<Attendance> findByDepartmentAndWorkDate(@Param("deptId") Long deptId, @Param("date") LocalDate date);
}
