package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class ApprovalListResponse {

    private Long id;
    private String title;
    private String formName;
    private String drafterName;
    private ApprovalDocStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;

    public static ApprovalListResponse from(ApprovalDoc doc) {
        return ApprovalListResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .formName(doc.getForm().getFormName())
                .drafterName(doc.getDrafter().getName())
                .status(doc.getStatus())
                .submittedAt(doc.getSubmittedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
