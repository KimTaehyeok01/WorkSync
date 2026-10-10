// 캘린더 일정 조회용 Repository
package com.worksync.domain.schedule.repository;

import com.worksync.domain.schedule.entity.Schedule;
import com.worksync.domain.schedule.entity.ScheduleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 캘린더 일정 Repository. */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	// 기간과 겹치는 일정 중 내가 볼 수 있는 것 — 내 개인 일정 + 내 부서 일정 + 전사 일정
	// 기간 겹침 조건: 일정.startAt < 조회 종료(미포함) AND 일정.endAt >= 조회 시작
	@Query("SELECT s FROM Schedule s JOIN FETCH s.owner " +
			"WHERE s.startAt < :toExclusive AND s.endAt >= :from " +
			"AND ((s.scope = :personal AND s.owner.id = :employeeId) " +
			"OR (s.scope = :department AND s.department.id = :departmentId) " +
			"OR s.scope = :company) " +
			"ORDER BY s.startAt")
	List<Schedule> findVisible(@Param("employeeId") Long employeeId,
			@Param("departmentId") Long departmentId,
			@Param("from") LocalDateTime from,
			@Param("toExclusive") LocalDateTime toExclusive,
			@Param("personal") ScheduleScope personal,
			@Param("department") ScheduleScope department,
			@Param("company") ScheduleScope company);
}
