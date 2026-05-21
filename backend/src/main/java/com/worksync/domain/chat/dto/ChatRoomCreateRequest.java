package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.RoomType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ChatRoomCreateRequest {

    @NotNull
    private RoomType roomType;

    private String name;

    @NotEmpty
    private List<Long> memberIds;
}
