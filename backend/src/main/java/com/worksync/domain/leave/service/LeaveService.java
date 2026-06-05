package com.worksync.domain.leave.service;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.StepType;
import com.worksync.domain.approval.event.ApprovalApprovedEvent;
import com.worksync.domain.approval.event.ApprovalRejectedEvent;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalFormRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.leave.dto.LeaveBalanceResponse;
import com.worksync.domain.leave.dto.LeaveCreateRequest;
import com.worksync.domain.leave.dto.LeaveResponse;
import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveStatus;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.leave.repository.LeaveRequestRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final ApprovalDocRepository approvalDocRepository;
    private  final ApprovalFormRepository approvalFormRepository;

    //휴가신청
    @Transactional
    public LeaveResponse request(Long employeeId, LeaveCreateRequest req) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Employee approver = employeeRepository.findById(req.getApproverId())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // 본인을 결재자로 지정 금지
        if (approver.getId().equals(employeeId)) {
            throw new CustomException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        }

        // 동일 기간 중복 신청 방지
        if (leaveRequestRepository.existsOverlapping(employeeId, req.getStartDate(), req.getEndDate())) {
            throw new CustomException(ErrorCode.DUPLICATE_LEAVE_REQUEST);
        }

        // 잔여 연차 부족 검증 — 휴가 시작일이 속한 연도의 연차 기준 (승인 시 차감 연도와 일치)
        short leaveYear = (short) req.getStartDate().getYear();
        // 해당 연도 연차 정보가 없으면 기본 15일로 자동 부여 후 진행
        AnnualLeaveBalance balance = annualLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, leaveYear)
                .orElseGet(() -> annualLeaveBalanceRepository.save(
                        AnnualLeaveBalance.builder()
                                .employee(employee)
                                .year(leaveYear)
                                .totalDays(BigDecimal.valueOf(15))
                                .build()));

        if (balance.getRemainingDays().compareTo(req.getDayCount()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
        }

        //approval_doc 생성
        ApprovalDoc approvalDoc=ApprovalDoc.builder()
                .drafter(employee)
                .form(approvalFormRepository.findByFormType("LEAVE")
                        .orElseThrow(()->new CustomException(ErrorCode.APPROVAL_FORM_NOT_FOUND)))
                .title(req.getLeaveType().name()+"신청 -"+employee.getName())
                .submittedAt(LocalDateTime.now())
                .build();

        ApprovalLine draftLine=ApprovalLine.builder()
                .doc(approvalDoc)
                .approver(employee)
                .stepOrder(1)
                .stepType(StepType.DRAFT)
                .build();
        draftLine.process(ApprovalLineStatus.APPROVED, null);

        ApprovalLine approvalLine = ApprovalLine.builder()
                .doc(approvalDoc)
                .approver(approver)
                .stepOrder(2)
                .stepType(StepType.APPROVE)
                .build();

        approvalDoc.getApprovalLines().add(draftLine);
        approvalDoc.getApprovalLines().add(approvalLine);
        approvalDocRepository.save(approvalDoc);

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .approver(approver)
                .approvalDoc(approvalDoc)
                .leaveType(req.getLeaveType())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .daysCount(req.getDayCount())
                .reason(req.getReason())
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        //결재자 알림 발송
        notificationService.send(
                approver.getId(),
                NotificationType.APPROVAL,
                employee.getName() + "님이 휴가를 신청했습니다",
                "LEAVE",
                saved.getId()
        );
        return LeaveResponse.from(saved);
    }

    //연차 잔여 조회
    public LeaveBalanceResponse getBalance(Long employeeId){
        short currentYear = (short)LocalDate.now().getYear();

        AnnualLeaveBalance balance = annualLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseThrow(() -> new CustomException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));

        return LeaveBalanceResponse.from(balance);
    }

    // 휴가 결재가 최종 승인되면 연차를 차감한다 (ApprovalApprovedEvent 구독)
    // process()의 트랜잭션 안에서 동기 실행되므로, 차감 실패 시 결재 승인도 함께 롤백된다.
    // @EventListener는 처음 써봐서 주석 많이 남깁니다.
    @EventListener
    @Transactional
    public void onApprovalApproved(ApprovalApprovedEvent event) {
        // 휴가 결재가 아니면 무시
        if (!"LEAVE".equals(event.formType())) {
            return;
        }

        // 결재 문서에 연결된 휴가 신청 조회 (없거나 이미 처리됐으면 중복 차감 방지)
        LeaveRequest leaveRequest = leaveRequestRepository.findByApprovalDocId(event.docId())
                .orElse(null);
        if (leaveRequest == null || leaveRequest.getStatus() != LeaveStatus.PENDING) {
            return;
        }

        // 신청 시작일 연도 기준 잔여 연차에서 차감
        short year = (short) leaveRequest.getStartDate().getYear();
        AnnualLeaveBalance balance = annualLeaveBalanceRepository
                .findByEmployeeIdAndYear(leaveRequest.getEmployee().getId(), year)
                .orElseThrow(() -> new CustomException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));

        // 승인 시점 잔여 재검증 — 신청 이후 다른 휴가가 차감돼 부족해졌을 수 있음 (음수 연차 방지)
        if (balance.getRemainingDays().compareTo(leaveRequest.getDaysCount()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
        }

        balance.useLeave(leaveRequest.getDaysCount());
        leaveRequest.approve();
    }

    // 휴가 결재가 반려되면 휴가 신청도 반려 처리한다 (ApprovalRejectedEvent 구독)
    @EventListener
    @Transactional
    public void onApprovalRejected(ApprovalRejectedEvent event) {
        // 휴가 결재가 아니면 무시
        if (!"LEAVE".equals(event.formType())) {
            return;
        }

        // 연결된 휴가 신청이 아직 대기 중일 때만 반려 처리
        LeaveRequest leaveRequest = leaveRequestRepository.findByApprovalDocId(event.docId())
                .orElse(null);
        if (leaveRequest == null || leaveRequest.getStatus() != LeaveStatus.PENDING) {
            return;
        }

        // 신청 시점에 차감하지 않았으므로 연차 복원은 불필요 — 상태만 REJECTED로 변경
        leaveRequest.reject();
    }
}
