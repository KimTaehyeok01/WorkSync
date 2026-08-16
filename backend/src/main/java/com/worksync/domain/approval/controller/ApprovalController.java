package com.worksync.domain.approval.controller;

import com.worksync.domain.approval.dto.ApprovalDto;
import com.worksync.domain.approval.dto.ApprovalFormDto;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.service.ApprovalService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Approval", description = "전자결재 API")
@Slf4j
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /* 결재 양식 */

    // 양식 목록 조회
    @Operation(summary = "결재 양식 목록 조회")
    @GetMapping("/forms")
    public ResponseEntity<ApiResponse<List<ApprovalFormDto.Response>>> getForms() {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getForms()));
    }

    // 양식 단건 조회
    @Operation(summary = "결재 양식 단건 조회")
    @GetMapping("/forms/{id}")
    public ResponseEntity<ApiResponse<ApprovalFormDto.Response>> getForm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getForm(id)));
    }

    /* 결재 문서 */

    // 결재 문서 제출
    @Operation(summary = "결재 문서 제출")
    @PostMapping
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalDto.CreateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                approvalService.submit(userDetails.getId(), request)));
    }
    
    // 내가 상신한 문서 목록 (status=IN_PROGRESS|APPROVED|REJECTED)
    @Operation(summary = "내가 상신한 결재 문서 목록 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ApprovalDto.ListResponse>>> getMyDocs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ApprovalDocStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getMyDocs(userDetails.getId(), status)));
    }

    // 내가 결재해야 할 문서 목록 (내 차례인 것만)
    @Operation(summary = "내가 결재해야 할 문서 목록 조회")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApprovalDto.ListResponse>>> getPendingDocs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getPendingDocs(userDetails.getId())));
    }

    // 결재함 - 내가 결재선에 포함된 문서 (REVIEW/APPROVE)
    @Operation(summary = "결재함 조회 (내가 결재선에 포함된 문서)")
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<List<ApprovalDto.ListResponse>>> getApprovalBox (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ApprovalDocStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getApprovalBoxDocs(userDetails.getId(), status)
        ));
    }

    // 참조함 - 내가 REFERENCE로 지정된 문서
    @Operation(summary = "참조함 조회 (내가 참조자로 지정된 문서)")
    @GetMapping("/reference")
    public ResponseEntity<ApiResponse<List<ApprovalDto.ListResponse>>> getReference (
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.getReferenceDocs(userDetails.getId())
        ));
    }

    // 결재 문서 상세 조회
    @Operation(summary = "결재 문서 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> getDoc(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalService.getDoc(id)));
    }

    // 결재 문서 수정 (기안자 본인 + IN_PROGRESS만)
    @Operation(summary = "결재 문서 수정 (기안자 본인 + IN_PROGRESS 상태만)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> updateDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.updateDoc(id, userDetails.getId(), request)));
    }

    // 결재 문서 취소 (기안자 본인 + IN_PROGRESS만)
    @Operation(summary = "결재 문서 취소 (기안자 본인 + IN_PROGRESS 상태만)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        approvalService.deleteDoc(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 결재 문서 회수 (기안자 본인 + IN_PROGRESS + 아직 아무도 승인 안 한 경우만)
    @Operation(summary = "결재 문서 회수 (기안자 본인 + IN_PROGRESS 상태만)")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.withdraw(id, userDetails.getId())));
    }

    // 결재 문서 재상신 (기안자 본인 + WITHDRAWN 상태만)
    @Operation(summary = "결재 문서 재상신 (기안자 본인 + WITHDRAWN 상태만)")
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> resubmit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.resubmit(id, userDetails.getId())));
    }

    // 결재 처리 (승인 or 반려)
    @Operation(summary = "결재 처리 (승인 또는 반려)")
    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<ApprovalDto.DetailResponse>> process(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ApprovalDto.ProcessRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(
                approvalService.process(id, userDetails.getId(), request, clientIp, userAgent)));
    }
}
