// 사내 메일 도메인 요청/응답 DTO 모음
package com.worksync.domain.mail.dto;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.mail.entity.Mail;
import com.worksync.domain.mail.entity.MailBox;
import com.worksync.domain.mail.entity.MailRecipient;
import com.worksync.domain.mail.entity.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 사내 메일 DTO 모음. */
public class MailDto {

	private static final int PREVIEW_LENGTH = 80;

	@Getter @Setter
	@Schema(name = "MailDto.SendRequest")
	public static class SendRequest {

		@NotEmpty
		@Schema(description = "받는 사람 직원 ID 목록", example = "[2, 3]")
		private List<Long> toIds;

		@Schema(description = "참조 직원 ID 목록", example = "[4]")
		private List<Long> ccIds;

		@NotBlank
		@Size(max = 200)
		@Schema(description = "제목", example = "주간 회의 자료 공유")
		private String subject;

		@NotBlank
		@Schema(description = "본문")
		private String content;
	}

	@Getter @Setter
	@Schema(name = "MailDto.DeleteRequest")
	public static class DeleteRequest {

		@NotNull
		@Schema(description = "메일 ID", example = "1")
		private Long id;

		@NotNull
		@Schema(description = "삭제할 메일함 (INBOX, SENT)", example = "INBOX")
		private MailBox box;
	}

	@Getter @Builder
	@Schema(name = "MailDto.PersonResponse")
	public static class PersonResponse {

		@Schema(description = "직원 ID", example = "1")
		private Long id;

		@Schema(description = "이름", example = "홍길동")
		private String name;

		@Schema(description = "부서명", example = "개발팀")
		private String departmentName;

		@Schema(description = "프로필 이미지 경로")
		private String profileImage;

		public static PersonResponse from(Employee employee) {
			return PersonResponse.builder()
					.id(employee.getId())
					.name(employee.getName())
					.departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
					.profileImage(employee.getProfileImage())
					.build();
		}
	}

	@Getter @Builder
	@Schema(name = "MailDto.RecipientResponse")
	public static class RecipientResponse {

		@Schema(description = "수신자")
		private PersonResponse person;

		@Schema(description = "수신 구분 (TO, CC)", example = "TO")
		private RecipientType type;

		@Schema(description = "읽은 일시 — 보낸 사람에게만 내려간다")
		private LocalDateTime readAt;

		public static RecipientResponse of(MailRecipient recipient, boolean showReadAt) {
			return RecipientResponse.builder()
					.person(PersonResponse.from(recipient.getRecipient()))
					.type(recipient.getRecipientType())
					.readAt(showReadAt ? recipient.getReadAt() : null)
					.build();
		}
	}

	@Getter @Builder
	@Schema(name = "MailDto.ListResponse")
	public static class ListResponse {

		@Schema(description = "메일 ID", example = "1")
		private Long id;

		@Schema(description = "제목")
		private String subject;

		@Schema(description = "본문 미리보기")
		private String preview;

		@Schema(description = "보낸 사람")
		private PersonResponse sender;

		@Schema(description = "수신자 요약", example = "홍길동 외 2명")
		private String recipientSummary;

		@Schema(description = "읽음 여부 — 받은편지함은 내가 읽었는지, 보낸편지함은 항상 true", example = "false")
		private Boolean isRead;

		@Schema(description = "보낸 일시")
		private LocalDateTime createdAt;

		public static ListResponse of(Mail mail, List<MailRecipient> recipients, boolean read) {
			String content = mail.getContent();
			return ListResponse.builder()
					.id(mail.getId())
					.subject(mail.getSubject())
					.preview(content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) : content)
					.sender(PersonResponse.from(mail.getSender()))
					.recipientSummary(summarize(recipients))
					.isRead(read)
					.createdAt(mail.getCreatedAt())
					.build();
		}

		private static String summarize(List<MailRecipient> recipients) {
			if (recipients.isEmpty()) {
				return "";
			}
			String first = recipients.get(0).getRecipient().getName();
			return recipients.size() == 1 ? first : first + " 외 " + (recipients.size() - 1) + "명";
		}
	}

	@Getter @Builder
	@Schema(name = "MailDto.DetailResponse")
	public static class DetailResponse {

		@Schema(description = "메일 ID", example = "1")
		private Long id;

		@Schema(description = "제목")
		private String subject;

		@Schema(description = "본문")
		private String content;

		@Schema(description = "보낸 사람")
		private PersonResponse sender;

		@Schema(description = "수신자 목록 (받는 사람·참조)")
		private List<RecipientResponse> recipients;

		@Schema(description = "조회한 사용자가 보낸 사람인지 여부", example = "false")
		private Boolean isSentByMe;

		@Schema(description = "보낸 일시")
		private LocalDateTime createdAt;

		public static DetailResponse of(Mail mail, List<MailRecipient> recipients, boolean sentByMe) {
			return DetailResponse.builder()
					.id(mail.getId())
					.subject(mail.getSubject())
					.content(mail.getContent())
					.sender(PersonResponse.from(mail.getSender()))
					.recipients(recipients.stream()
							.map(recipient -> RecipientResponse.of(recipient, sentByMe))
							.toList())
					.isSentByMe(sentByMe)
					.createdAt(mail.getCreatedAt())
					.build();
		}
	}
}
