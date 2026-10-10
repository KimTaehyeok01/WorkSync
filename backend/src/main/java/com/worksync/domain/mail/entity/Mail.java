// 사내 메일 본문 엔티티 — 보낸 사람 기준 1건, 수신자별 상태는 MailRecipient 가 가진다
package com.worksync.domain.mail.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

/** 사내 메일 본문. */
@Entity
@Table(name = "mail")
@Comment("사내 메일")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Mail extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Comment("메일 ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id", nullable = false)
	@Comment("보낸 사람")
	private Employee sender;

	@Column(nullable = false, length = 200)
	@Comment("제목")
	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	@Comment("본문")
	private String content;

	// 보낸 사람이 보낸편지함에서 지웠는지 — 수신자 쪽에는 영향 없음
	@Column(name = "sender_deleted", nullable = false)
	@Comment("보낸 사람 삭제 여부")
	@Builder.Default
	private Boolean isSenderDeleted = false;

	// 메일 저장 시 수신자도 함께 저장한다
	@OneToMany(mappedBy = "mail", cascade = CascadeType.PERSIST)
	@Builder.Default
	private List<MailRecipient> recipients = new ArrayList<>();

	public void addRecipient(Employee recipient, RecipientType type) {
		this.recipients.add(MailRecipient.builder()
				.mail(this)
				.recipient(recipient)
				.recipientType(type)
				.build());
	}

	public void deleteBySender() {
		this.isSenderDeleted = true;
	}
}