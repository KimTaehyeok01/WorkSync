package com.worksync.domain.approval.dto;

import com.worksync.domain.approval.entity.ApprovalDoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalListResponse {

    private Long id;
    private String title;
    private ApprovalDoc.Status status;
    private String drafterName;
    private String drafterJobGrade;
    private String drafterProfileImage;
    private String formName;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
