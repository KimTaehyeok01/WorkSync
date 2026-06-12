package com.worksync.domain.approval.service;

import com.worksync.domain.approval.dto.*;
import com.worksync.domain.approval.entity.*;
import com.worksync.domain.approval.event.ApprovalApprovedEvent;
import com.worksync.domain.approval.event.ApprovalRejectedEvent;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalFormRepository;
import com.worksync.domain.approval.repository.ApprovalLineRepository;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveType;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.leave.repository.LeaveRequestRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    // 감사 로그 카테고리 / 액션명
    private static final String CATEGORY_APPROVAL = "APPROVAL";
    private static final String ACTION_APPROVE = "결재 승인";
    private static final String ACTION_REJECT = "결재 반려";

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

        // 결재선 유효성 검증 — REVIEW/APPROVE 라인 최소 1개 필수
        boolean hasReviewOrApprove = request.getApprovalLines().stream()
                .anyMatch(l -> l.getStepType() == StepType.REVIEW
                        || l.getStepType() == StepType.APPROVE);
        if (!hasReviewOrApprove) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_LINE);
        }

        approvalDocRepository.save(doc);

// ─────────────────────────────────────────────
// 전자결재 시스템을 통해 LEAVE(연차/휴가) 양식을 제출하면
// ApprovalDoc만 생성되고 LeaveRequest는 생성되지 않는다.
// 그런데 연차 차감 이벤트(onApprovalApproved)는
// LeaveRequest를 조회해서 차감 처리하는 구조이므로,
// LeaveRequest가 없으면 최종 승인이 되어도 연차가 차감되지 않는다.
// 따라서 LEAVE 타입 결재 문서 생성 시 LeaveRequest도 함께 생성해야 한다.
// ─────────────────────────────────────────────
        if ("LEAVE".equals(form.getFormType())) {
            Map<String, String> items = request.getItems();
            System.out.println("items:" + items);

            // null 체크
            String leaveTypeStr = items.get("leaveType");
            if (leaveTypeStr == null || leaveTypeStr.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_LEAVE_TYPE);
            }

            boolean isHalf = "HALF".equals(leaveTypeStr);

            // 날짜 파싱
            LocalDate startDate = isHalf
                    ? LocalDate.parse(items.get("halfDate"))
                    : LocalDate.parse(items.get("startDate"));
            LocalDate endDate = isHalf ? startDate : LocalDate.parse(items.get("endDate"));

            // 일수 계산
            BigDecimal daysCount = isHalf
                    ? BigDecimal.valueOf(0.5)
                    : BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate) + 1);

            System.out.println("daysCount: " + daysCount);

            // 잔여 연차 검증
            short leaveYear = (short) startDate.getYear();
            AnnualLeaveBalance balance = annualLeaveBalanceRepository
                    .findByEmployeeIdAndYear(drafterId, leaveYear)
                    .orElseGet(() -> annualLeaveBalanceRepository.save(
                            AnnualLeaveBalance.builder()
                                    .employee(drafter)
                                    .year(leaveYear)
                                    .totalDays(BigDecimal.valueOf(15))
                                    .build()));

            if (balance.getRemainingDays().compareTo(daysCount) < 0) {
                throw new CustomException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
            }

            // LeaveRequest 생성
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .employee(drafter)
                    .approvalDoc(doc)
                    .leaveType(LeaveType.valueOf(leaveTypeStr))
                    .startDate(startDate)
                    .endDate(endDate)
                    .daysCount(daysCount)
                    .reason(items.get("reason"))
                    .build();
            leaveRequestRepository.save(leaveRequest);

            balance.addPendingDays(daysCount);
            annualLeaveBalanceRepository.save(balance);
        }


        // 결재선 생성 — doc 컬렉션에 직접 추가
        for (ApprovalCreateRequest.ApprovalLineRequest lineReq : request.getApprovalLines()) {
            Employee approver = employeeRepository.findById(lineReq.getApproverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            ApprovalLine line = ApprovalLine.builder()
                    .doc(doc)
                    .approver(approver)
                    .stepOrder(lineReq.getStepOrder())
                    .stepType(lineReq.getStepType())
                    .build();

            // DRAFT 타입은 기안자 본인만 등록 가능, 제출 시 자동 승인
            if (lineReq.getStepType() == StepType.DRAFT) {
                if (!lineReq.getApproverId().equals(drafterId)) {
                    throw new CustomException(ErrorCode.NOT_YOUR_APPROVAL);
                }
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

    // 결재함 - 내가 결재선에 REVIEW/APPROVE로 포함된 문서 전체 (상태 필터링 가능)
    public  List<ApprovalListResponse> getApprovalBoxDocs(Long approverId, ApprovalDocStatus status){
        return approvalLineRepository
                .findByApproverId(approverId)
                .stream()
                .sorted(Comparator.comparing(line -> line.getDoc().getCreatedAt(), Comparator.reverseOrder())) // 정렬 기준: 문서 생성일, 방향: 내림차순 (최신순)
                .filter(line -> line.getStepType() == StepType.REVIEW
                || line.getStepType() == StepType.APPROVE)
                .filter(line -> status == null || line.getDoc().getStatus() == status)
                .map(line -> ApprovalListResponse.from(line.getDoc()))
                .distinct() // 중복제거
                .collect(Collectors.toList());
    }
    // 참조함 - 내가 REFERENCE로 지정된 문서
    public List<ApprovalListResponse> getReferenceDocs(Long approverId) {
        return approvalLineRepository
                .findByApproverId(approverId)// 내가 결재자로 지정된 결재선들을 가져온 다음
                .stream()
                .filter(line -> line.getStepType() == StepType.REFERENCE)
                .map(line -> ApprovalListResponse.from(line.getDoc())) // 그 결재선이 속한 문서를 꺼냄
                .distinct()
                .collect(Collectors.toList());
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

        // 이미 한 명이라도 승인한 경우 수정 불가
        boolean alreadyStarted = doc.getApprovalLines().stream()
                .filter(l -> l.getStepType() == StepType.REVIEW || l.getStepType() == StepType.APPROVE)
                .anyMatch(l -> l.getStatus() == ApprovalLineStatus.APPROVED);
        if (alreadyStarted) {
            throw new CustomException(ErrorCode.APPROVAL_EDIT_FORBIDDEN);
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

    // 결재 문서 취소/삭제 (기안자 본인 + IN_PROGRESS + 아직 아무도 승인 안 한 경우만 가능)
    @Transactional
    public void deleteDoc(Long id, Long drafterId) {
        ApprovalDoc doc = approvalDocRepository.findWithDetailsById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));

        if (!doc.getDrafter().getId().equals(drafterId)) {
            throw new CustomException(ErrorCode.NOT_YOUR_APPROVAL);
        }
        if (doc.getStatus() != ApprovalDocStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        // 이미 한 명이라도 승인한 경우 삭제 불가
        boolean alreadyStarted = doc.getApprovalLines().stream()
                .filter(l -> l.getStepType() == StepType.REVIEW || l.getStepType() == StepType.APPROVE)
                .anyMatch(l -> l.getStatus() == ApprovalLineStatus.APPROVED);
        if (alreadyStarted) {
            throw new CustomException(ErrorCode.APPROVAL_EDIT_FORBIDDEN);
        }

        approvalDocRepository.delete(doc);
    }

    // 결재 처리 (승인 or 반려)
    @Transactional
    public ApprovalDetailResponse process(Long docId, Long approverId, ApprovalProcessRequest request,
                                          String clientIp, String userAgent) {
        ApprovalDoc doc = approvalDocRepository.findWithDetailsById(docId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_DOC_NOT_FOUND));

        // 이미 완결된 문서는 처리 불가
        if (doc.getStatus() != ApprovalDocStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        // APPROVED / REJECTED 만 허용 (WAITING 상태 차단)
        if (request.getStatus() != ApprovalLineStatus.APPROVED
                && request.getStatus() != ApprovalLineStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STATUS);
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

            // 반려 이벤트 발행 → 구독 측(leave 등)이 후속 처리(휴가 신청 반려 등) 수행
            eventPublisher.publishEvent(
                    new ApprovalRejectedEvent(doc.getId(), doc.getForm().getFormType()));

            // 감사 로그 — 결재 반려
            auditLogService.log(myLine.getApprover().getId(), myLine.getApprover().getName(),
                    ACTION_REJECT, CATEGORY_APPROVAL, doc.getId(), clientIp, userAgent);
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

                // 최종 승인 이벤트 발행 → 구독 측(leave 등)이 후속 처리(연차 차감 등) 수행
                eventPublisher.publishEvent(
                        new ApprovalApprovedEvent(doc.getId(), doc.getForm().getFormType()));

                // 감사 로그 — 결재 최종 승인
                auditLogService.log(myLine.getApprover().getId(), myLine.getApprover().getName(),
                        ACTION_APPROVE, CATEGORY_APPROVAL, doc.getId(), clientIp, userAgent);
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