package com.worksync.domain.notification.repository;

import com.worksync.domain.notification.entity.Notification;
import com.worksync.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    // 알림 센터 — 유형·안 읽음 필터 조합별 페이징 조회
    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndIsReadFalse(Long receiverId, Pageable pageable);

    Page<Notification> findByReceiverIdAndType(Long receiverId, NotificationType type, Pageable pageable);

    Page<Notification> findByReceiverIdAndTypeAndIsReadFalse(Long receiverId, NotificationType type, Pageable pageable);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    // 내 안 읽은 알림 전체 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.receiver.id = :receiverId AND n.isRead = false")
    int markAllAsRead(@Param("receiverId") Long receiverId, @Param("now") LocalDateTime now);
}
