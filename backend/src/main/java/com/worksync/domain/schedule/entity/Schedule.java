// 캘린더 일정 엔티티
package com.worksync.domain.schedule.entity;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 캘린더 일정. */
@Entity
@Table(name = "schedule")
@Comment("캘린더 일정")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("일정 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	@Comment("작성자")
	private Employee owner;

	// 부서 일정(DEPARTMENT)일 때만 채워진다
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	@Comment("공유 대상 부서")
	private Department department;

	@Column(nullable = false, length = 100)
	@Comment("제목")
	private String title;

	@Column(columnDefinition = "TEXT")
	@Comment("설명")
	private String description;

	@Column(length = 100)
	@Comment("장소")
	private String location;

	@Column(name = "start_at", nullable = false)
	@Comment("시작 일시")
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	@Comment("종료 일시")
	private LocalDateTime endAt;

	@Column(name = "all_day", nullable = false)
	@Comment("종일 여부")
	private Boolean isAllDay;

	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "schedule_scope_enum")
	@Comment("공개 범위")
	private ScheduleScope scope;

	@Enumerated(EnumType.STRING)
	@Column(name = "repeat_type", nullable = false, length = 10)
	@Comment("반복 주기")
	@Builder.Default
	private RepeatType repeatType = RepeatType.NONE;

	// 이 날짜까지 반복 (null 이면 계속 반복)
	@Column(name = "repeat_until")
	@Comment("반복 종료일")
	private LocalDate repeatUntilDt;

	public void update(String title, String description, String location,
			LocalDateTime startAt, LocalDateTime endAt, Boolean isAllDay,
			ScheduleScope scope, Department department,
			RepeatType repeatType, LocalDate repeatUntilDt) {
		this.repeatType = repeatType;
		this.repeatUntilDt = repeatUntilDt;
		this.title = title;
		this.description = description;
		this.location = location;
		this.startAt = startAt;
		this.endAt = endAt;
		this.isAllDay = isAllDay;
		this.scope = scope;
		this.department = department;
	}
}
