package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDetailResponse {

    private Long id;
    private String title;
    private ApprovalDoc.Status status;
    private Long drafterId;
    private String drafterName;
    private String drafterJobGrade;
    private Long formId;
    private String formName;
    private String formSchema;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private List<ItemDto> items;
    private List<LineDto> lines;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {
        private String itemKey;
        private String itemValue;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineDto {
        private Long id;
        private int stepOrder;
        private ApprovalLine.StepType stepType;
        private ApprovalLine.LineStatus status;
        private Long approverId;
        private String approverName;
        private String approverJobGrade;
        private String comment;
        private LocalDateTime processedAt;
    }
}
