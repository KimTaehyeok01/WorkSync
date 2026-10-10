// 캘린더 일정 조회용 Repository
package com.worksync.domain.schedule.repository;

import com.worksync.domain.schedule.entity.RepeatType;
import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 캘린더 일정 Repository. */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	// 기간에 걸칠 수 있는 일정 중 내가 볼 수 있는 것 — 내 개인 일정 + 내 부서 일정 + 전사 일정 + 내가 참석자인 일정
	// 단발 일정은 기간과 겹치는 것, 반복 일정은 조회 종료 전에 시작해 반복이 조회 시작 이후까지 이어지는 것
	// (반복 일정의 실제 발생 여부는 서비스에서 회차를 전개해 판단한다)
	@Query("SELECT s FROM Schedule s JOIN FETCH s.owner " +
			"WHERE s.startAt < :toExclusive " +
			"AND (s.endAt >= :from " +
			"     OR (s.repeatType <> :none AND (s.repeatUntilDt IS NULL OR s.repeatUntilDt >= :fromDate))) " +
			"AND ((s.scope = :personal AND s.owner.id = :employeeId) " +
			"OR (s.scope = :department AND s.department.id = :departmentId) " +
			"OR s.scope = :company " +
			"OR EXISTS (SELECT 1 FROM ScheduleAttendee a WHERE a.schedule = s AND a.employee.id = :employeeId)) " +
			"ORDER BY s.startAt")
	List<Schedule> findVisible(@Param("employeeId") Long employeeId,
			@Param("departmentId") Long departmentId,
			@Param("from") LocalDateTime from,
			@Param("fromDate") LocalDate fromDate,
			@Param("toExclusive") LocalDateTime toExclusive,
			@Param("none") RepeatType none,
			@Param("personal") ScheduleScope personal,
			@Param("department") ScheduleScope department,
			@Param("company") ScheduleScope company);

	// 통합 검색 — 내가 볼 수 있는 일정 중 제목이 일치하는 것 (최근 시작 순)
	@Query("SELECT s FROM Schedule s JOIN FETCH s.owner " +
			"WHERE s.title LIKE CONCAT('%', :keyword, '%') " +
			"AND ((s.scope = :personal AND s.owner.id = :employeeId) " +
			"OR (s.scope = :department AND s.department.id = :departmentId) " +
			"OR s.scope = :company " +
			"OR EXISTS (SELECT 1 FROM ScheduleAttendee a WHERE a.schedule = s AND a.employee.id = :employeeId)) " +
			"ORDER BY s.startAt DESC")
	List<Schedule> searchVisible(@Param("employeeId") Long employeeId,
			@Param("departmentId") Long departmentId,
			@Param("keyword") String keyword,
			@Param("personal") ScheduleScope personal,
			@Param("department") ScheduleScope department,
			@Param("company") ScheduleScope company,
			Pageable pageable);
}
