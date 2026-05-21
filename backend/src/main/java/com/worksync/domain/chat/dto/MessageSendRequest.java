package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MessageSendRequest {

    @NotNull
    private Long roomId;

    private String content;

    private MessageType msgType;

    private Long fileId;
}
