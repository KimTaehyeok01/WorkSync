// 자원 예약 Repository
package com.worksync.domain.reservation.repository;

import com.worksync.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 자원 예약 Repository. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	// 같은 자원에 시간이 겹치는 예약이 있는지 — 끝과 시작이 맞닿는 경우(10~11, 11~12)는 겹침 아님
	@Query("SELECT COUNT(r) > 0 FROM Reservation r " +
			"WHERE r.resource.id = :resourceId " +
			"AND r.startAt < :endAt AND r.endAt > :startAt")
	boolean existsOverlapping(@Param("resourceId") Long resourceId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);

	// 기간과 겹치는 전체 예약 (일자별 시간표용)
	@Query("SELECT r FROM Reservation r JOIN FETCH r.resource JOIN FETCH r.employee " +
			"WHERE r.startAt < :toExclusive AND r.endAt > :from " +
			"ORDER BY r.startAt")
	List<Reservation> findByPeriod(@Param("from") LocalDateTime from,
			@Param("toExclusive") LocalDateTime toExclusive);

	// 특정 직원의 아직 끝나지 않은 예약
	@Query("SELECT r FROM Reservation r JOIN FETCH r.resource JOIN FETCH r.employee " +
			"WHERE r.employee.id = :employeeId AND r.endAt >= :now " +
			"ORDER BY r.startAt")
	List<Reservation> findUpcomingByEmployee(@Param("employeeId") Long employeeId,
			@Param("now") LocalDateTime now);
}
