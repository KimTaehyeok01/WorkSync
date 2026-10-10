// 메일 수신자 Repository
package com.worksync.domain.mail.repository;

import com.worksync.domain.mail.entity.MailRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 메일 수신자 Repository. */
public interface MailRecipientRepository extends JpaRepository<MailRecipient, Long> {

	// 받은편지함 — 내가 수신자이고 지우지 않은 메일 (unreadOnly 면 안 읽은 것만)
	@Query(value = "SELECT r FROM MailRecipient r JOIN FETCH r.mail m JOIN FETCH m.sender " +
			"WHERE r.recipient.id = :recipientId AND r.isDeleted = false " +
			"AND (:unreadOnly = false OR r.readAt IS NULL) " +
			"AND (CAST(:keyword AS string) IS NULL OR m.subject LIKE CONCAT('%', CAST(:keyword AS string), '%')) " +
			"ORDER BY r.createdAt DESC",
			countQuery = "SELECT COUNT(r) FROM MailRecipient r " +
					"WHERE r.recipient.id = :recipientId AND r.isDeleted = false " +
					"AND (:unreadOnly = false OR r.readAt IS NULL) " +
					"AND (CAST(:keyword AS string) IS NULL OR r.mail.subject LIKE CONCAT('%', CAST(:keyword AS string), '%'))")
	Page<MailRecipient> findInbox(@Param("recipientId") Long recipientId,
			@Param("unreadOnly") boolean unreadOnly,
			@Param("keyword") String keyword,
			Pageable pageable);

	long countByRecipientIdAndIsDeletedFalseAndReadAtIsNull(Long recipientId);

	Optional<MailRecipient> findByMailIdAndRecipientId(Long mailId, Long recipientId);

	// 여러 메일의 수신자를 한 번에 조회 (목록의 수신자 요약, 상세의 수신자 목록)
	@Query("SELECT r FROM MailRecipient r JOIN FETCH r.recipient " +
			"WHERE r.mail.id IN :mailIds ORDER BY r.id")
	List<MailRecipient> findByMailIds(@Param("mailIds") List<Long> mailIds);
}