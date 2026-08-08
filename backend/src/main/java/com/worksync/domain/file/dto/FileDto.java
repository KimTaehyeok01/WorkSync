// 파일 도메인 요청/응답 DTO 모음
package com.worksync.domain.file.dto;

import com.worksync.domain.file.entity.FileAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class FileDto {

    @Getter
    @Setter
    @Builder
    @Schema(name = "FileDto.SaveRequest")
    public static class SaveRequest {

        @Schema(description = "파일 ID", example = "1")
        private Long id;

        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String originalName;

        @Schema(description = "스토리지 저장 경로")
        private String filePath;

        @Schema(description = "파일 크기 (byte)", example = "10240")
        private Long fileSize;

        @Schema(description = "MIME 타입", example = "application/pdf")
        private String mimeType;

        @Schema(description = "첨부 위치 유형 (APPROVAL, TASK, CHAT, POST, DEPARTMENT, EMPLOYEE, BOARD)", example = "APPROVAL")
        private String refType;

        @Schema(description = "첨부 위치 참조 ID", example = "1")
        private Long refId;

        @Schema(description = "파일 버전", example = "1")
        private Integer version;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static SaveRequest from(FileAttachment file) {
            return SaveRequest.builder()
                    .id(file.getId())
                    .originalName(file.getOriginalName())
                    .filePath(file.getFilePath())
                    .fileSize(file.getFileSize())
                    .mimeType(file.getMimeType())
                    .refType(file.getRefType().toString())
                    .refId(file.getRefId())
                    .build();
        }
    }

    @Getter @Builder
    @Schema(name = "FileDto.UploadResponse")
    public static class UploadResponse {

        @Schema(description = "파일 ID", example = "1")
        private Long id;

        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String originalName;

        @Schema(description = "스토리지 저장 경로")
        private String filePath;

        @Schema(description = "파일 크기 (byte)", example = "10240")
        private Long fileSize;

        @Schema(description = "MIME 타입", example = "application/pdf")
        private String mimeType;

        @Schema(description = "첨부 위치 유형 (APPROVAL, TASK, CHAT, POST, DEPARTMENT, EMPLOYEE, BOARD)", example = "APPROVAL")
        private String refType;

        @Schema(description = "첨부 위치 참조 ID", example = "1")
        private Long refId;

        @Schema(description = "파일 버전", example = "1")
        private Integer version;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static UploadResponse from(FileAttachment file) {
            return UploadResponse.builder()
                    .id(file.getId())
                    .originalName(file.getOriginalName())
                    .filePath(file.getFilePath())
                    .fileSize(file.getFileSize())
                    .mimeType(file.getMimeType())
                    .createdAt(file.getCreatedAt())
                    .build();
        }
    }
}
