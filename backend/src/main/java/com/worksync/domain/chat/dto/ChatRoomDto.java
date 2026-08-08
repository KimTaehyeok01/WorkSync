// 채팅방/멤버 도메인 요청/응답 DTO 모음
package com.worksync.domain.chat.dto;

import com.worksync.domain.chat.entity.ChatMember;
import com.worksync.domain.chat.entity.ChatRoom;
import com.worksync.domain.chat.entity.RoomType;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.entity.JobGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ChatRoomDto {

    @Getter @Setter
    @Schema(name = "ChatRoomDto.CreateRequest")
    public static class CreateRequest {

        @NotNull
        @Schema(description = "채팅방 유형 (DIRECT, GROUP)", example = "DIRECT")
        private RoomType roomType;

        @Schema(description = "채팅방 이름 (그룹 채팅방용)", example = "개발팀 단톡방")
        private String name;

        @NotEmpty
        @Schema(description = "초대할 직원 ID 목록")
        private List<Long> memberIds;
    }

    @Getter @Builder
    @Schema(name = "ChatRoomDto.Response")
    public static class Response {

        @Schema(description = "채팅방 ID", example = "1")
        private Long id;

        @Schema(description = "채팅방 유형 (DIRECT, GROUP)", example = "DIRECT")
        private RoomType roomType;

        @Schema(description = "채팅방 이름", example = "개발팀 단톡방")
        private String name;

        @Schema(description = "생성자 ID", example = "1")
        private Long createdById;

        @Schema(description = "생성자 이름", example = "홍길동")
        private String createdByName;

        @Schema(description = "참여 직원 ID 목록")
        private List<Long> memberIds;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(ChatRoom room) {
            return Response.builder()
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

    @Getter @Builder
    @Schema(name = "ChatRoomDto.ListResponse")
    public static class ListResponse {

        @Schema(description = "채팅방 ID", example = "1")
        private Long id;

        @Schema(description = "채팅방 유형 (DIRECT, GROUP)", example = "DIRECT")
        private RoomType roomType;

        @Schema(description = "채팅방 이름", example = "개발팀 단톡방")
        private String name;

        @Schema(description = "썸네일 이미지 URL")
        private String thumbnailImage;

        @Schema(description = "마지막 메시지 내용", example = "네 알겠습니다.")
        private String lastMessage;

        @Schema(description = "마지막 메시지 일시")
        private LocalDateTime lastMessageAt;

        @Schema(description = "안 읽은 메시지 수", example = "3")
        private int unreadCount;

        @Schema(description = "상대방 상태 (ACTIVE, INACTIVE, AWAY) — 1:1 채팅방용", example = "ACTIVE")
        private EmployeeStatus otherStatus;
    }

    @Getter @Builder
    @Schema(name = "ChatRoomDto.MemberResponse")
    public static class MemberResponse {

        @Schema(description = "직원 ID", example = "1")
        private Long employeeId;

        @Schema(description = "직원 이름", example = "홍길동")
        private String name;

        @Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, CEO)", example = "STAFF")
        private JobGrade jobGrade;

        @Schema(description = "프로필 이미지 URL")
        private String profileImage;

        @Schema(description = "직원 상태 (ACTIVE, INACTIVE, AWAY)", example = "ACTIVE")
        private EmployeeStatus status;

        public static MemberResponse from(ChatMember member) {
            return MemberResponse.builder()
                    .employeeId(member.getEmployee().getId())
                    .name(member.getEmployee().getName())
                    .jobGrade(member.getEmployee().getJobGrade())
                    .profileImage(member.getEmployee().getProfileImage())
                    .status(member.getEmployee().getStatus())
                    .build();
        }
    }
}
