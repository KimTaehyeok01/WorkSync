package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocItem;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.StepType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter @Builder
public class ApprovalDetailResponse {

    private Long id;
    private String title;
    private Long formId;
    private String formName;
    private Long drafterId;
    private String drafterName;
    private ApprovalDocStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<ApprovalLineDetail> approvalLines;
    private Map<String, String> items;

    public static ApprovalDetailResponse from(ApprovalDoc doc) {
        return ApprovalDetailResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .formId(doc.getForm().getId())
                .formName(doc.getForm().getFormName())
                .drafterId(doc.getDrafter().getId())
                .drafterName(doc.getDrafter().getName())
                .status(doc.getStatus())
                .submittedAt(doc.getSubmittedAt())
                .completedAt(doc.getCompletedAt())
                .createdAt(doc.getCreatedAt())
                .approvalLines(doc.getApprovalLines().stream()
                        .map(ApprovalLineDetail::from)
                        .collect(Collectors.toList()))
                .items(doc.getApprovalDocItems().stream()
                        .collect(Collectors.toMap(ApprovalDocItem::getItemKey, item ->
                                item.getItemValue() != null ? item.getItemValue() : "")))
                .build();
    }

    @Getter @Builder
    public static class ApprovalLineDetail {
        private Long id;
        private Long approverId;
        private String approverName;
        private Integer stepOrder;
        private StepType stepType;
        private ApprovalLineStatus status;
        private String comment;
        private LocalDateTime processedAt;

        public static ApprovalLineDetail from(ApprovalLine line) {
            return ApprovalLineDetail.builder()
                    .id(line.getId())
                    .approverId(line.getApprover().getId())
                    .approverName(line.getApprover().getName())
                    .stepOrder(line.getStepOrder())
                    .stepType(line.getStepType())
                    .status(line.getStatus())
                    .comment(line.getComment())
                    .processedAt(line.getProcessedAt())
                    .build();
        }
    }
}
