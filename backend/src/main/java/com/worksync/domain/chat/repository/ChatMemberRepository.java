package com.worksync.domain.chat.repository;

import com.worksync.domain.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    Optional<ChatMember> findByRoomIdAndEmployeeId(Long roomId, Long employeeId);

    boolean existsByRoomIdAndEmployeeId(Long roomId, Long employeeId);

    List<ChatMember> findByRoomId(Long roomId);
}
