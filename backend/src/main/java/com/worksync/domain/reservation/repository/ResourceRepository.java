// 예약 대상 자원 Repository
package com.worksync.domain.reservation.repository;

import com.worksync.domain.reservation.entity.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 예약 대상 자원 Repository. */
public interface ResourceRepository extends JpaRepository<Resource, Long> {

	List<Resource> findAllByOrderByIdAsc();

	// 예약 생성 시 같은 자원에 대한 동시 예약을 직렬화하기 위한 비관적 락 조회
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Resource r WHERE r.id = :id")
	Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
