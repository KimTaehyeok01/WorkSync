package com.worksync.domain.chat.repository;

import com.worksync.domain.chat.entity.ChatRoom;
import com.worksync.domain.chat.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 내 채팅방 목록 (최근 메시지 순)
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members cm WHERE cm.employee.id = :employeeId ORDER BY COALESCE(cr.lastMessageAt, cr.createdAt) DESC")
    List<ChatRoom> findMyRooms(@Param("employeeId") Long employeeId);

    // 1:1 중복 채팅방 확인
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members cm1 JOIN cr.members cm2 WHERE cr.roomType = :roomType AND cm1.employee.id = :userId AND cm2.employee.id = :targetId")
    Optional<ChatRoom> findDirectRoom(@Param("roomType") RoomType roomType, @Param("userId") Long userId, @Param("targetId") Long targetId);
}
