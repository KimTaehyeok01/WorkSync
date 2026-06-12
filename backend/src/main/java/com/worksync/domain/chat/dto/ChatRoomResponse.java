package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.ChatRoom;
import com.worksync.domain.chat.entity.RoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Builder
public class ChatRoomResponse {

    private Long id;
    private RoomType roomType;
    private String name;
    private Long createdById;
    private String createdByName;
    private List<Long> memberIds;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .roomType(room.getRoomType())
                .name(room.getName())
                .createdById(room.getCreatedBy().getId())
                .createdByName(room.getCreatedBy().getName())
                .memberIds(room.getMembers().stream()
                        .map(m -> m.getEmployee().getId())
                        .collect(Collectors.toList()))
                .createdAt(room.getCreatedAt())
                .build();
    }
}
