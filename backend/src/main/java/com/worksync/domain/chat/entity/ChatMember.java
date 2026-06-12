package com.worksync.domain.chat.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_member",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "employee_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "in_room", nullable = false)
    @Builder.Default
    private boolean inRoom = false;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void enterRoom() {
        this.inRoom = true;
    }

    public void leaveRoom() {
        this.inRoom = false;
    }
}
