// 메일 수신자 엔티티 — 수신 구분과 읽음·삭제 상태를 수신자별로 가진다
package com.worksync.domain.mail.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseEntity;
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

import java.time.LocalDateTime;

/** 메일 수신자. */
@Entity
@Table(
		name = "mail_recipient",
		uniqueConstraints = @UniqueConstraint(columnNames = {"mail_id", "recipient_id"})
)
@Comment("메일 수신자")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MailRecipient extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("수신 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_id", nullable = false)
	@Comment("메일")
	private Mail mail;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	@Comment("수신자")
	private Employee recipient;

	@Enumerated(EnumType.STRING)
	@Column(name = "recipient_type", nullable = false, length = 10)
	@Comment("수신 구분 (TO, CC)")
	private RecipientType recipientType;

	@Column(name = "read_at")
	@Comment("읽은 일시")
	private LocalDateTime readAt;

	@Column(name = "deleted", nullable = false)
	@Comment("수신자 삭제 여부")
	@Builder.Default
	private Boolean isDeleted = false;

	// 처음 읽을 때만 시각을 남긴다
	public void markRead(LocalDateTime readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}

	public void delete() {
		this.isDeleted = true;
	}
}