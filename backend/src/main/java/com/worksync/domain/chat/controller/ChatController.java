package com.worksync.domain.chat.controller;

import com.worksync.domain.chat.dto.ChatRoomDto;
import com.worksync.domain.chat.dto.MessageDto;
import com.worksync.domain.chat.service.ChatService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "채팅 API")
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // POST /api/chat/rooms — 채팅방 생성 (1:1 or 그룹)
    @Operation(summary = "채팅방 생성 (1:1 또는 그룹)")
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomDto.Response>> createRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRoomDto.CreateRequest request) {

        ChatRoomDto.Response response = chatService.createRoom(userDetails.getId(), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(response));
    }

    // GET /api/chat/rooms — 내 채팅방 목록
    @Operation(summary = "내 채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomDto.ListResponse>>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword) {

        List<ChatRoomDto.ListResponse> response = chatService.getMyRooms(userDetails.getId(), keyword);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // GET /api/chat/rooms/{roomId}/messages — 메시지 목록
    @Operation(summary = "채팅방 메시지 목록 조회")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto.Response>>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "30") int size) {

        List<MessageDto.Response> response = chatService.getMessages(roomId, userDetails.getId(), lastMessageId, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // POST /api/chat/rooms/{roomId}/messages — 메시지 전송
    @Operation(summary = "채팅 메시지 전송")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<MessageDto.Response>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody MessageDto.SendRequest request) {

        MessageDto.Response response = chatService.sendMessage(roomId, userDetails.getId(), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(response));
    }

    // PUT /api/chat/rooms/{roomId}/read — 읽음 처리
    @Operation(summary = "채팅방 메시지 읽음 처리")
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.readMessages(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PUT /api/chat/rooms/{roomId}/enter — 채팅방 입장
    @Operation(summary = "채팅방 입장")
    @PutMapping("/rooms/{roomId}/enter")
    public ResponseEntity<ApiResponse<Void>> enterRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.enterRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PUT /api/chat/rooms/{roomId}/leave — 채팅방 퇴장
    @Operation(summary = "채팅방 퇴장")
    @PutMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        chatService.leaveRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // GET /api/chat/rooms/{roomId}/members — 구성원 목록
    @Operation(summary = "채팅방 구성원 목록 조회")
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<ApiResponse<List<ChatRoomDto.MemberResponse>>> getRoomMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        List<ChatRoomDto.MemberResponse> response = chatService.getRoomMembers(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // GET /api/chat/rooms/{roomId}/summary — 대화 요약
    @Operation(summary = "채팅방 대화 AI 요약 조회")
    @GetMapping("/rooms/{roomId}/summary")
    public ResponseEntity<ApiResponse<String>> summarizeRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        String summary = chatService.summarizeRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    // GET /api/chat/rooms/{roomId}/files — 공유 파일 목록
    @Operation(summary = "채팅방 공유 파일 목록 조회")
    @GetMapping("/rooms/{roomId}/files")
    public ResponseEntity<ApiResponse<List<MessageDto.Response>>> getRoomFiles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {

        List<MessageDto.Response> response = chatService.getRoomFiles(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
