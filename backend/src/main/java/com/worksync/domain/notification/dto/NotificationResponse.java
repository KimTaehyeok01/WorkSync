package com.worksync.domain.notification.dto;

import com.worksync.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Notification.Type type;
    private String content;
    private String targetType;
    private Long targetId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
