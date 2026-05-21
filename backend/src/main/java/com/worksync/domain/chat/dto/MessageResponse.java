package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private String content;
    private Message.MsgType msgType;
    private Long fileId;
    private LocalDateTime sentAt;
}
