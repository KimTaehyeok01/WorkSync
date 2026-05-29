package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalForm;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class ApprovalFormResponse {

    private Long id;
    private String formName;
    private String formType;
    private String formSchema;
    private LocalDateTime createdAt;

    public static ApprovalFormResponse from(ApprovalForm form) {
        return ApprovalFormResponse.builder()
                .id(form.getId())
                .formName(form.getFormName())
                .formType(form.getFormType())
                .formSchema(form.getFormSchema())
                .createdAt(form.getCreatedAt())
                .build();
    }
}
