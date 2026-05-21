package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalLine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalCreateRequest {

    @NotNull
    private Long formId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotEmpty
    private List<ItemDto> items;

    @NotEmpty
    private List<ApproverDto> approvers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {
        @NotBlank
        private String itemKey;
        private String itemValue;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApproverDto {
        @NotNull
        private Long employeeId;
        @NotNull
        private Integer stepOrder;
        @NotNull
        private ApprovalLine.StepType stepType;
    }
}
