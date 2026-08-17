// ChatService 단위 테스트
package com.worksync.domain.chat.service;

import com.worksync.domain.chat.dto.ChatRoomDto;
import com.worksync.domain.chat.dto.MessageDto;
import com.worksync.domain.chat.entity.ChatMember;
import com.worksync.domain.chat.entity.ChatRoom;
import com.worksync.domain.chat.entity.Message;
import com.worksync.domain.chat.entity.MessageType;
import com.worksync.domain.chat.entity.RoomType;
import com.worksync.domain.chat.repository.ChatMemberRepository;
import com.worksync.domain.chat.repository.ChatRoomRepository;
import com.worksync.domain.chat.repository.MessageRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.ai.GroqService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** ChatService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private GroqService groqService;

    @InjectMocks
    private ChatService chatService;

    private Employee buildEmployee(Long id, String name) {
        return buildEmployee(id, name, EmployeeStatus.ACTIVE);
    }

    private Employee buildEmployee(Long id, String name, EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .status(status)
                .build();
    }

    private ChatRoom buildRoom(Long id, RoomType roomType, String name, Employee createdBy) {
        return ChatRoom.builder()
                .id(id)
                .roomType(roomType)
                .name(name)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ChatMember buildMember(Long id, ChatRoom room, Employee employee, Long lastReadMessageId, boolean inRoom) {
        return ChatMember.builder()
                .id(id)
                .room(room)
                .employee(employee)
                .lastReadMessageId(lastReadMessageId)
                .inRoom(inRoom)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    private Message buildMessage(Long id, ChatRoom room, Employee sender, String content, MessageType msgType) {
        return Message.builder()
                .id(id)
                .room(room)
                .sender(sender)
                .content(content)
                .msgType(msgType)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private ChatRoomDto.CreateRequest buildCreateRequest(RoomType roomType, String name, List<Long> memberIds) {
        ChatRoomDto.CreateRequest request = new ChatRoomDto.CreateRequest();
        request.setRoomType(roomType);
        request.setName(name);
        request.setMemberIds(memberIds);
        return request;
    }

    private MessageDto.SendRequest buildSendRequest(String content, MessageType msgType, Long fileId) {
        MessageDto.SendRequest request = new MessageDto.SendRequest();
        request.setContent(content);
        request.setMsgType(msgType);
        request.setFileId(fileId);
        return request;
    }

    // ==================== createRoom ====================

    @DisplayName("1:1 채팅 신청 시 상대방을 2명 이상 지정하면 예외가 발생한다")
    @Test
    void createRoom_directWithMultipleMembers_throwsIllegalArgumentException() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.DIRECT, null, List.of(2L, 3L));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

        // when & then
        assertThatThrownBy(() -> chatService.createRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("이미 존재하는 1:1 채팅방이 있으면 기존 방을 반환하고 새로 저장하지 않는다")
    @Test
    void createRoom_directExistingRoom_returnsExistingRoom() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee target = buildEmployee(2L, "박영희");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.DIRECT, null, List.of(2L));

        ChatRoom existingRoom = buildRoom(10L, RoomType.DIRECT, null, creator);
        existingRoom.getMembers().add(buildMember(101L, existingRoom, creator, null, true));
        existingRoom.getMembers().add(buildMember(102L, existingRoom, target, null, false));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(chatRoomRepository.findDirectRoom(RoomType.DIRECT, 1L, 2L)).willReturn(Optional.of(existingRoom));

        // when
        ChatRoomDto.Response result = chatService.createRoom(1L, request);

        // then
        assertThat(result.getId()).isEqualTo(10L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @DisplayName("기존 1:1 채팅방이 없으면 새 채팅방과 시스템 메시지를 생성한다")
    @Test
    void createRoom_directNoExistingRoom_createsNewRoom() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee target = buildEmployee(2L, "박영희");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.DIRECT, null, List.of(2L));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(target));
        given(chatRoomRepository.findDirectRoom(RoomType.DIRECT, 1L, 2L)).willReturn(Optional.empty());

        // when
        ChatRoomDto.Response result = chatService.createRoom(1L, request);

        // then
        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(roomCaptor.capture());
        ChatRoom savedRoom = roomCaptor.getValue();
        assertThat(savedRoom.getMembers()).hasSize(2);
        assertThat(savedRoom.getLastMessageAt()).isNotNull();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getMsgType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("대화가 시작되었습니다.");

        assertThat(result).isNotNull();
    }

    @DisplayName("그룹 채팅방 생성 시 이름이 없으면 예외가 발생한다")
    @Test
    void createRoom_groupBlankName_throwsIllegalArgumentException() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.GROUP, "  ", List.of(2L, 3L));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

        // when & then
        assertThatThrownBy(() -> chatService.createRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("그룹 채팅방을 정상적으로 생성하면 참여자 전원과 시스템 메시지가 저장된다")
    @Test
    void createRoom_groupSuccess_createsRoomWithSystemMessage() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee member2 = buildEmployee(2L, "박영희");
        Employee member3 = buildEmployee(3L, "이몽룡");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.GROUP, "개발팀", List.of(2L, 3L));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(member2));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(member3));

        // when
        ChatRoomDto.Response result = chatService.createRoom(1L, request);

        // then
        assertThat(result.getName()).isEqualTo("개발팀");

        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getMembers()).hasSize(3);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("[개발팀] 채팅방이 생성되었습니다.");
    }

    @DisplayName("방장(생성자)이 존재하지 않으면 예외가 발생한다")
    @Test
    void createRoom_creatorNotFound_throwsCustomException() {
        // given
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.DIRECT, null, List.of(2L));
        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.createRoom(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("초대할 멤버가 존재하지 않으면 예외가 발생한다")
    @Test
    void createRoom_memberNotFound_throwsCustomException() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        ChatRoomDto.CreateRequest request = buildCreateRequest(RoomType.GROUP, "개발팀", List.of(2L));

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(employeeRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.createRoom(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    // ==================== getMyRooms ====================

    @DisplayName("내 채팅방 목록 조회 시 1:1 방은 상대방 정보, 그룹 방은 안읽음 수를 각각 계산한다")
    @Test
    void getMyRooms_success_returnsDirectAndGroupRoomInfo() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        Employee other = buildEmployee(2L, "박영희", EmployeeStatus.AWAY);

        ChatRoom roomA = buildRoom(10L, RoomType.DIRECT, null, me);
        ChatMember myMemberA = buildMember(101L, roomA, me, null, true);
        roomA.getMembers().add(myMemberA);
        roomA.getMembers().add(buildMember(102L, roomA, other, null, false));

        ChatRoom roomB = buildRoom(11L, RoomType.GROUP, "개발팀", me);
        ChatMember myMemberB = buildMember(103L, roomB, me, 5L, true);
        roomB.getMembers().add(myMemberB);

        given(chatRoomRepository.findMyRooms(1L)).willReturn(List.of(roomA, roomB));
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMemberA));
        given(chatMemberRepository.findByRoomIdAndEmployeeId(11L, 1L)).willReturn(Optional.of(myMemberB));
        given(messageRepository.findTopByRoomIdOrderBySentAtDesc(10L))
                .willReturn(Optional.of(buildMessage(1L, roomA, other, "안녕", MessageType.TEXT)));
        given(messageRepository.findTopByRoomIdOrderBySentAtDesc(11L)).willReturn(Optional.empty());
        given(messageRepository.countByRoomIdAndSenderIdNot(10L, 1L)).willReturn(3L);
        given(messageRepository.countByRoomIdAndIdGreaterThanAndSenderIdNot(11L, 5L, 1L)).willReturn(2L);

        // when
        List<ChatRoomDto.ListResponse> result = chatService.getMyRooms(1L, null);

        // then
        assertThat(result).hasSize(2);
        ChatRoomDto.ListResponse direct = result.get(0);
        assertThat(direct.getName()).isEqualTo("박영희");
        assertThat(direct.getLastMessage()).isEqualTo("안녕");
        assertThat(direct.getUnreadCount()).isEqualTo(3);
        assertThat(direct.getOtherStatus()).isEqualTo(EmployeeStatus.AWAY);

        ChatRoomDto.ListResponse group = result.get(1);
        assertThat(group.getName()).isEqualTo("개발팀");
        assertThat(group.getLastMessage()).isNull();
        assertThat(group.getUnreadCount()).isEqualTo(2);
    }

    @DisplayName("1:1 채팅방에서 상대방이 조회되지 않으면 이름을 알 수 없음으로 표시한다")
    @Test
    void getMyRooms_directOtherMemberMissing_returnsUnknownName() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.DIRECT, null, me);
        ChatMember myMember = buildMember(101L, room, me, null, true);
        room.getMembers().add(myMember);

        given(chatRoomRepository.findMyRooms(1L)).willReturn(List.of(room));
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMember));
        given(messageRepository.findTopByRoomIdOrderBySentAtDesc(10L)).willReturn(Optional.empty());
        given(messageRepository.countByRoomIdAndSenderIdNot(10L, 1L)).willReturn(0L);

        // when
        List<ChatRoomDto.ListResponse> result = chatService.getMyRooms(1L, null);

        // then
        assertThat(result.get(0).getName()).isEqualTo("알 수 없음");
        assertThat(result.get(0).getThumbnailImage()).isNull();
        assertThat(result.get(0).getOtherStatus()).isNull();
    }

    @DisplayName("검색어와 일치하는 상대방이 있는 채팅방만 필터링된다")
    @Test
    void getMyRooms_withKeyword_filtersMatchingRoomOnly() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        Employee other = buildEmployee(2L, "박영희");
        Employee groupMate = buildEmployee(3L, "이몽룡");

        ChatRoom roomA = buildRoom(10L, RoomType.DIRECT, null, me);
        ChatMember myMemberA = buildMember(101L, roomA, me, null, true);
        roomA.getMembers().add(myMemberA);
        roomA.getMembers().add(buildMember(102L, roomA, other, null, false));

        ChatRoom roomB = buildRoom(11L, RoomType.GROUP, "개발팀", me);
        roomB.getMembers().add(buildMember(103L, roomB, me, null, true));
        roomB.getMembers().add(buildMember(104L, roomB, groupMate, null, false));

        given(chatRoomRepository.findMyRooms(1L)).willReturn(List.of(roomA, roomB));
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMemberA));
        given(messageRepository.findTopByRoomIdOrderBySentAtDesc(10L)).willReturn(Optional.empty());
        given(messageRepository.countByRoomIdAndSenderIdNot(10L, 1L)).willReturn(0L);

        // when
        List<ChatRoomDto.ListResponse> result = chatService.getMyRooms(1L, "박");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    @DisplayName("검색어와 일치하는 채팅방이 없으면 빈 목록을 반환한다")
    @Test
    void getMyRooms_withKeyword_noMatch_returnsEmptyList() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        Employee other = buildEmployee(2L, "박영희");

        ChatRoom roomA = buildRoom(10L, RoomType.DIRECT, null, me);
        roomA.getMembers().add(buildMember(101L, roomA, me, null, true));
        roomA.getMembers().add(buildMember(102L, roomA, other, null, false));

        given(chatRoomRepository.findMyRooms(1L)).willReturn(List.of(roomA));

        // when
        List<ChatRoomDto.ListResponse> result = chatService.getMyRooms(1L, "존재하지않는이름");

        // then
        assertThat(result).isEmpty();
    }

    // ==================== getMessages ====================

    @DisplayName("채팅방 멤버가 아니면 메시지 목록 조회 시 예외가 발생한다")
    @Test
    void getMessages_notMember_throwsCustomException() {
        // given
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.getMessages(10L, 1L, null, 20))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("lastMessageId가 없으면 최신 메시지 목록을 조회한다")
    @Test
    void getMessages_firstLoad_returnsLatestMessages() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message message = buildMessage(1L, room, me, "안녕", MessageType.TEXT);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(10L), any(Pageable.class)))
                .willReturn(List.of(message));

        // when
        List<MessageDto.Response> result = chatService.getMessages(10L, 1L, null, 20);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("안녕");
    }

    @DisplayName("lastMessageId가 있으면 해당 메시지보다 오래된 메시지 목록을 조회한다")
    @Test
    void getMessages_scrollUp_returnsOlderMessages() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message message = buildMessage(1L, room, me, "이전 메시지", MessageType.TEXT);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(eq(10L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(message));

        // when
        List<MessageDto.Response> result = chatService.getMessages(10L, 1L, 5L, 20);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("이전 메시지");
    }

    // ==================== sendMessage ====================

    @DisplayName("존재하지 않는 채팅방에 메시지를 보내면 예외가 발생한다")
    @Test
    void sendMessage_roomNotFound_throwsCustomException() {
        // given
        given(chatRoomRepository.findById(10L)).willReturn(Optional.empty());
        MessageDto.SendRequest request = buildSendRequest("안녕", null, null);

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @DisplayName("채팅방 멤버가 아니면 메시지 전송 시 예외가 발생한다")
    @Test
    void sendMessage_notMember_throwsCustomException() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        MessageDto.SendRequest request = buildSendRequest("안녕", null, null);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("발신자 정보가 존재하지 않으면 메시지 전송 시 예외가 발생한다")
    @Test
    void sendMessage_senderNotFound_throwsCustomException() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        MessageDto.SendRequest request = buildSendRequest("안녕", null, null);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("메시지 유형을 지정하지 않으면 TEXT로 저장되고 채팅방 밖 멤버에게만 알림이 전송된다")
    @Test
    void sendMessage_msgTypeNull_defaultsToTextAndNotifiesOutOfRoomMembers() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        Employee outOfRoomMember = buildEmployee(2L, "박영희");
        Employee inRoomMember = buildEmployee(4L, "최민수");

        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        room.getMembers().add(buildMember(101L, room, me, null, true));
        room.getMembers().add(buildMember(102L, room, outOfRoomMember, null, false));
        room.getMembers().add(buildMember(104L, room, inRoomMember, 50L, true));

        MessageDto.SendRequest request = buildSendRequest("안녕하세요", null, null);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(messageRepository.countByRoomIdAndSenderIdNot(10L, 2L)).willReturn(4L);

        // when
        MessageDto.Response result = chatService.sendMessage(10L, 1L, request);

        // then
        assertThat(result.getMsgType()).isEqualTo(MessageType.TEXT);
        assertThat(result.getContent()).isEqualTo("안녕하세요");
        assertThat(room.getLastMessageAt()).isNotNull();

        verify(notificationService).send(eq(2L), eq(NotificationType.MESSAGE), eq("김철수: 안녕하세요"), eq("CHAT_ROOM"), eq(10L));
        verify(notificationService, never()).send(eq(4L), any(), any(), any(), any());
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/chat/unread"), eq(Map.of("roomId", 10L, "unreadCount", 4L)));
        verify(messagingTemplate).convertAndSend(eq("/topic/room/10"), any(MessageDto.Response.class));
    }

    @DisplayName("메시지 유형을 지정하면 지정한 유형으로 저장된다")
    @Test
    void sendMessage_msgTypeGiven_usesGivenType() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(20L, RoomType.DIRECT, null, me);
        room.getMembers().add(buildMember(201L, room, me, null, true));

        MessageDto.SendRequest request = buildSendRequest("사진", MessageType.IMAGE, 7L);

        given(chatRoomRepository.findById(20L)).willReturn(Optional.of(room));
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(20L, 1L)).willReturn(true);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));

        // when
        MessageDto.Response result = chatService.sendMessage(20L, 1L, request);

        // then
        assertThat(result.getMsgType()).isEqualTo(MessageType.IMAGE);
        assertThat(result.getFileId()).isEqualTo(7L);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/20"), any(MessageDto.Response.class));
        verifyNoInteractions(notificationService);
    }

    // ==================== readMessages ====================

    @DisplayName("존재하지 않는 채팅방을 읽음 처리하면 예외가 발생한다")
    @Test
    void readMessages_roomNotFound_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.readMessages(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @DisplayName("채팅방 멤버가 아니면 읽음 처리 시 예외가 발생한다")
    @Test
    void readMessages_notMember_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.readMessages(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("최신 메시지가 있으면 마지막으로 읽은 메시지 ID가 갱신된다")
    @Test
    void readMessages_success_updatesLastReadMessageId() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        ChatMember myMember = buildMember(101L, room, me, null, true);
        Message latest = buildMessage(99L, room, me, "마지막 메시지", MessageType.TEXT);

        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMember));
        given(messageRepository.findTopByRoomIdOrderByIdDesc(10L)).willReturn(Optional.of(latest));

        // when
        chatService.readMessages(10L, 1L);

        // then
        assertThat(myMember.getLastReadMessageId()).isEqualTo(99L);
    }

    @DisplayName("채팅방에 메시지가 없으면 마지막으로 읽은 메시지 ID가 변경되지 않는다")
    @Test
    void readMessages_noMessages_lastReadMessageIdUnchanged() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        ChatMember myMember = buildMember(101L, room, me, 5L, true);

        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMember));
        given(messageRepository.findTopByRoomIdOrderByIdDesc(10L)).willReturn(Optional.empty());

        // when
        chatService.readMessages(10L, 1L);

        // then
        assertThat(myMember.getLastReadMessageId()).isEqualTo(5L);
    }

    // ==================== enterRoom ====================

    @DisplayName("존재하지 않는 채팅방에 입장하면 예외가 발생한다")
    @Test
    void enterRoom_roomNotFound_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.enterRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @DisplayName("채팅방 멤버가 아니면 입장 시 예외가 발생한다")
    @Test
    void enterRoom_notMember_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.enterRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("정상적으로 입장하면 입장 상태로 변경된다")
    @Test
    void enterRoom_success_setsInRoomTrue() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        ChatMember myMember = buildMember(101L, room, me, null, false);

        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMember));

        // when
        chatService.enterRoom(10L, 1L);

        // then
        assertThat(myMember.isInRoom()).isTrue();
    }

    // ==================== leaveRoom ====================

    @DisplayName("존재하지 않는 채팅방에서 퇴장하면 예외가 발생한다")
    @Test
    void leaveRoom_roomNotFound_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.leaveRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @DisplayName("채팅방 멤버가 아니면 퇴장 시 예외가 발생한다")
    @Test
    void leaveRoom_notMember_throwsCustomException() {
        // given
        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.leaveRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("정상적으로 퇴장하면 퇴장 상태로 변경된다")
    @Test
    void leaveRoom_success_setsInRoomFalse() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        ChatMember myMember = buildMember(101L, room, me, null, true);

        given(chatRoomRepository.existsById(10L)).willReturn(true);
        given(chatMemberRepository.findByRoomIdAndEmployeeId(10L, 1L)).willReturn(Optional.of(myMember));

        // when
        chatService.leaveRoom(10L, 1L);

        // then
        assertThat(myMember.isInRoom()).isFalse();
    }

    // ==================== getRoomMembers ====================

    @DisplayName("채팅방 멤버가 아니면 구성원 목록 조회 시 예외가 발생한다")
    @Test
    void getRoomMembers_notMember_throwsCustomException() {
        // given
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.getRoomMembers(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("정상적으로 채팅방 구성원 목록을 조회한다")
    @Test
    void getRoomMembers_success_returnsMemberList() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        Employee other = buildEmployee(2L, "박영희");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        ChatMember myMember = buildMember(101L, room, me, null, true);
        ChatMember otherMember = buildMember(102L, room, other, null, false);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(chatMemberRepository.findByRoomId(10L)).willReturn(List.of(myMember, otherMember));

        // when
        List<ChatRoomDto.MemberResponse> result = chatService.getRoomMembers(10L, 1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1).getName()).isEqualTo("박영희");
    }

    // ==================== summarizeRoom ====================

    @DisplayName("채팅방 멤버가 아니면 대화 요약 시 예외가 발생한다")
    @Test
    void summarizeRoom_notMember_throwsCustomException() {
        // given
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.summarizeRoom(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("TEXT 메시지가 없으면 AI 요약을 호출하지 않고 기본 안내 문구를 반환한다")
    @Test
    void summarizeRoom_noTextMessages_returnsDefaultMessageWithoutCallingGroq() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message systemMessage = buildMessage(1L, room, null, "채팅방이 생성되었습니다.", MessageType.SYSTEM);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(10L), any(Pageable.class)))
                .willReturn(List.of(systemMessage));

        // when
        String result = chatService.summarizeRoom(10L, 1L);

        // then
        assertThat(result).isEqualTo("요약할 대화 내용이 없습니다.");
        verifyNoInteractions(groqService);
    }

    @DisplayName("TEXT 메시지가 있으면 AI 요약을 호출하여 결과를 반환한다")
    @Test
    void summarizeRoom_withTextMessages_returnsGroqSummary() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message message = buildMessage(1L, room, me, "안녕하세요", MessageType.TEXT);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(10L), any(Pageable.class)))
                .willReturn(List.of(message));
        given(groqService.generate(any())).willReturn("요약결과");

        // when
        String result = chatService.summarizeRoom(10L, 1L);

        // then
        assertThat(result).isEqualTo("요약결과");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqService).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("김철수: 안녕하세요");
    }

    @DisplayName("AI 요약 호출이 실패하면 예외가 그대로 전파된다")
    @Test
    void summarizeRoom_groqServiceThrows_propagatesException() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message message = buildMessage(1L, room, me, "안녕하세요", MessageType.TEXT);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdOrderByIdDesc(eq(10L), any(Pageable.class)))
                .willReturn(List.of(message));
        given(groqService.generate(any())).willThrow(new RuntimeException("Groq API 호출 실패"));

        // when & then
        assertThatThrownBy(() -> chatService.summarizeRoom(10L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Groq API 호출 실패");
    }

    // ==================== getRoomFiles ====================

    @DisplayName("채팅방 멤버가 아니면 공유 파일 목록 조회 시 예외가 발생한다")
    @Test
    void getRoomFiles_notMember_throwsCustomException() {
        // given
        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.getRoomFiles(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_CHAT_MEMBER);
    }

    @DisplayName("정상적으로 FILE/IMAGE 타입 메시지 목록을 조회한다")
    @Test
    void getRoomFiles_success_returnsFileAndImageMessages() {
        // given
        Employee me = buildEmployee(1L, "김철수");
        ChatRoom room = buildRoom(10L, RoomType.GROUP, "개발팀", me);
        Message fileMessage = buildMessage(1L, room, me, "문서.pdf", MessageType.FILE);

        given(chatMemberRepository.existsByRoomIdAndEmployeeId(10L, 1L)).willReturn(true);
        given(messageRepository.findByRoomIdAndMsgTypeIn(10L, List.of(MessageType.FILE, MessageType.IMAGE)))
                .willReturn(List.of(fileMessage));

        // when
        List<MessageDto.Response> result = chatService.getRoomFiles(10L, 1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("문서.pdf");
    }
}
