// 인사 발령 이력 엔티티 — 부서·직급·권한이 바뀔 때마다 한 줄씩 남는다
package com.worksync.domain.employee.entity;

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

/** 인사 발령 이력. */
@Entity
@Table(name = "employee_history")
@Comment("인사 발령 이력")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmployeeHistory extends BaseEntity {

	public static final String TYPE_DEPARTMENT = "DEPARTMENT";
	public static final String TYPE_JOB_GRADE = "JOB_GRADE";
	public static final String TYPE_ROLE = "ROLE";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("이력 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	@Comment("대상 직원")
	private Employee employee;

	@Column(name = "change_type", nullable = false, length = 20)
	@Comment("변경 항목 (DEPARTMENT, JOB_GRADE, ROLE)")
	private String changeType;

	@Column(name = "before_value", length = 100)
	@Comment("변경 전 값")
	private String beforeValue;

	@Column(name = "after_value", length = 100)
	@Comment("변경 후 값")
	private String afterValue;

	@Column(name = "actor_id")
	@Comment("처리한 관리자 ID")
	private Long actorId;

	@Column(name = "actor_name", length = 50)
	@Comment("처리한 관리자 이름")
	private String actorName;
}
