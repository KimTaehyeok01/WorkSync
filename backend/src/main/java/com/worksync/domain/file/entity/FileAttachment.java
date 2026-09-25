package com.worksync.domain.file.entity;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "file_attachment")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Employee uploader;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ref_type", nullable = false, columnDefinition = "ref_type")
    private RefType refType = RefType.APPROVAL;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    public void updateRefId (Long refId) {
        this.refId = refId;
    }
}
