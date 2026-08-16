// 결재 양식 도메인 요청/응답 DTO 모음
package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalForm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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

    // === 커스텀 양식 생성 요청 ===
    @Getter @Setter
    @Schema(name = "ApprovalFormDto.CreateRequest")
    public static class CreateRequest {

        @NotBlank
        @Size(max = 100)
        @Schema(description = "결재 양식명", example = "동호회 지원 신청서")
        private String formName;

        @NotEmpty
        @Valid
        @Schema(description = "폼 필드 목록")
        private List<FieldDef> fields;

        @Getter @Setter
        @Schema(name = "ApprovalFormDto.CreateRequest.FieldDef")
        public static class FieldDef {

            @NotBlank
            @Size(max = 100)
            @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "필드 키는 영문자로 시작하는 영문자/숫자/밑줄만 사용할 수 있습니다.")
            @Schema(description = "필드 키 (결재 문서 항목의 key로 사용됨)", example = "reason")
            private String key;

            @NotBlank
            @Size(max = 255)
            @Schema(description = "필드 라벨", example = "사유")
            private String label;

            @NotBlank
            @Schema(description = "필드 타입 (TEXT, TEXTAREA, DATE, NUMBER, SELECT)", example = "TEXTAREA")
            private String type;

            @Schema(description = "필수 입력 여부", example = "true")
            private boolean required;

            @Schema(description = "선택지 목록 (SELECT 타입일 때만 사용)")
            private List<String> options;
        }
    }
}
