package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.ChatRoom;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomCreateRequest {

    @NotNull
    private ChatRoom.RoomType roomType;

    private String name;

    @NotEmpty
    private List<Long> memberIds;
}
