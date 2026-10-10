package com.worksync.domain.notification.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.dto.NotificationDto;
import com.worksync.domain.notification.entity.Notification;
import com.worksync.domain.notification.entity.NotificationSetting;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.repository.NotificationRepository;
import com.worksync.domain.notification.repository.NotificationSettingRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 내 알림 목록 조회
    public List<NotificationDto.Response> getMyNotifications(Long myId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(myId)
                : notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId);

        return notifications.stream()
                .map(NotificationDto.Response::from)
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
        log.debug("읽음 처리 완료 : {}", myId);

        // (webSocket) 읽음 처리 후 알림 목록 갱신
        List<NotificationDto.Response> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId)
                .stream()
                .map(NotificationDto.Response::from)
                .collect(Collectors.toList());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(myId),
                "/queue/notifications",
                notifications
        );
        log.debug("목록 갱신 완료 : {}", myId);
    }

    // 알림 센터 목록 — 유형·안 읽음 필터, 페이징
    public Page<NotificationDto.Response> getCenter(Long myId, NotificationType type, boolean unreadOnly,
                                                    Pageable pageable) {
        Page<Notification> page;
        if (type == null) {
            page = unreadOnly
                    ? notificationRepository.findByReceiverIdAndIsReadFalse(myId, pageable)
                    : notificationRepository.findByReceiverId(myId, pageable);
        } else {
            page = unreadOnly
                    ? notificationRepository.findByReceiverIdAndTypeAndIsReadFalse(myId, type, pageable)
                    : notificationRepository.findByReceiverIdAndType(myId, type, pageable);
        }
        return page.map(NotificationDto.Response::from);
    }

    // 알림 1건 읽음 처리 (알림 센터용)
    @Transactional
    public void readOne(Long myId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, myId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
        notificationRepository.flush();
        publish(myId);
    }

    // 내 알림 전체 읽음 처리
    @Transactional
    public void readAll(Long myId) {
        notificationRepository.markAllAsRead(myId, LocalDateTime.now());
        publish(myId);
    }

    // 알림 수신 설정 조회 — 설정한 적 없는 유형은 수신으로 본다
    public List<NotificationDto.SettingResponse> getSettings(Long myId) {
        Map<NotificationType, Boolean> saved = notificationSettingRepository.findByEmployeeId(myId).stream()
                .collect(Collectors.toMap(NotificationSetting::getType, NotificationSetting::getIsEnabled));

        return Arrays.stream(NotificationType.values())
                .map(type -> new NotificationDto.SettingResponse(type, saved.getOrDefault(type, true)))
                .toList();
    }

    // 알림 수신 설정 변경
    @Transactional
    public void updateSetting(Long myId, NotificationType type, boolean enabled) {
        notificationSettingRepository.findByEmployeeIdAndType(myId, type)
                .ifPresentOrElse(
                        setting -> setting.changeEnabled(enabled),
                        () -> notificationSettingRepository.save(NotificationSetting.builder()
                                .employee(employeeRepository.findById(myId)
                                        .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND)))
                                .type(type)
                                .isEnabled(enabled)
                                .build()));
    }

    // (webSocket) 안 읽은 수와 알림 목록을 본인에게 다시 전송
    private void publish(Long myId) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(myId),
                "/queue/notifications/unread-count",
                notificationRepository.countByReceiverIdAndIsReadFalse(myId));
        messagingTemplate.convertAndSendToUser(
                String.valueOf(myId),
                "/queue/notifications",
                notificationRepository.findByReceiverIdOrderByCreatedAtDesc(myId).stream()
                        .map(NotificationDto.Response::from)
                        .toList());
    }

    // 여러 명에게 같은 알림 전송 (내부용) — 보낸 사람 본인(excludeId)은 제외, 중복 수신자는 한 번만
    @Transactional
    public void sendToMany(Collection<Long> receiverIds, Long excludeId, NotificationType type,
                           String content, String targetType, Long targetId) {
        receiverIds.stream()
                .distinct()
                .filter(receiverId -> !receiverId.equals(excludeId))
                .forEach(receiverId -> send(receiverId, type, content, targetType, targetId));
    }

    // 알림 전송 (내부용 — 다른 서비스에서 호출)
    @Transactional
    public void send(Long receiverId, NotificationType type, String content,
                     String targetType, Long targetId) {

        // 수신자가 이 유형의 알림을 꺼 두었으면 보내지 않는다
        if (notificationSettingRepository.existsByEmployeeIdAndTypeAndIsEnabledFalse(receiverId, type)) {
            return;
        }

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
        log.debug("실시간 알림 전송 : {}", receiverId);

        // (webSocket) 실시간 목록 전송
        List<NotificationDto.Response> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId)
                .stream()
                .map(NotificationDto.Response::from)
                .collect(Collectors.toList());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/notifications",
                notifications
        );
        log.debug("실시간 목록 전송 : {}", receiverId);
    }
}
