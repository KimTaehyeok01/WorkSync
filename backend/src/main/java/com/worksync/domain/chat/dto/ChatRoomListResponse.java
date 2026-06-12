package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.RoomType;
import com.worksync.domain.employee.entity.EmployeeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class ChatRoomListResponse {

    private Long id;
    private RoomType roomType;
    private String name;
    private String thumbnailImage;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private EmployeeStatus otherStatus;
}
