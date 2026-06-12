package com.worksync.domain.file.dto;

import com.worksync.domain.file.entity.FileAttachment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class FileUploadResponse {
    private Long id;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String refType;
    private Long refId;
    private Integer version;
    private LocalDateTime createdAt;

    public static FileUploadResponse from(FileAttachment file) {
        return FileUploadResponse.builder()
                .originalName(file.getOriginalName())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .build();
    }
}
