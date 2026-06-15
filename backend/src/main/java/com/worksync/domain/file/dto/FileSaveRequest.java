package com.worksync.domain.file.dto;

import com.worksync.domain.file.entity.FileAttachment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FileSaveRequest {
    private Long id;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String refType;
    private Long refId;
    private Integer version;
    private LocalDateTime createdAt;

    public static FileSaveRequest from(FileAttachment file) {
        return FileSaveRequest.builder()
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
