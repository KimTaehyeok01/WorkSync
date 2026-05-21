package com.worksync.domain.approval.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_doc_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ApprovalDocItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDoc doc;

    @Column(nullable = false, length = 100)
    private String itemKey;

    @Column(columnDefinition = "TEXT")
    private String itemValue;
}
