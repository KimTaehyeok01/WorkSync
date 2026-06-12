package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.MessageType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MessageSendRequest {

    @Size(max = 300, message = "메시지는 300자 이하여야 합니다.")
    private String content;

    private MessageType msgType;

    private Long fileId;
}
