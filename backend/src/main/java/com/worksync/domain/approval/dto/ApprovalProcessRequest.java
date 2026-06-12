package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalLineStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApprovalProcessRequest {

    @NotNull
    private ApprovalLineStatus status;

    private String comment;
}
