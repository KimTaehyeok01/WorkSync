package com.worksync.domain.notification.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.dto.NotificationResponse;
import com.worksync.domain.notification.entity.Notification;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.repository.NotificationRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 내 알림 목록 조회
    public List<NotificationResponse> getMyNotifications(Long myId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(myId)
                : notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId);

        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // 안읽은 알림 수
    public long getUnreadCount(Long myId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(myId);
    }

    // 단건 읽음 처리
    @Transactional(readOnly = false)
    public void readNotification(Long myId, String targetType, Long targetId) {
        // targetType&&targetId 같은 알림만 읽음 처리
        notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId)
                .stream()
                .filter(notification -> targetType.equals(notification.getTargetType()) && targetId.equals(notification.getTargetId()))
                .forEach(Notification::markAsRead);

        notificationRepository.flush();

        // (webSocket) 읽음 처리 후 실시간 전송
        Long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalse(myId);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(myId),
                "/queue/notifications/unread-count",
                unreadCount
        );
        System.out.println("읽음 처리 완료 : " + myId);

        // (webSocket) 읽음 처리 후 알림 목록 갱신
        List<NotificationResponse> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(myId),
                "/queue/notifications",
                notifications
        );
        System.out.println("목록 갱신 완료 : " + myId);
    }

    // 알림 전송 (내부용 — 다른 서비스에서 호출)
    @Transactional
    public void send(Long receiverId, NotificationType type, String content,
                     String targetType, Long targetId) {

        Employee receiver = employeeRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .content(content)
                .targetType(targetType)
                .targetId(targetId)
                .build();

        notificationRepository.save(notification);

        // (webSocket) 실시간 알림 전송 - isRead = false 인 것만 카운트
        Long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/notifications/unread-count",
                unreadCount
        );
        System.out.println("실시간 알림 전송 : " + receiverId);

        // (webSocket) 실시간 목록 전송
        List<NotificationResponse> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/notifications",
                notifications
        );
        System.out.println("실시간 목록 전송 : " + receiverId);
    }
}
