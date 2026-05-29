package com.worksync.domain.approval.entity;

import com.worksync.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "step_type", nullable = false, columnDefinition = "step_type_enum")
    private StepType stepType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "approval_line_status")
    @Builder.Default
    private ApprovalLineStatus status = ApprovalLineStatus.WAITING;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 결재 처리 (승인 or 반려)
    public void process(ApprovalLineStatus status, String comment) {
        this.status = status;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
}
