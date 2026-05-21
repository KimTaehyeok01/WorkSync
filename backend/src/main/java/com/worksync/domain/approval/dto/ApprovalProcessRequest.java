package com.worksync.domain.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalProcessRequest {

    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED")
    private String action;

    private String comment;
}
