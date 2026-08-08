// 결재 양식 도메인 요청/응답 DTO 모음
package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ApprovalFormDto {

    @Getter @Builder
    @Schema(name = "ApprovalFormDto.Response")
    public static class Response {

        @Schema(description = "결재 양식 ID", example = "1")
        private Long id;

        @Schema(description = "결재 양식명", example = "출장 신청서")
        private String formName;

        @Schema(description = "결재 양식 유형", example = "BUSINESS_TRIP")
        private String formType;

        @Schema(description = "결재 양식 스키마 (JSON)")
        private String formSchema;

        @Schema(description = "생성 일시")
        private LocalDateTime createdAt;

        public static Response from(ApprovalForm form) {
            return Response.builder()
                    .id(form.getId())
                    .formName(form.getFormName())
                    .formType(form.getFormType())
                    .formSchema(form.getFormSchema())
                    .createdAt(form.getCreatedAt())
                    .build();
        }
    }
}
