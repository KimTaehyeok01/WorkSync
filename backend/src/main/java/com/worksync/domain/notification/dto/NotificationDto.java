// 알림 도메인 요청/응답 DTO 모음
package com.worksync.domain.notification.dto;

import com.worksync.domain.notification.entity.Notification;
import com.worksync.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @Setter
    @Schema(name = "NotificationDto.Request")
    public static class Request {

        @Schema(description = "읽음 처리할 알림 대상 유형", example = "APPROVAL")
        private String targetType;

        @Schema(description = "읽음 처리할 알림 대상 ID", example = "10")
        private Long targetId;
    }

    @Getter @Builder
    @Schema(name = "NotificationDto.Response")
    public static class Response {

        @Schema(description = "알림 ID", example = "1")
        private Long id;

        @Schema(description = "알림 유형 (APPROVAL, TASK, MESSAGE)", example = "APPROVAL")
        private NotificationType type;

        @Schema(description = "알림 내용", example = "새 결재 요청이 도착했습니다.")
        private String content;

        @Schema(description = "알림 대상 유형", example = "APPROVAL")
        private String targetType;

        @Schema(description = "알림 대상 ID", example = "10")
        private Long targetId;

        @Schema(description = "읽음 여부", example = "false")
        private Boolean isRead;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "읽은 일시 — 안읽은 알림은 null")
        private LocalDateTime readAt;

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .content(notification.getContent())
                    .targetType(notification.getTargetType())
                    .targetId(notification.getTargetId())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .readAt(notification.getReadAt())
                    .build();
        }
    }
}
