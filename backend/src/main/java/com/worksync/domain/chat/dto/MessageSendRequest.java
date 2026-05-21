package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.Message;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequest {

    @NotNull
    private Message.MsgType msgType;

    @Size(max = 300)
    private String content;

    private Long fileId;
}
