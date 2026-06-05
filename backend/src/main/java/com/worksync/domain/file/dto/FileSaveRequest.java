package com.worksync.domain.file.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FileSaveRequest {
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String refType;
    private Long refId;
    private Integer version;
    private LocalDateTime createdAt;
}
