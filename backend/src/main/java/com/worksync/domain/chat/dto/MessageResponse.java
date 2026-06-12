package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.Message;
import com.worksync.domain.chat.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class MessageResponse {

    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private String content;
    private MessageType msgType;
    private Long fileId;
    private LocalDateTime sentAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getName() : null)
                .senderProfileImage(message.getSender() != null ? message.getSender().getProfileImage() : null)
                .content(message.getContent())
                .msgType(message.getMsgType())
                .fileId(message.getFileId())
                .sentAt(message.getSentAt())
                .build();
    }
}
