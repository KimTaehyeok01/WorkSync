package com.worksync.domain.file.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class FileAttachment {

    public enum RefType { TASK, POST, APPROVAL, MESSAGE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Employee uploader;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 512)
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RefType refType;

    private Long refId;

    @Builder.Default
    @Column(nullable = false)
    private int version = 1;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
