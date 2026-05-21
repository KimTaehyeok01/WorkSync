package com.worksync.domain.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
public class ApprovalUpdateRequest {

    @NotBlank
    private String title;

    private Map<String, String> items;
}
