// 감사 로그 도메인 응답 DTO 모음
package com.worksync.domain.audit.dto;

import com.worksync.domain.audit.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AuditLogDto {

    @Getter @Builder
    @Schema(name = "AuditLogDto.Response")
    public static class Response {

        @Schema(description = "감사 로그 ID", example = "1")
        private Long id;

        @Schema(description = "행위자 사원 ID", example = "1")
        private Long actorId;

        @Schema(description = "행위자 이름", example = "홍길동")
        private String actorName;

        @Schema(description = "수행 동작", example = "로그인 실패")
        private String action;

        @Schema(description = "대상 리소스 유형", example = "APPROVAL")
        private String targetType;

        @Schema(description = "대상 리소스 ID", example = "10")
        private Long targetId;

        @Schema(description = "요청 클라이언트 IP", example = "127.0.0.1")
        private String clientIp;

        @Schema(description = "요청 User-Agent")
        private String userAgent;

        @Schema(description = "발생 일시")
        private LocalDateTime createdAt;

        public static Response from(AuditLog log) {
            return Response.builder()
                    .id(log.getId())
                    .actorId(log.getActorId())
                    .actorName(log.getActorName())
                    .action(log.getAction())
                    .targetType(log.getTargetType())
                    .targetId(log.getTargetId())
                    .clientIp(log.getClientIp())
                    .userAgent(log.getUserAgent())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }

    @Getter @Builder
    @Schema(name = "AuditLogDto.Summary")
    public static class Summary {

        @Schema(description = "전체 로그 수", example = "120")
        private long totalCount;      // 전체 로그 수

        @Schema(description = "오늘 활동 수", example = "15")
        private long todayCount;      // 오늘 활동 수

        @Schema(description = "로그인 실패 수", example = "2")
        private long loginFailCount;  // 로그인 실패 수

        @Schema(description = "전자결재 처리 수", example = "8")
        private long approvalCount;   // 전자결재 처리 수
    }
}
