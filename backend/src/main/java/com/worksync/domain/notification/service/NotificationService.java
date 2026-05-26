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
    @Transactional
    public void readNotification(Long notificationId, Long myId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getId().equals(myId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        notification.markAsRead();
    }

    // 전체 읽음 처리
    @Transactional
    public void readAllNotifications(Long myId) {
        notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(myId)
                .forEach(Notification::markAsRead);
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
    }
}
