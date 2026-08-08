// 메시지 도메인 요청/응답 DTO 모음
package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.Message;
import com.worksync.domain.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class MessageDto {

    @Getter @Setter
    @Schema(name = "MessageDto.SendRequest")
    public static class SendRequest {

        @Size(max = 300, message = "메시지는 300자 이하여야 합니다.")
        @Schema(description = "메시지 내용", maxLength = 300, example = "안녕하세요.")
        private String content;

        @Schema(description = "메시지 유형 (TEXT, FILE, IMAGE, SYSTEM)", example = "TEXT")
        private MessageType msgType;

        @Schema(description = "첨부 파일 ID (파일/이미지 메시지용)", example = "1")
        private Long fileId;
    }

    @Getter @Builder
    @Schema(name = "MessageDto.Response")
    public static class Response {

        @Schema(description = "메시지 ID", example = "1")
        private Long id;

        @Schema(description = "채팅방 ID", example = "1")
        private Long roomId;

        @Schema(description = "발신자 ID (시스템 메시지면 null)", example = "1")
        private Long senderId;

        @Schema(description = "발신자 이름 (시스템 메시지면 null)", example = "홍길동")
        private String senderName;

        @Schema(description = "발신자 프로필 이미지 URL")
        private String senderProfileImage;

        @Schema(description = "메시지 내용", example = "안녕하세요.")
        private String content;

        @Schema(description = "메시지 유형 (TEXT, FILE, IMAGE, SYSTEM)", example = "TEXT")
        private MessageType msgType;

        @Schema(description = "첨부 파일 ID (없으면 null)", example = "1")
        private Long fileId;

        @Schema(description = "발신 일시")
        private LocalDateTime sentAt;

        public static Response from(Message message) {
            return Response.builder()
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
}
