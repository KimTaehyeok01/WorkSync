package com.worksync.domain.approval.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_line")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDoc doc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, columnDefinition = "step_type_enum")
    private StepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "approval_line_status")
    @Builder.Default
    private ApprovalLineStatus status = ApprovalLineStatus.WAITING;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
