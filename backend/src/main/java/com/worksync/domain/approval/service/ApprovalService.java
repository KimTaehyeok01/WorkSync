package com.worksync.domain.approval.service;

import com.worksync.domain.approval.dto.ApprovalCreateRequest;
import com.worksync.domain.approval.dto.ApprovalDetailResponse;
import com.worksync.domain.approval.dto.ApprovalFormResponse;
import com.worksync.domain.approval.dto.ApprovalListResponse;
import com.worksync.domain.approval.dto.ApprovalProcessRequest;
import com.worksync.domain.approval.dto.ApprovalUpdateRequest;
import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocItem;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.entity.ApprovalForm;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.StepType;
import com.worksync.domain.approval.repository.ApprovalDocItemRepository;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalFormRepository;
import com.worksync.domain.approval.repository.ApprovalLineRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalDocRepository approvalDocRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalFormRepository approvalFormRepository;
    private final ApprovalDocItemRepository approvalDocItemRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    /* 결재 양식 */

    // 양식 목록 조회
    public List<ApprovalFormResponse> getForms() {
        return approvalFormRepository.findAll().stream()
                .map(ApprovalFormResponse::from)
                .collect(Collectors.toList());
    }

    // 양식 단건 조회
    public ApprovalFormResponse getForm(Long id) {
        ApprovalForm form = approvalFormRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_FORM_NOT_FOUND));
        return ApprovalFormResponse.from(form);
    }

    /* 결재 문서 */

    // 결재 문서 제출
    @Transactional
    public ApprovalDetailResponse submit(Long drafterId, ApprovalCreateRequest request) {
        Employee drafter = employeeRepository
                .findById(drafterId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        ApprovalForm form = approvalFormRepository.findById(request.getFormId())
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_FORM_NOT_FOUND));

        // 결재 문서 생성
        ApprovalDoc doc = ApprovalDoc.builder()
                .drafter(drafter)
                .form(form)
                .title(request.getTitle())
                .submittedAt(LocalDateTime.now())
                .build();

        approvalDocRepository.save(doc);

        // 결재선 생성 — doc 컬렉션에 직접 추가 (cascade로 저장됨)
        for (ApprovalCreateRequest.ApprovalLineRequest lineReq : request.getApprovalLines()) {
            Employee approver = employeeRepository.findById(lineReq.getApproverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            ApprovalLine line = ApprovalLine.builder()
                    .doc(doc)
                    .approver(approver)
                    .stepOrder(lineReq.getStepOrder())
                    .stepType(lineReq.getStepType())
                    .build();

            // DRAFT 타입(기안자 본인)은 제출 시 자동 승인
            if (lineReq.getStepType() == StepType.DRAFT) {
                line.process(ApprovalLineStatus.APPROVED, null);
            }

            doc.getApprovalLines().add(line);
        }

        // 문서 항목(폼 필드 값) 저장 — doc 컬렉션에 직접 추가
        if (request.getItems() != null) {
            for (Map.Entry<String, String> entry : request.getItems().entrySet()) {
                doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                        .doc(doc)
                        .itemKey(entry.getKey())
                        .itemValue(entry.getValue())
                        .build());
            }
        }

        // 첫 번째 결재 순서의 결재자에게 알림
        int firstOrder = doc.getApprovalLines().stream()
                .filter(l -> l.getStepType() == StepType.REVIEW || l.getStepType() == StepType.APPROVE)
                .mapToInt(ApprovalLine::getStepOrder)
                .min()
                .orElse(Integer.MAX_VALUE);

        doc.getApprovalLines().stream()
                .filter(l -> l.getStepOrder() == firstOrder)
                .filter(l -> l.getStepType() == StepType.REVIEW || l.getStepType() == StepType.APPROVE)
                .forEach(l -> notificationService.send(
                        l.getApprover().getId(),
                        NotificationType.APPROVAL,
                        "'" + doc.getTitle() + "' 결재 요청이 도착했습니다.",
                        "APPROVAL",
                        doc.getId()
                ));

        return ApprovalDetailResponse.from(doc);
    }

    // 내가 상신한 문서 목록
    public List<ApprovalListResponse> getMyDocs(Long drafterId, ApprovalDocStatus status) {
        List<ApprovalDoc> docs = (status != null)
                ? approvalDocRepository.findByDrafterIdAndStatus(drafterId, status)
                : approvalDocRepository.findByDrafterId(drafterId);

        return docs.stream().map(ApprovalListResponse::from).collect(Collectors.toList());
    }

    // 내가 결재해야 할 문서 목록 (내 차례인 것만)
    public List<ApprovalListResponse> getPendingDocs(Long approverId) {
        return approvalLineRepository
                .findByApproverIdAndStatus(approverId, ApprovalLineStatus.WAITING)
                .stream()
                // REVIEW / APPROVE 타입만 결재 대상
                .filter(line -> line.getStepType() == StepType.REVIEW
                        || line.getStepType() == StepType.APPROVE)
                // 진행 중인 문서만 가능
                .filter(line -> line.getDoc().getStatus() == ApprovalDocStatus.IN_PROGRESS)
                // 내 순서인지 확인 (최소 WAITING stepOrder = 내 stepOrder)
                .filter(line -> {
                    int minOrder = line.getDoc().getApprovalLines().stream()
                            .filter(l -> l.getStatus() == ApprovalLineStatus.WAITING)
                            .filter(l -> l.getStepType() == StepType.REVIEW
                                    || l.getStepType() == StepType.APPROVE)
                            .mapToInt(ApprovalLine::getStepOrder)
                            .min()
                            .orElse(Integer.MAX_VALUE);
                    return line.getStepOrder() == minOrder;
                })
                .map(line -> ApprovalListResponse.from(line.getDoc()))
                .collect(Collectors.toList());
    }

    // 결재 문서 상세 조회
    public ApprovalDetailResponse getDoc(Long id) {
        ApprovalDoc doc = approvalDocRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));
        return ApprovalDetailResponse.from(doc);
    }

    // 결재 문서 수정 (기안자 본인 + IN_PROGRESS 상태만 가능)
    @Transactional
    public ApprovalDetailResponse updateDoc(Long id, Long drafterId, ApprovalUpdateRequest request) {
        ApprovalDoc doc = approvalDocRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));

        if (!doc.getDrafter().getId().equals(drafterId)) {
            throw new CustomException(ErrorCode.NOT_YOUR_APPROVAL);
        }
        if (doc.getStatus() != ApprovalDocStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        doc.updateTitle(request.getTitle());

        if (request.getItems() != null) {
            List<ApprovalDocItem> newItems = request.getItems().entrySet().stream()
                    .map(entry -> ApprovalDocItem.builder()
                            .doc(doc)
                            .itemKey(entry.getKey())
                            .itemValue(entry.getValue())
                            .build())
                    .collect(Collectors.toList());
            doc.replaceItems(newItems);
        }

        return ApprovalDetailResponse.from(doc);
    }

    // 결재 문서 취소/삭제 (기안자 본인 + IN_PROGRESS 상태만 가능)
    @Transactional
    public void deleteDoc(Long id, Long drafterId) {
        ApprovalDoc doc = approvalDocRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));

        if (!doc.getDrafter().getId().equals(drafterId)) {
            throw new CustomException(ErrorCode.NOT_YOUR_APPROVAL);
        }
        if (doc.getStatus() != ApprovalDocStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        approvalDocRepository.delete(doc);
    }

    // 결재 처리 (승인 or 반려)
    @Transactional
    public ApprovalDetailResponse process(Long docId, Long approverId, ApprovalProcessRequest request) {
        ApprovalDoc doc = approvalDocRepository.findWithDetailsById(docId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));

        // 이미 완결된 문서는 처리 불가
        if (doc.getStatus() != ApprovalDocStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        // 나의 WAITING 결재선 찾기
        ApprovalLine myLine = doc.getApprovalLines().stream()
                .filter(l -> l.getApprover().getId().equals(approverId))
                .filter(l -> l.getStatus() == ApprovalLineStatus.WAITING)
                .filter(l -> l.getStepType() == StepType.REVIEW
                        || l.getStepType() == StepType.APPROVE)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_YOUR_APPROVAL));

        // 내 차례인지 확인 (최소 WAITING stepOrder여야 처리 가능)
        int minWaitingOrder = doc.getApprovalLines().stream()
                .filter(l -> l.getStatus() == ApprovalLineStatus.WAITING)
                .filter(l -> l.getStepType() == StepType.REVIEW
                        || l.getStepType() == StepType.APPROVE)
                .mapToInt(ApprovalLine::getStepOrder)
                .min()
                .orElse(Integer.MAX_VALUE);

        if (myLine.getStepOrder() != minWaitingOrder) {
            throw new CustomException(ErrorCode.NOT_YOUR_APPROVAL);
        }

        // 결재 처리
        myLine.process(request.getStatus(), request.getComment());

        if (request.getStatus() == ApprovalLineStatus.REJECTED) {
            // 반려 → 즉시 문서 반려 + 기안자에게 알림
            doc.reject();
            notificationService.send(
                    doc.getDrafter().getId(),
                    NotificationType.APPROVAL,
                    "'" + doc.getTitle() + "' 결재가 반려됐습니다.",
                    "APPROVAL",
                    doc.getId()
            );
        } else {
            // 승인 → 모든 REVIEW/APPROVE 라인이 승인 완료면 문서 최종 승인
            boolean allApproved = doc.getApprovalLines().stream()
                    .filter(l -> l.getStepType() == StepType.REVIEW
                            || l.getStepType() == StepType.APPROVE)
                    .allMatch(l -> l.getStatus() == ApprovalLineStatus.APPROVED);

            if (allApproved) {
                // 최종 승인 → 기안자에게 알림
                doc.approve();
                notificationService.send(
                        doc.getDrafter().getId(),
                        NotificationType.APPROVAL,
                        "'" + doc.getTitle() + "' 결재가 최종 승인됐습니다.",
                        "APPROVAL",
                        doc.getId()
                );
            } else {
                // 중간 승인 → 다음 순서 결재자에게 알림
                int nextOrder = doc.getApprovalLines().stream()
                        .filter(l -> l.getStatus() == ApprovalLineStatus.WAITING)
                        .filter(l -> l.getStepType() == StepType.REVIEW
                                || l.getStepType() == StepType.APPROVE)
                        .mapToInt(ApprovalLine::getStepOrder)
                        .min()
                        .orElse(Integer.MAX_VALUE);

                doc.getApprovalLines().stream()
                        .filter(l -> l.getStepOrder() == nextOrder)
                        .filter(l -> l.getStepType() == StepType.REVIEW
                                || l.getStepType() == StepType.APPROVE)
                        .forEach(l -> notificationService.send(
                                l.getApprover().getId(),
                                NotificationType.APPROVAL,
                                "'" + doc.getTitle() + "' 결재 요청이 도착했습니다.",
                                "APPROVAL",
                                doc.getId()
                        ));
            }
        }

        return ApprovalDetailResponse.from(doc);
    }
}