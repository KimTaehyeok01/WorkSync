package com.worksync.domain.chat.controller;

import com.worksync.domain.chat.dto.*;
import com.worksync.domain.chat.service.ChatService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // POST /api/chat/rooms — 채팅방 생성 (1:1 or 그룹)
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRoomCreateRequest request) {

        ChatRoomResponse response = chatService.createRoom(userDetails.getId(), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(response));
    }

    // GET /api/chat/rooms — 내 채팅방 목록
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword) {

        List<ChatRoomListResponse> response = chatService.getMyRooms(userDetails.getId(), keyword);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // GET /api/chat/rooms/{roomId}/messages — 메시지 목록
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "30") int size) {

        List<MessageResponse> response = chatService.getMessages(roomId, userDetails.getId(), lastMessageId, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // POST /api/chat/rooms/{roomId}/messages — 메시지 전송
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody MessageSendRequest request) {

        MessageResponse response = chatService.sendMessage(roomId, userDetails.getId(), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(response));
    }

    // PUT /api/chat/rooms/{roomId}/read — 읽음 처리
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.readMessages(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PUT /api/chat/rooms/{roomId}/enter — 채팅방 입장
    @PutMapping("/rooms/{roomId}/enter")
    public ResponseEntity<ApiResponse<Void>> enterRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.enterRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PUT /api/chat/rooms/{roomId}/leave — 채팅방 퇴장
    @PutMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.leaveRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // GET /api/chat/rooms/{roomId}/members — 구성원 목록
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<ApiResponse<List<ChatMemberResponse>>> getRoomMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        List<ChatMemberResponse> response = chatService.getRoomMembers(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // GET /api/chat/rooms/{roomId}/files — 공유 파일 목록
    @GetMapping("/rooms/{roomId}/files")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getRoomFiles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        List<MessageResponse> response = chatService.getRoomFiles(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
