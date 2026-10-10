// 예약 대상 자원(회의실·차량·비품) 엔티티
package com.worksync.domain.reservation.entity;

import com.worksync.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 예약 대상 자원(회의실·차량·비품). */
@Entity
@Table(name = "resource")
@Comment("예약 대상 자원")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Resource extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("자원 ID")
	private Long id;

	@Column(nullable = false, length = 50)
	@Comment("자원명")
	private String name;

	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "resource_category_enum")
	@Comment("분류")
	private ResourceCategory category;

	@Column(length = 100)
	@Comment("위치")
	private String location;

	@Comment("수용 인원")
	private Integer capacity;

	@Column(length = 255)
	@Comment("설명")
	private String description;

	// false면 신규 예약을 받지 않는다 (기존 예약 이력은 보존)
	@Column(name = "active", nullable = false)
	@Comment("사용 여부")
	@Builder.Default
	private Boolean isActive = true;

	public void update(String name, ResourceCategory category, String location,
			Integer capacity, String description, Boolean isActive) {
		this.name = name;
		this.category = category;
		this.location = location;
		this.capacity = capacity;
		this.description = description;
		this.isActive = isActive;
	}
}
