// 결재 문서 도메인 요청/응답 DTO 모음
package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocItem;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.StepType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApprovalDto {

    @Getter @Setter
    @Schema(name = "ApprovalDto.CreateRequest")
    public static class CreateRequest {

        @NotNull
        @Schema(description = "결재 양식 ID", example = "1")
        private Long formId;

        @NotBlank
        @Schema(description = "결재 문서 제목", example = "출장 신청서")
        private String title;

        @NotEmpty
        @Valid
        @Schema(description = "결재선 목록")
        private List<ApprovalLineRequest> approvalLines;

        @Schema(description = "양식 항목 (key-value)")
        private Map<String, String> items;

        @Getter @Setter
        @Schema(name = "ApprovalDto.CreateRequest.ApprovalLineRequest")
        public static class ApprovalLineRequest {

            @NotNull
            @Schema(description = "결재자 ID", example = "1")
            private Long approverId;

            @NotNull
            @Schema(description = "결재 순서", example = "1")
            private Integer stepOrder;

            @NotNull
            @Schema(description = "결재 단계 유형 (DRAFT, REVIEW, APPROVE, REFERENCE)", example = "APPROVE")
            private StepType stepType;
        }
    }

    @Getter @Builder
    @Schema(name = "ApprovalDto.DetailResponse")
    public static class DetailResponse {

        @Schema(description = "결재 문서 ID", example = "1")
        private Long id;

        @Schema(description = "결재 문서 제목", example = "출장 신청서")
        private String title;

        @Schema(description = "결재 양식 ID", example = "1")
        private Long formId;

        @Schema(description = "결재 양식명", example = "출장 신청서")
        private String formName;

        @Schema(description = "기안자 ID", example = "1")
        private Long drafterId;

        @Schema(description = "기안자 이름", example = "홍길동")
        private String drafterName;

        @Schema(description = "결재 상태 (IN_PROGRESS, APPROVED, REJECTED)", example = "IN_PROGRESS")
        private ApprovalDocStatus status;

        @Schema(description = "상신 일시")
        private LocalDateTime submittedAt;

        @Schema(description = "결재 완료 일시")
        private LocalDateTime completedAt;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        @Schema(description = "결재선 상세 목록")
        private List<ApprovalLineDetail> approvalLines;

        @Schema(description = "양식 항목 (key-value)")
        private Map<String, String> items;

        public static DetailResponse from(ApprovalDoc doc) {
            return DetailResponse.builder()
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
        @Schema(name = "ApprovalDto.DetailResponse.ApprovalLineDetail")
        public static class ApprovalLineDetail {
            @Schema(description = "결재선 ID", example = "1")
            private Long id;

            @Schema(description = "결재자 ID", example = "1")
            private Long approverId;

            @Schema(description = "결재자 이름", example = "김부장")
            private String approverName;

            @Schema(description = "결재 순서", example = "1")
            private Integer stepOrder;

            @Schema(description = "결재 단계 유형 (DRAFT, REVIEW, APPROVE, REFERENCE)", example = "APPROVE")
            private StepType stepType;

            @Schema(description = "결재선 상태 (WAITING, APPROVED, REJECTED)", example = "WAITING")
            private ApprovalLineStatus status;

            @Schema(description = "결재 의견", example = "승인합니다.")
            private String comment;

            @Schema(description = "결재 처리 일시")
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

    @Getter @Builder
    @Schema(name = "ApprovalDto.ListResponse")
    public static class ListResponse {

        @Schema(description = "결재 문서 ID", example = "1")
        private Long id;

        @Schema(description = "결재 문서 제목", example = "출장 신청서")
        private String title;

        @Schema(description = "결재 양식명", example = "출장 신청서")
        private String formName;

        @Schema(description = "기안자 이름", example = "홍길동")
        private String drafterName;

        @Schema(description = "결재 상태 (IN_PROGRESS, APPROVED, REJECTED)", example = "IN_PROGRESS")
        private ApprovalDocStatus status;

        @Schema(description = "상신 일시")
        private LocalDateTime submittedAt;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static ListResponse from(ApprovalDoc doc) {
            return ListResponse.builder()
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

    @Getter @Setter
    @Schema(name = "ApprovalDto.ProcessRequest")
    public static class ProcessRequest {

        @NotNull
        @Schema(description = "결재 처리 상태 (APPROVED, REJECTED)", example = "APPROVED")
        private ApprovalLineStatus status;

        @Schema(description = "결재 의견", example = "승인합니다.")
        private String comment;
    }

    @Getter @Setter
    @Schema(name = "ApprovalDto.UpdateRequest")
    public static class UpdateRequest {

        @NotBlank
        @Schema(description = "결재 문서 제목", example = "출장 신청서 (수정)")
        private String title;

        @Schema(description = "양식 항목 (key-value)")
        private Map<String, String> items;
    }
}
