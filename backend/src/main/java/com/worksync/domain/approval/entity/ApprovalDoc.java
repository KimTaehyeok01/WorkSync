package com.worksync.domain.approval.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_doc")
@EntityListeners(AuditingEntityListener.class)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_id", nullable = false)
    private Employee drafter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private ApprovalForm form;

    @Column(nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "approval_doc_status")
    @Builder.Default
    private ApprovalDocStatus status = ApprovalDocStatus.IN_PROGRESS;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "doc", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApprovalLine> approvalLines = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "doc", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApprovalDocItem> approvalDocItems = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 최종 승인
    public void approve() {
        this.status = ApprovalDocStatus.APPROVED;
        this.completedAt = LocalDateTime.now();
    }

    // 반려
    public void reject() {
        this.status = ApprovalDocStatus.REJECTED;
        this.completedAt = LocalDateTime.now();
    }

    // 제목 수정
    public void updateTitle(String title) {
        this.title = title;
    }

    // 문서 항목(items) 교체
    public void replaceItems(List<ApprovalDocItem> newItems) {
        this.approvalDocItems.clear();
        this.approvalDocItems.addAll(newItems);
    }
}
