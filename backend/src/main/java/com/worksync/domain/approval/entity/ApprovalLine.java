package com.worksync.domain.approval.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_line",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doc_id", "step_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ApprovalLine {

    public enum StepType   { DRAFT, REVIEW, APPROVE, REFERENCE }
    public enum LineStatus { WAITING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDoc doc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "step_type_enum")
    private StepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "approval_line_status")
    private LineStatus status;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime processedAt;

    public void approve(String comment) {
        this.status = LineStatus.APPROVED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String comment) {
        this.status = LineStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
}
