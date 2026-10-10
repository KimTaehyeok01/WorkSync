// 일정 참석자 Repository
package com.worksync.domain.schedule.repository;

import com.worksync.domain.schedule.entity.ScheduleAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 일정 참석자 Repository. */
public interface ScheduleAttendeeRepository extends JpaRepository<ScheduleAttendee, Long> {

	// 여러 일정의 참석자를 한 번에 조회
	@Query("SELECT a FROM ScheduleAttendee a JOIN FETCH a.employee " +
			"WHERE a.schedule.id IN :scheduleIds ORDER BY a.id")
	List<ScheduleAttendee> findByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

	// 일정 수정 시 빠진 참석자 제거
	@Modifying
	@Query("DELETE FROM ScheduleAttendee a WHERE a.schedule.id = :scheduleId AND a.employee.id IN :employeeIds")
	void deleteByScheduleIdAndEmployeeIds(@Param("scheduleId") Long scheduleId,
			@Param("employeeIds") List<Long> employeeIds);
}
