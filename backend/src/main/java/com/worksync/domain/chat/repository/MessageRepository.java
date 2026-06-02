package com.worksync.domain.chat.repository;

import com.worksync.domain.chat.entity.Message;
import com.worksync.domain.chat.entity.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 첫 로드 — 최신 N개
    List<Message> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    // 스크롤 업 — lastMessageId보다 오래된 N개
    List<Message> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long lastMessageId, Pageable pageable);

    // 읽지 않은 메시지 수
    long countByRoomIdAndIdGreaterThanAndSenderIdNot(Long roomId, Long lastReadMessageId, Long senderId);

    // 마지막 메시지 (목록 표시용 - 시각 기준)
    Optional<Message> findTopByRoomIdOrderBySentAtDesc(Long roomId);

    // 마지막 메시지 (읽음 처리용 - id 기준, unread 계산과 정렬 기준 통일)
    Optional<Message> findTopByRoomIdOrderByIdDesc(Long roomId);

    // 읽지 않은 메시지 수 (lastReadMessageId가 null인 경우 — 전체)
    long countByRoomIdAndSenderIdNot(Long roomId, Long senderId);

    // 공유 파일 목록
    List<Message> findByRoomIdAndMsgTypeIn(Long roomId, List<MessageType> types);
}
