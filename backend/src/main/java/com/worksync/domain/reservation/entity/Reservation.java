// 자원 예약 엔티티
package com.worksync.domain.reservation.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/** 자원 예약. */
@Entity
@Table(name = "reservation")
@Comment("자원 예약")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("예약 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resource_id", nullable = false)
	@Comment("예약 자원")
	private Resource resource;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	@Comment("예약자")
	private Employee employee;

	@Column(nullable = false, length = 100)
	@Comment("예약 목적")
	private String title;

	@Column(name = "start_at", nullable = false)
	@Comment("시작 일시")
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	@Comment("종료 일시")
	private LocalDateTime endAt;
}
