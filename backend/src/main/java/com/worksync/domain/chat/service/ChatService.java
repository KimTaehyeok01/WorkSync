package com.worksync.domain.chat.service;

import com.worksync.domain.chat.dto.*;
import com.worksync.domain.chat.entity.*;
import com.worksync.domain.chat.repository.ChatMemberRepository;
import com.worksync.domain.chat.repository.ChatRoomRepository;
import com.worksync.domain.chat.repository.MessageRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅방 생성
    @Transactional
    public ChatRoomResponse createRoom(Long myId, ChatRoomCreateRequest request) {

        Employee creator = employeeRepository.findById(myId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // 1:1 채팅 — 이미 존재하는 방이면 기존 방 반환
        if (request.getRoomType() == RoomType.DIRECT) {
            if (request.getMemberIds().size() != 1) {
                throw new IllegalArgumentException("1:1 대화는 상대방을 한 명만 선택해야 합니다.");
            }
            Long targetId = request.getMemberIds().get(0);
            return chatRoomRepository.findDirectRoom(RoomType.DIRECT, myId, targetId)
                    .map(ChatRoomResponse::from)
                    .orElseGet(() -> buildAndSaveRoom(creator, request));
        }

        // 그룹 채팅 — 이름 필수
        if (request.getRoomType() == RoomType.GROUP &&
                (request.getName() == null || request.getName().isBlank())) {
            throw new IllegalArgumentException("그룹 채팅방 이름을 입력해주세요.");
        }

        return buildAndSaveRoom(creator, request);
    }

    private ChatRoomResponse buildAndSaveRoom(Employee creator, ChatRoomCreateRequest request) {

        ChatRoom room = ChatRoom.builder()
                .roomType(request.getRoomType())
                .name(request.getName())
                .createdBy(creator)
                .build();

        // 생성자 + 요청 멤버 모두 포함 (중복 제거)
        Set<Long> allMemberIds = new LinkedHashSet<>();
        allMemberIds.add(creator.getId());
        allMemberIds.addAll(request.getMemberIds());

        for (Long memberId : allMemberIds) {
            Employee emp = employeeRepository.findById(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
            ChatMember member = ChatMember.builder()
                    .room(room)
                    .employee(emp)
                    .build();
            room.getMembers().add(member);
        }

        chatRoomRepository.save(room); // CascadeType.ALL → 멤버도 함께 저장

        // 시스템 메시지 자동 저장
        String systemContent = room.getRoomType() == RoomType.DIRECT
                ? "대화가 시작되었습니다."
                : "[" + room.getName() + "] 채팅방이 생성되었습니다.";

        Message systemMessage = Message.builder()
                .room(room)
                .sender(null)
                .content(systemContent)
                .msgType(MessageType.SYSTEM)
                .build();
        messageRepository.save(systemMessage);
        room.updateLastMessageAt(LocalDateTime.now());

        return ChatRoomResponse.from(room);
    }

    // 내 채팅방 목록
    public List<ChatRoomListResponse> getMyRooms(Long myId, String keyword) {

        return chatRoomRepository.findMyRooms(myId).stream()
                .filter(room -> matchesKeyword(room, myId, keyword))
                .map(room -> buildListResponse(room, myId))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(ChatRoom room, Long myId, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        // 본인 제외한 멤버 이름으로만 검색
        return room.getMembers().stream()
                .filter(m -> !m.getEmployee().getId().equals(myId))
                .anyMatch(m -> m.getEmployee().getName().contains(keyword));
    }

    private ChatRoomListResponse buildListResponse(ChatRoom room, Long myId) {

        // 이름 / 썸네일 결정
        String name;
        String thumbnailImage = null;

        EmployeeStatus otherStatus = null;
        if (room.getRoomType() == RoomType.DIRECT) {
            ChatMember other = room.getMembers().stream()
                    .filter(m -> !m.getEmployee().getId().equals(myId))
                    .findFirst()
                    .orElse(null);
            name = other != null ? other.getEmployee().getName() : "알 수 없음";
            thumbnailImage = other != null ? other.getEmployee().getProfileImage() : null;
            otherStatus = other != null ? other.getEmployee().getStatus() : null;
        } else {
            name = room.getName();
        }

        // 마지막 메시지
        String lastMessage = messageRepository
                .findTopByRoomIdOrderBySentAtDesc(room.getId())
                .map(Message::getContent)
                .orElse(null);

        // 읽지 않은 메시지 수
        ChatMember myMember = chatMemberRepository
                .findByRoomIdAndEmployeeId(room.getId(), myId)
                .orElse(null);

        long unreadCount = 0;
        if (myMember != null) {
            if (myMember.getLastReadMessageId() == null) {
                unreadCount = messageRepository
                        .countByRoomIdAndSenderIdNot(room.getId(), myId);
            } else {
                unreadCount = messageRepository
                        .countByRoomIdAndIdGreaterThanAndSenderIdNot(
                                room.getId(), myMember.getLastReadMessageId(), myId);
            }
        }

        return ChatRoomListResponse.builder()
                .id(room.getId())
                .roomType(room.getRoomType())
                .name(name)
                .thumbnailImage(thumbnailImage)
                .lastMessage(lastMessage)
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount((int) unreadCount)
                .otherStatus(otherStatus)
                .build();
    }

    // 메시지 목록 (커서 기반 스크롤)
    public List<MessageResponse> getMessages(Long roomId, Long myId, Long lastMessageId, int size) {

        if (!chatMemberRepository.existsByRoomIdAndEmployeeId(roomId, myId)) {
            throw new CustomException(ErrorCode.NOT_CHAT_MEMBER);
        }

        Pageable pageable = PageRequest.of(0, size);
        List<Message> messages = (lastMessageId == null)
                ? messageRepository.findByRoomIdOrderByIdDesc(roomId, pageable)
                : messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, lastMessageId, pageable);

        return messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    // 메시지 전송
    @Transactional
    public MessageResponse sendMessage(Long roomId, Long myId, MessageSendRequest request) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!chatMemberRepository.existsByRoomIdAndEmployeeId(roomId, myId)) {
            throw new CustomException(ErrorCode.NOT_CHAT_MEMBER);
        }

        Employee sender = employeeRepository.findById(myId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .msgType(request.getMsgType() != null ? request.getMsgType() : MessageType.TEXT)
                .fileId(request.getFileId())
                .build();

        messageRepository.save(message);
        room.updateLastMessageAt(LocalDateTime.now());

        // 나를 제외한 채팅방 멤버들에게 알림 전송
        String notificationContent = sender.getName() + ": " + request.getContent();
        room.getMembers().stream()
                .filter(m -> !m.getEmployee().getId().equals(myId))
                .forEach(m -> notificationService.send(
                        m.getEmployee().getId(),
                        NotificationType.MESSAGE,
                        notificationContent,
                        "CHAT_ROOM",
                        roomId
                ));

        // 채팅방 구독자에게 실시간 메시지 전송 (WebSocket)
        MessageResponse response = MessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        return response;
    }

    // 읽음 처리
    @Transactional
    public void readMessages(Long roomId, Long myId) {

        if (!chatRoomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        ChatMember myMember = chatMemberRepository
                .findByRoomIdAndEmployeeId(roomId, myId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHAT_MEMBER));

        // unread 계산이 id 기준이므로 읽음 처리도 id 기준 최신 메시지로 통일
        // 일 그만 시켜...ㅠㅠ
        messageRepository.findTopByRoomIdOrderByIdDesc(roomId)
                .ifPresent(latest -> myMember.updateLastRead(latest.getId()));
    }

    // 구성원 목록
    public List<ChatMemberResponse> getRoomMembers(Long roomId, Long myId) {

        if (!chatMemberRepository.existsByRoomIdAndEmployeeId(roomId, myId)) {
            throw new CustomException(ErrorCode.NOT_CHAT_MEMBER);
        }

        return chatMemberRepository.findByRoomId(roomId).stream()
                .map(ChatMemberResponse::from)
                .collect(Collectors.toList());
    }

    // 공유 파일 목록
    public List<MessageResponse> getRoomFiles(Long roomId, Long myId) {

        if (!chatMemberRepository.existsByRoomIdAndEmployeeId(roomId, myId)) {
            throw new CustomException(ErrorCode.NOT_CHAT_MEMBER);
        }

        return messageRepository
                .findByRoomIdAndMsgTypeIn(
                        roomId, List.of(MessageType.FILE, MessageType.IMAGE))
                .stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }
}
