// 사내 메일 Repository
package com.worksync.domain.mail.repository;

import com.worksync.domain.mail.entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 사내 메일 Repository. */
public interface MailRepository extends JpaRepository<Mail, Long> {

	// 보낸편지함 — 내가 보냈고 지우지 않은 메일
	@Query(value = "SELECT m FROM Mail m JOIN FETCH m.sender " +
			"WHERE m.sender.id = :senderId AND m.isSenderDeleted = false " +
			"AND (CAST(:keyword AS string) IS NULL OR m.subject LIKE CONCAT('%', CAST(:keyword AS string), '%')) " +
			"ORDER BY m.createdAt DESC",
			countQuery = "SELECT COUNT(m) FROM Mail m " +
					"WHERE m.sender.id = :senderId AND m.isSenderDeleted = false " +
					"AND (CAST(:keyword AS string) IS NULL OR m.subject LIKE CONCAT('%', CAST(:keyword AS string), '%'))")
	Page<Mail> findSent(@Param("senderId") Long senderId,
			@Param("keyword") String keyword,
			Pageable pageable);
}