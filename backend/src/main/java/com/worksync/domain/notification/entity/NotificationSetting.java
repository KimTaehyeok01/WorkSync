// 알림 수신 설정 엔티티 — 직원·유형별 수신 여부 (행이 없으면 수신)
package com.worksync.domain.notification.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 알림 수신 설정. */
@Entity
@Table(
		name = "notification_setting",
		uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "type"})
)
@Comment("알림 수신 설정")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("설정 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	@Comment("직원")
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Comment("알림 유형")
	private NotificationType type;

	@Column(name = "enabled", nullable = false)
	@Comment("수신 여부")
	private Boolean isEnabled;

	public void changeEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}
