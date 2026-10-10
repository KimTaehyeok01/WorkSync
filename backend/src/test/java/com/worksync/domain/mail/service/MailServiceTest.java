// MailService 단위 테스트
package com.worksync.domain.mail.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.domain.mail.dto.MailDto;
import com.worksync.domain.mail.entity.Mail;
import com.worksync.domain.mail.entity.MailBox;
import com.worksync.domain.mail.entity.MailRecipient;
import com.worksync.domain.mail.entity.RecipientType;
import com.worksync.domain.mail.repository.MailRecipientRepository;
import com.worksync.domain.mail.repository.MailRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** MailService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

	@Mock
	private MailRepository mailRepository;
	@Mock
	private MailRecipientRepository mailRecipientRepository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private NotificationService notificationService;
	@Mock
	private FileAttachmentRepository fileAttachmentRepository;

	@InjectMocks
	private MailService mailService;

	private Employee buildEmployee(Long id) {
		return Employee.builder()
				.id(id)
				.empNo("EMP" + id)
				.name("직원" + id)
				.email("emp" + id + "@worksync.com")
				.password("encoded-password")
				.build();
	}

	private MailDto.SendRequest buildRequest(List<Long> toIds, List<Long> ccIds) {
		MailDto.SendRequest request = new MailDto.SendRequest();
		request.setToIds(toIds);
		request.setCcIds(ccIds);
		request.setSubject(" 주간 회의 자료 ");
		request.setContent("첨부 내용 확인 부탁드립니다.");
		return request;
	}

	// 보낸 사람 1, 받는 사람 2(TO)·3(CC) 인 메일
	private Mail buildMail(Long id) {
		Mail mail = Mail.builder()
				.id(id).sender(buildEmployee(1L)).subject("주간 회의 자료").content("내용").build();
		mail.addRecipient(buildEmployee(2L), RecipientType.TO);
		mail.addRecipient(buildEmployee(3L), RecipientType.CC);
		return mail;
	}

	// === 목록의 첨부 유무(hasAttachment) ===

	@DisplayName("받은편지함 목록은 첨부가 있는 메일만 hasAttachment 가 true 이고 첨부 조회는 한 번만 한다")
	@Test
	void getInbox_mapsHasAttachmentWithSingleQuery() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Mail withFile = buildMail(10L);
		Mail withoutFile = buildMail(11L);
		MailRecipient mineOnWithFile = withFile.getRecipients().get(0);
		MailRecipient mineOnWithoutFile = withoutFile.getRecipients().get(0);
		List<MailRecipient> allRecipients = new ArrayList<>(withFile.getRecipients());
		allRecipients.addAll(withoutFile.getRecipients());

		given(mailRecipientRepository.findInbox(2L, false, null, pageable))
				.willReturn(new PageImpl<>(List.of(mineOnWithFile, mineOnWithoutFile), pageable, 2));
		given(mailRecipientRepository.findByMailIds(List.of(10L, 11L))).willReturn(allRecipients);
		given(fileAttachmentRepository.findRefIdsWithAttachment(RefType.MAIL, List.of(10L, 11L)))
				.willReturn(List.of(10L));

		// when
		Page<MailDto.ListResponse> result = mailService.getInbox(2L, false, null, pageable);

		// then
		assertThat(result.getContent()).extracting(MailDto.ListResponse::getId).containsExactly(10L, 11L);
		assertThat(result.getContent()).extracting(MailDto.ListResponse::getHasAttachment)
				.containsExactly(true, false);
		verify(fileAttachmentRepository).findRefIdsWithAttachment(any(), any());
	}

	@DisplayName("보낸편지함 목록도 첨부가 있는 메일만 hasAttachment 가 true 이다")
	@Test
	void getSent_mapsHasAttachment() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Mail withFile = buildMail(10L);
		Mail withoutFile = buildMail(11L);
		List<MailRecipient> allRecipients = new ArrayList<>(withFile.getRecipients());
		allRecipients.addAll(withoutFile.getRecipients());

		given(mailRepository.findSent(1L, null, pageable))
				.willReturn(new PageImpl<>(List.of(withFile, withoutFile), pageable, 2));
		given(mailRecipientRepository.findByMailIds(List.of(10L, 11L))).willReturn(allRecipients);
		given(fileAttachmentRepository.findRefIdsWithAttachment(RefType.MAIL, List.of(10L, 11L)))
				.willReturn(List.of(11L));

		// when
		Page<MailDto.ListResponse> result = mailService.getSent(1L, null, pageable);

		// then
		assertThat(result.getContent()).extracting(MailDto.ListResponse::getHasAttachment)
				.containsExactly(false, true);
		assertThat(result.getContent()).allMatch(MailDto.ListResponse::getIsRead);
	}

	@DisplayName("첨부가 하나도 없으면 모든 메일의 hasAttachment 가 false 이다")
	@Test
	void getInbox_noAttachments_allFalse() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		Mail mail = buildMail(10L);
		given(mailRecipientRepository.findInbox(2L, true, "회의", pageable))
				.willReturn(new PageImpl<>(List.of(mail.getRecipients().get(0)), pageable, 1));
		given(mailRecipientRepository.findByMailIds(List.of(10L))).willReturn(mail.getRecipients());
		given(fileAttachmentRepository.findRefIdsWithAttachment(RefType.MAIL, List.of(10L))).willReturn(List.of());

		// when
		Page<MailDto.ListResponse> result = mailService.getInbox(2L, true, "회의", pageable);

		// then
		assertThat(result.getContent().get(0).getHasAttachment()).isFalse();
		assertThat(result.getContent().get(0).getIsRead()).isFalse();
	}

	@DisplayName("받은편지함이 비어 있으면 첨부 조회를 호출하지 않는다")
	@Test
	void getInbox_emptyPage_doesNotQueryAttachments() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(mailRecipientRepository.findInbox(2L, false, null, pageable)).willReturn(Page.empty(pageable));

		// when
		Page<MailDto.ListResponse> result = mailService.getInbox(2L, false, null, pageable);

		// then
		assertThat(result.getContent()).isEmpty();
		verifyNoInteractions(fileAttachmentRepository);
	}

	@DisplayName("보낸편지함이 비어 있으면 첨부 조회를 호출하지 않는다")
	@Test
	void getSent_emptyPage_doesNotQueryAttachments() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(mailRepository.findSent(1L, null, pageable)).willReturn(Page.empty(pageable));

		// when
		Page<MailDto.ListResponse> result = mailService.getSent(1L, null, pageable);

		// then
		assertThat(result.getContent()).isEmpty();
		verifyNoInteractions(fileAttachmentRepository);
	}

	@DisplayName("메일을 보내면 받는 사람·참조가 저장되고 수신자에게 알림이 간다")
	@Test
	void send_success_savesRecipientsAndNotifies() {
		// given
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(1L)));
		given(employeeRepository.findAllById(Set.of(2L))).willReturn(List.of(buildEmployee(2L)));
		given(employeeRepository.findAllById(Set.of(3L))).willReturn(List.of(buildEmployee(3L)));
		given(mailRepository.save(any(Mail.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		MailDto.DetailResponse result = mailService.send(1L, buildRequest(List.of(2L), List.of(3L)));

		// then
		ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
		verify(mailRepository).save(captor.capture());
		assertThat(captor.getValue().getSubject()).isEqualTo("주간 회의 자료");
		assertThat(captor.getValue().getRecipients())
				.extracting(MailRecipient::getRecipientType)
				.containsExactly(RecipientType.TO, RecipientType.CC);
		assertThat(result.getIsSentByMe()).isTrue();
		verify(notificationService).sendToMany(
				eq(List.of(2L, 3L)), eq(1L), eq(NotificationType.MAIL), anyString(), eq("MAIL"), any());
	}

	@DisplayName("받는 사람과 참조에 같은 사람이 있으면 받는 사람으로만 저장한다")
	@Test
	void send_duplicateInToAndCc_keepsOnlyTo() {
		// given
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(1L)));
		given(employeeRepository.findAllById(Set.of(2L))).willReturn(List.of(buildEmployee(2L)));
		given(employeeRepository.findAllById(Set.of())).willReturn(List.of());
		given(mailRepository.save(any(Mail.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		MailDto.DetailResponse result = mailService.send(1L, buildRequest(List.of(2L), List.of(2L)));

		// then
		assertThat(result.getRecipients()).hasSize(1);
		assertThat(result.getRecipients().get(0).getType()).isEqualTo(RecipientType.TO);
	}

	@DisplayName("존재하지 않는 직원에게 보내면 EMPLOYEE_NOT_FOUND 예외가 발생하고 저장하지 않는다")
	@Test
	void send_unknownRecipient_throwsEmployeeNotFound() {
		// given
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(1L)));
		given(employeeRepository.findAllById(Set.of(999L))).willReturn(List.of());

		// when & then
		assertThatThrownBy(() -> mailService.send(1L, buildRequest(List.of(999L), null)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
		verify(mailRepository, never()).save(any());
	}

	@DisplayName("수신자가 메일을 처음 열면 읽음 처리되고, 읽은 시각은 응답에 내려가지 않는다")
	@Test
	void getDetail_byRecipient_marksReadAndHidesReadAt() {
		// given
		Mail mail = buildMail(10L);
		MailRecipient mine = mail.getRecipients().get(0);
		given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
		given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 2L)).willReturn(Optional.of(mine));
		given(mailRecipientRepository.findByMailIds(List.of(10L))).willReturn(mail.getRecipients());

		// when
		MailDto.DetailResponse result = mailService.getDetail(2L, 10L);

		// then
		assertThat(mine.getReadAt()).isNotNull();
		assertThat(result.getIsSentByMe()).isFalse();
		assertThat(result.getRecipients()).allMatch(recipient -> recipient.getReadAt() == null);
		verify(notificationService).readNotification(2L, "MAIL", 10L);
	}

	@DisplayName("보낸 사람이 메일을 열면 수신자별 읽은 시각을 볼 수 있다")
	@Test
	void getDetail_bySender_showsReadAt() {
		// given
		Mail mail = buildMail(10L);
		mail.getRecipients().get(0).markRead(LocalDateTime.of(2026, 10, 12, 9, 0));
		given(mailRepository.findById(10L)).willReturn(Optional.of(mail));
		given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 1L)).willReturn(Optional.empty());
		given(mailRecipientRepository.findByMailIds(List.of(10L))).willReturn(mail.getRecipients());

		// when
		MailDto.DetailResponse result = mailService.getDetail(1L, 10L);

		// then
		assertThat(result.getIsSentByMe()).isTrue();
		assertThat(result.getRecipients().get(0).getReadAt()).isEqualTo(LocalDateTime.of(2026, 10, 12, 9, 0));
		assertThat(result.getRecipients().get(1).getReadAt()).isNull();
	}

	@DisplayName("보낸 사람도 수신자도 아니면 MAIL_NOT_FOUND 예외가 발생한다")
	@Test
	void getDetail_byStranger_throwsMailNotFound() {
		// given
		given(mailRepository.findById(10L)).willReturn(Optional.of(buildMail(10L)));
		given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 9L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> mailService.getDetail(9L, 10L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.MAIL_NOT_FOUND);
	}

	@DisplayName("받은편지함에서 삭제하면 내 수신 기록만 삭제 처리되고 보낸 사람 쪽은 그대로다")
	@Test
	void delete_inbox_marksOnlyMyRecipientDeleted() {
		// given
		Mail mail = buildMail(10L);
		MailRecipient mine = mail.getRecipients().get(0);
		given(mailRecipientRepository.findByMailIdAndRecipientId(10L, 2L)).willReturn(Optional.of(mine));

		// when
		mailService.delete(2L, 10L, MailBox.INBOX);

		// then
		assertThat(mine.getIsDeleted()).isTrue();
		assertThat(mail.getIsSenderDeleted()).isFalse();
		assertThat(mail.getRecipients().get(1).getIsDeleted()).isFalse();
	}

	@DisplayName("남이 보낸 메일을 보낸편지함에서 삭제하려 하면 MAIL_NOT_FOUND 예외가 발생한다")
	@Test
	void delete_sentByOther_throwsMailNotFound() {
		// given
		given(mailRepository.findById(10L)).willReturn(Optional.of(buildMail(10L)));

		// when & then
		assertThatThrownBy(() -> mailService.delete(2L, 10L, MailBox.SENT))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.MAIL_NOT_FOUND);
	}
}
