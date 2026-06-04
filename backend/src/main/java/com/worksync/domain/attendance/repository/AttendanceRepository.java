package com.worksync.domain.attendance.repository;

import com.worksync.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
