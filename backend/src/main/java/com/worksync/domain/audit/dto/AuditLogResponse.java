package com.worksync.domain.audit.dto;

import com.worksync.domain.audit.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String targetType;
    private Long targetId;
    private String clientIp;
    private String userAgent;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorName(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
