package com.worksync.domain.notification.controller;

import com.worksync.domain.notification.dto.NotificationDto;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notification", description = "알림 API")
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 (안읽은 것만)
    @Operation(summary = "내 알림 목록 조회 (unreadOnly=true 시 안읽은 것만)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto.Response>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        List<NotificationDto.Response> response =
                notificationService.getMyNotifications(userDetails.getId(), unreadOnly);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 안읽은 알림 수
    @Operation(summary = "안읽은 알림 수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        long count = notificationService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    // 읽음 처리
    @Operation(summary = "알림 읽음 처리 (targetType + targetId 일치 건)")
    @PutMapping("/read")
    public ResponseEntity<ApiResponse<Void>> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody NotificationDto.Request request) {

        notificationService.readNotification(userDetails.getId(), request.getTargetType(), request.getTargetId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 전체 읽음 처리
//    @PutMapping("/read-all")
//    public ResponseEntity<ApiResponse<Void>> readAllNotifications(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @RequestParam String targetType,
//            @RequestParam Long targetId) {
//
//        notificationService.readAllNotifications(userDetails.getId(), targetType, targetId);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
}
