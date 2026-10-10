// 사내 메일 발송·조회·삭제 서비스
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 사내 메일 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailService {

	private static final String TARGET_TYPE = "MAIL";

	private final MailRepository mailRepository;
	private final MailRecipientRepository mailRecipientRepository;
	private final EmployeeRepository employeeRepository;
	private final NotificationService notificationService;
	private final FileAttachmentRepository fileAttachmentRepository;

	// 받은편지함
	public Page<MailDto.ListResponse> getInbox(Long myId, boolean unreadOnly, String keyword, Pageable pageable) {
		Page<MailRecipient> page = mailRecipientRepository.findInbox(myId, unreadOnly, normalize(keyword), pageable);
		List<Long> mailIds = page.getContent().stream().map(r -> r.getMail().getId()).toList();
		Map<Long, List<MailRecipient>> recipientsByMail = loadRecipients(mailIds);
		Set<Long> attachedMailIds = loadAttachedMailIds(mailIds);

		return page.map(mine -> MailDto.ListResponse.of(
				mine.getMail(),
				recipientsByMail.getOrDefault(mine.getMail().getId(), List.of()),
				mine.getReadAt() != null,
				attachedMailIds.contains(mine.getMail().getId())));
	}

	// 보낸편지함
	public Page<MailDto.ListResponse> getSent(Long myId, String keyword, Pageable pageable) {
		Page<Mail> page = mailRepository.findSent(myId, normalize(keyword), pageable);
		List<Long> mailIds = page.getContent().stream().map(Mail::getId).toList();
		Map<Long, List<MailRecipient>> recipientsByMail = loadRecipients(mailIds);
		Set<Long> attachedMailIds = loadAttachedMailIds(mailIds);

		return page.map(mail -> MailDto.ListResponse.of(
				mail, recipientsByMail.getOrDefault(mail.getId(), List.of()), true,
				attachedMailIds.contains(mail.getId())));
	}

	// 안 읽은 메일 수
	public long getUnreadCount(Long myId) {
		return mailRecipientRepository.countByRecipientIdAndIsDeletedFalseAndReadAtIsNull(myId);
	}

	// 상세 조회 — 보낸 사람이거나 수신자인 경우만 볼 수 있고, 수신자가 처음 열면 읽음 처리한다
	@Transactional
	public MailDto.DetailResponse getDetail(Long myId, Long mailId) {
		Mail mail = mailRepository.findById(mailId)
				.orElseThrow(() -> new CustomException(ErrorCode.MAIL_NOT_FOUND));
		Optional<MailRecipient> mine = mailRecipientRepository.findByMailIdAndRecipientId(mailId, myId)
				.filter(recipient -> !recipient.getIsDeleted());
		boolean sentByMe = mail.getSender().getId().equals(myId) && !mail.getIsSenderDeleted();

		// 남의 메일은 존재 여부도 알려주지 않는다
		if (mine.isEmpty() && !sentByMe) {
			throw new CustomException(ErrorCode.MAIL_NOT_FOUND);
		}

		if (mine.isPresent() && mine.get().getReadAt() == null) {
			mine.get().markRead(LocalDateTime.now());
			notificationService.readNotification(myId, TARGET_TYPE, mailId);
		}

		return MailDto.DetailResponse.of(mail, mailRecipientRepository.findByMailIds(List.of(mailId)), sentByMe);
	}

	// 메일 발송 — 받는 사람과 참조에 같은 사람이 있으면 받는 사람으로만 처리한다
	@Transactional
	public MailDto.DetailResponse send(Long myId, MailDto.SendRequest request) {
		Employee sender = employeeRepository.findById(myId)
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

		Set<Long> toIds = new LinkedHashSet<>(request.getToIds());
		Set<Long> ccIds = new LinkedHashSet<>(request.getCcIds() != null ? request.getCcIds() : List.of());
		ccIds.removeAll(toIds);

		Mail mail = Mail.builder()
				.sender(sender)
				.subject(request.getSubject().trim())
				.content(request.getContent())
				.build();
		findEmployees(toIds).forEach(employee -> mail.addRecipient(employee, RecipientType.TO));
		findEmployees(ccIds).forEach(employee -> mail.addRecipient(employee, RecipientType.CC));

		Mail saved = mailRepository.save(mail);

		// 수신자에게 알림 (나에게 보낸 메일은 알리지 않는다)
		notificationService.sendToMany(
				saved.getRecipients().stream().map(recipient -> recipient.getRecipient().getId()).toList(),
				myId,
				NotificationType.MAIL,
				sender.getName() + "님이 메일을 보냈습니다: " + saved.getSubject(),
				TARGET_TYPE,
				saved.getId());

		return MailDto.DetailResponse.of(saved, saved.getRecipients(), true);
	}

	// 삭제 — 내 메일함에서만 지운다 (상대방 메일함에는 남는다)
	@Transactional
	public void delete(Long myId, Long mailId, MailBox box) {
		if (box == MailBox.SENT) {
			Mail mail = mailRepository.findById(mailId)
					.filter(found -> found.getSender().getId().equals(myId))
					.orElseThrow(() -> new CustomException(ErrorCode.MAIL_NOT_FOUND));
			mail.deleteBySender();
			return;
		}
		MailRecipient mine = mailRecipientRepository.findByMailIdAndRecipientId(mailId, myId)
				.orElseThrow(() -> new CustomException(ErrorCode.MAIL_NOT_FOUND));
		mine.delete();
	}

	private List<Employee> findEmployees(Set<Long> ids) {
		List<Employee> employees = employeeRepository.findAllById(ids);
		if (employees.size() != ids.size()) {
			throw new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND);
		}
		return employees;
	}

	private Map<Long, List<MailRecipient>> loadRecipients(List<Long> mailIds) {
		if (mailIds.isEmpty()) {
			return Map.of();
		}
		return mailRecipientRepository.findByMailIds(mailIds).stream()
				.collect(Collectors.groupingBy(recipient -> recipient.getMail().getId()));
	}

	// 목록의 메일 중 첨부가 있는 메일 id — 한 번의 쿼리로 추린다
	private Set<Long> loadAttachedMailIds(List<Long> mailIds) {
		if (mailIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(fileAttachmentRepository.findRefIdsWithAttachment(RefType.MAIL, mailIds));
	}

	private String normalize(String keyword) {
		return StringUtils.hasText(keyword) ? keyword.trim() : null;
	}
}
