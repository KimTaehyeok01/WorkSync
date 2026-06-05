package com.worksync.domain.approval.controller;

import com.worksync.domain.approval.dto.ApprovalCreateRequest;
import com.worksync.domain.approval.dto.ApprovalDetailResponse;
import com.worksync.domain.approval.dto.ApprovalFormResponse;
import com.worksync.domain.approval.dto.ApprovalListResponse;
import com.worksync.domain.approval.dto.ApprovalProcessRequest;
import com.worksync.domain.approval.dto.ApprovalUpdateRequest;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.service.ApprovalService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /* 결재 양식 */

    // 양식 목록 조회
    @GetMapping("/forms")
    public ResponseEntity<ApiResponse<List<ApprovalFormResponse>>> getForms() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getForms()));
    }

    // 양식 단건 조회
    @GetMapping("/forms/{id}")
    public ResponseEntity<ApiResponse<ApprovalFormResponse>> getForm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getForm(id)));
    }

    /* 결재 문서 */

    // 결재 문서 제출
    @PostMapping
    public ResponseEntity<ApiResponse<ApprovalDetailResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalCreateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                approvalService.submit(userDetails.getId(), request)));
    }

    // 내가 상신한 문서 목록 (status=IN_PROGRESS|APPROVED|REJECTED)
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ApprovalListResponse>>> getMyDocs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ApprovalDocStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getMyDocs(userDetails.getId(), status)));
    }

    // 내가 결재해야 할 문서 목록 (내 차례인 것만)
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApprovalListResponse>>> getPendingDocs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getPendingDocs(userDetails.getId())));
    }

    // 결재 문서 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalDetailResponse>> getDoc(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getDoc(id)));
    }

    // 결재 문서 수정 (기안자 본인 + IN_PROGRESS만)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalDetailResponse>> updateDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.updateDoc(id, userDetails.getId(), request)));
    }

    // 결재 문서 취소 (기안자 본인 + IN_PROGRESS만)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        approvalService.deleteDoc(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 결재 처리 (승인 or 반려)
    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<ApprovalDetailResponse>> process(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalProcessRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.process(id, userDetails.getId(), request, clientIp, userAgent)));
    }
}
