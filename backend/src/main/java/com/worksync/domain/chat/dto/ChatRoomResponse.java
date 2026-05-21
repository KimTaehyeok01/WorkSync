package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.ChatRoom;
import com.worksync.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private Long id;
    private ChatRoom.RoomType roomType;
    private String name;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private List<MemberDto> members;
    private LocalDateTime createdAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberDto {
        private Long id;
        private String name;
        private String profileImage;
        private Employee.Status status;
        private Employee.JobGrade jobGrade;
    }
}
