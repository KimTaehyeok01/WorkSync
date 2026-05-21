package com.worksync.domain.file.dto;

import com.worksync.domain.file.entity.FileAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {

    private Long id;
    private String originalName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private FileAttachment.RefType refType;
    private Long refId;
    private int version;
    private LocalDateTime createdAt;
}
