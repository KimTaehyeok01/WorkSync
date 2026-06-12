package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.StepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
public class ApprovalCreateRequest {

    @NotNull
    private Long formId;

    @NotBlank
    private String title;

    @NotEmpty
    private List<ApprovalLineRequest> approvalLines;

    private Map<String, String> items;

    @Getter @Setter
    public static class ApprovalLineRequest {

        @NotNull
        private Long approverId;

        @NotNull
        private Integer stepOrder;

        @NotNull
        private StepType stepType;
    }
}
