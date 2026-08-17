// NotificationService 단위 테스트
package com.worksync.domain.notification.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.dto.NotificationDto;
import com.worksync.domain.notification.entity.Notification;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.repository.NotificationRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** NotificationService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    private Notification buildNotification(Long id, Employee receiver, NotificationType type, String content,
                                             String targetType, Long targetId, boolean isRead) {
        return Notification.builder()
                .id(id)
                .receiver(receiver)
                .type(type)
                .content(content)
                .targetType(targetType)
                .targetId(targetId)
                .isRead(isRead)
                .build();
    }

    @DisplayName("안읽은 알림만 조회하면 안읽은 알림 목록을 반환한다")
    @Test
    void getMyNotifications_unreadOnlyTrue_returnsUnreadList() {
        // given
        Employee receiver = buildEmployee(1L, "김철수");
        Notification unread = buildNotification(1L, receiver, NotificationType.APPROVAL, "내용", "APPROVAL", 10L, false);
        given(notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .willReturn(List.of(unread));

        // when
        List<NotificationDto.Response> result = notificationService.getMyNotifications(1L, true);

        // then
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(1L);
        verify(notificationRepository, never()).findByReceiverIdOrderByCreatedAtDesc(any());
    }

    @DisplayName("전체 알림을 조회하면 전체 알림 목록을 반환한다")
    @Test
    void getMyNotifications_unreadOnlyFalse_returnsAllList() {
        // given
        Employee receiver = buildEmployee(1L, "김철수");
        Notification n1 = buildNotification(1L, receiver, NotificationType.APPROVAL, "내용1", "APPROVAL", 10L, false);
        Notification n2 = buildNotification(2L, receiver, NotificationType.TASK, "내용2", "TASK", 20L, true);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L)).willReturn(List.of(n1, n2));

        // when
        List<NotificationDto.Response> result = notificationService.getMyNotifications(1L, false);

        // then
        assertThat(result).hasSize(2);
        verify(notificationRepository).findByReceiverIdOrderByCreatedAtDesc(1L);
    }

    @DisplayName("안읽은 알림 수를 조회하면 레포지토리 결과를 그대로 반환한다")
    @Test
    void getUnreadCount_success() {
        // given
        given(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).willReturn(3L);

        // when
        long result = notificationService.getUnreadCount(1L);

        // then
        assertThat(result).isEqualTo(3L);
    }

    @DisplayName("대상이 일치하는 알림을 읽음 처리하고 실시간으로 결과를 전송한다")
    @Test
    void readNotification_matchingNotification_marksAsReadAndBroadcasts() {
        // given
        Employee receiver = buildEmployee(1L, "김철수");
        Notification matching = buildNotification(1L, receiver, NotificationType.APPROVAL, "내용", "APPROVAL", 10L, false);
        Notification nonMatching = buildNotification(2L, receiver, NotificationType.TASK, "다른내용", "TASK", 20L, false);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(matching, nonMatching));
        given(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).willReturn(1L);

        // when
        notificationService.readNotification(1L, "APPROVAL", 10L);

        // then
        assertThat(matching.getIsRead()).isTrue();
        assertThat(nonMatching.getIsRead()).isFalse();
        verify(notificationRepository).flush();
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications/unread-count"), eq(1L));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any(List.class));
    }

    @DisplayName("대상이 일치하는 알림이 없으면 아무 것도 읽음 처리하지 않는다")
    @Test
    void readNotification_noMatchingNotification_noneMarkedButBroadcasts() {
        // given
        Employee receiver = buildEmployee(1L, "김철수");
        Notification notification = buildNotification(1L, receiver, NotificationType.APPROVAL, "내용", "APPROVAL", 10L, false);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L)).willReturn(List.of(notification));
        given(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).willReturn(1L);

        // when
        notificationService.readNotification(1L, "TASK", 99L);

        // then
        assertThat(notification.getIsRead()).isFalse();
        verify(notificationRepository).flush();
    }

    @DisplayName("알림을 전송하면 저장되고 실시간으로 전달된다")
    @Test
    void send_success_savesAndBroadcasts() {
        // given
        Employee receiver = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(receiver));
        given(notificationRepository.countByReceiverIdAndIsReadFalse(1L)).willReturn(1L);
        given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        // when
        notificationService.send(1L, NotificationType.APPROVAL, "새 결재 요청이 도착했습니다.", "APPROVAL", 10L);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications/unread-count"), eq(1L));
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any(List.class));
    }

    @DisplayName("수신자가 존재하지 않으면 알림을 전송하지 않고 예외가 발생한다")
    @Test
    void send_receiverNotFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.send(999L, NotificationType.APPROVAL, "내용", "APPROVAL", 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(messagingTemplate);
    }
}
