// ApprovalService 단위 테스트
package com.worksync.domain.approval.service;

import com.worksync.domain.approval.dto.ApprovalDto;
import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalDocItem;
import com.worksync.domain.approval.entity.ApprovalDocStatus;
import com.worksync.domain.approval.entity.ApprovalForm;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.StepType;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** ApprovalService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalDocRepository approvalDocRepository;
    @Mock
    private ApprovalLineRepository approvalLineRepository;
    @Mock
    private ApprovalFormRepository approvalFormRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private ApprovalService approvalService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    private ApprovalDto.CreateRequest.ApprovalLineRequest lineRequest(Long approverId, int stepOrder, StepType stepType) {
        ApprovalDto.CreateRequest.ApprovalLineRequest line = new ApprovalDto.CreateRequest.ApprovalLineRequest();
        line.setApproverId(approverId);
        line.setStepOrder(stepOrder);
        line.setStepType(stepType);
        return line;
    }

    private ApprovalDto.CreateRequest createRequest(Long formId, String title,
                                                      List<ApprovalDto.CreateRequest.ApprovalLineRequest> lines,
                                                      Map<String, String> items) {
        ApprovalDto.CreateRequest request = new ApprovalDto.CreateRequest();
        request.setFormId(formId);
        request.setTitle(title);
        request.setApprovalLines(lines);
        request.setItems(items);
        return request;
    }

    @DisplayName("일반 문서를 정상 제출하면 결재 문서가 저장된다")
    @Test
    void submit_success() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDto.CreateRequest request = createRequest(1L, "출장 신청서",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), null);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when
        ApprovalDto.DetailResponse result = approvalService.submit(drafterId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("출장 신청서");
        verify(approvalDocRepository).save(any(ApprovalDoc.class));
        verify(notificationService).send(eq(2L), eq(NotificationType.APPROVAL), anyString(), eq("APPROVAL"), any());
    }

    @DisplayName("결재선에 REVIEW/APPROVE가 없으면 예외가 발생한다")
    @Test
    void submit_noReviewOrApproveLine_throwsInvalidApprovalLine() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDto.CreateRequest request = createRequest(1L, "출장 신청서",
                List.of(lineRequest(1L, 1, StepType.DRAFT)), null);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when & then
        assertThatThrownBy(() -> approvalService.submit(drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_APPROVAL_LINE);
    }

    @DisplayName("LEAVE 양식 제출 시 휴가 신청과 연차 대기일수가 반영된다")
    @Test
    void submit_leaveType_success() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        Map<String, String> items = new HashMap<>();
        items.put("leaveType", "ANNUAL");
        items.put("startDate", "2026-08-10");
        items.put("endDate", "2026-08-11");
        items.put("reason", "개인 사유");

        ApprovalDto.CreateRequest request = createRequest(1L, "연차 신청",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), items);

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15)).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when
        ApprovalDto.DetailResponse result = approvalService.submit(drafterId, request);

        // then
        assertThat(result).isNotNull();
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
        assertThat(balance.getPendingDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @DisplayName("휴가 유형 값이 유효하지 않으면 INVALID_LEAVE_TYPE 예외가 발생한다")
    @Test
    void submit_leaveType_invalidLeaveTypeString_throwsInvalidLeaveType() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        Map<String, String> items = new HashMap<>();
        items.put("leaveType", "INVALID_TYPE");
        items.put("startDate", "2026-08-10");
        items.put("endDate", "2026-08-11");

        ApprovalDto.CreateRequest request = createRequest(1L, "연차 신청",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), items);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when & then
        assertThatThrownBy(() -> approvalService.submit(drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LEAVE_TYPE);
    }

    @DisplayName("LEAVE 양식 제출 시 items가 null이면 INVALID_LEAVE_TYPE 예외가 발생한다")
    @Test
    void submit_leaveType_nullItems_throwsInvalidLeaveType() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDto.CreateRequest request = createRequest(1L, "연차 신청",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), null);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when & then
        assertThatThrownBy(() -> approvalService.submit(drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LEAVE_TYPE);
    }

    @DisplayName("잔여 연차가 부족하면 예외가 발생한다")
    @Test
    void submit_leaveType_insufficientBalance_throwsInsufficientLeaveBalance() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        Map<String, String> items = new HashMap<>();
        items.put("leaveType", "ANNUAL");
        items.put("startDate", "2026-08-10");
        items.put("endDate", "2026-08-11");

        ApprovalDto.CreateRequest request = createRequest(1L, "연차 신청",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), items);

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(1)).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when & then
        assertThatThrownBy(() -> approvalService.submit(drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
    }

    @DisplayName("DRAFT 결재선의 결재자가 기안자 본인이 아니면 예외가 발생한다")
    @Test
    void submit_draftLineNotSelf_throwsNotYourApproval() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        Employee other = buildEmployee(99L, "이과장");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDto.CreateRequest request = createRequest(1L, "출장 신청서",
                List.of(
                        lineRequest(99L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), null);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));
        given(employeeRepository.findById(99L)).willReturn(Optional.of(other));

        // when & then
        assertThatThrownBy(() -> approvalService.submit(drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_YOUR_APPROVAL);
    }

    @DisplayName("이미 완결된 문서는 처리할 수 없다")
    @Test
    void process_alreadyCompleted_throwsAlreadyProcessed() {
        // given
        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L)
                .status(ApprovalDocStatus.APPROVED)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.APPROVED);

        // when & then
        assertThatThrownBy(() -> approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @DisplayName("결재 처리 상태가 APPROVED/REJECTED가 아니면 예외가 발생한다")
    @Test
    void process_invalidStatus_throwsInvalidApprovalStatus() {
        // given
        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L)
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.WAITING);

        // when & then
        assertThatThrownBy(() -> approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_APPROVAL_STATUS);
    }

    @DisplayName("내 차례가 아니면 결재를 처리할 수 없다")
    @Test
    void process_notMyTurn_throwsNotYourApproval() {
        // given
        Employee approver1 = buildEmployee(1L, "선배결재자");
        Employee approver2 = buildEmployee(2L, "내결재자");

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L)
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line1 = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver1).stepOrder(1).stepType(StepType.REVIEW)
                .status(ApprovalLineStatus.WAITING).build();
        ApprovalLine line2 = ApprovalLine.builder()
                .id(2L).doc(doc).approver(approver2).stepOrder(2).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line1);
        doc.getApprovalLines().add(line2);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.APPROVED);

        // when & then
        assertThatThrownBy(() -> approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_YOUR_APPROVAL);
    }

    @DisplayName("반려 처리 시 문서가 반려되고 반려 이벤트가 발행된다")
    @Test
    void process_reject_publishesRejectedEvent() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver = buildEmployee(2L, "결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver).stepOrder(1).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.REJECTED);
        request.setComment("반려합니다");

        // when
        approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit");

        // then
        assertThat(doc.getStatus()).isEqualTo(ApprovalDocStatus.REJECTED);
        verify(notificationService).send(eq(1L), eq(NotificationType.APPROVAL), anyString(), eq("APPROVAL"), eq(1L));

        ArgumentCaptor<ApprovalRejectedEvent> captor = ArgumentCaptor.forClass(ApprovalRejectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().docId()).isEqualTo(1L);
        assertThat(captor.getValue().formType()).isEqualTo("BUSINESS_TRIP");
    }

    @DisplayName("마지막 결재자 승인 시 문서가 최종 승인되고 승인 이벤트가 발행된다")
    @Test
    void process_finalApprove_publishesApprovedEvent() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver = buildEmployee(2L, "결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver).stepOrder(1).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.APPROVED);

        // when
        approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit");

        // then
        assertThat(doc.getStatus()).isEqualTo(ApprovalDocStatus.APPROVED);
        verify(eventPublisher).publishEvent(any(ApprovalApprovedEvent.class));
        verify(auditLogService).log(eq(2L), eq("결재자"), anyString(), anyString(), eq(1L), anyString(), anyString());
    }

    @DisplayName("중간 승인 시 다음 순번 결재자에게만 알림이 발송된다")
    @Test
    void process_middleApprove_notifiesNextApprover() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver1 = buildEmployee(2L, "1차결재자");
        Employee approver2 = buildEmployee(3L, "2차결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line1 = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver1).stepOrder(1).stepType(StepType.REVIEW)
                .status(ApprovalLineStatus.WAITING).build();
        ApprovalLine line2 = ApprovalLine.builder()
                .id(2L).doc(doc).approver(approver2).stepOrder(2).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line1);
        doc.getApprovalLines().add(line2);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.ProcessRequest request = new ApprovalDto.ProcessRequest();
        request.setStatus(ApprovalLineStatus.APPROVED);

        // when
        approvalService.process(1L, 2L, request, "127.0.0.1", "JUnit");

        // then
        assertThat(doc.getStatus()).isEqualTo(ApprovalDocStatus.IN_PROGRESS);
        verify(notificationService, times(1))
                .send(eq(3L), eq(NotificationType.APPROVAL), anyString(), eq("APPROVAL"), eq(1L));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @DisplayName("기안자가 아무도 승인하지 않은 문서를 회수하면 WITHDRAWN 상태가 된다")
    @Test
    void withdraw_success() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver = buildEmployee(2L, "결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver).stepOrder(1).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when
        ApprovalDto.DetailResponse result = approvalService.withdraw(1L, 1L);

        // then
        assertThat(result.getStatus()).isEqualTo(ApprovalDocStatus.WITHDRAWN);
        assertThat(doc.getStatus()).isEqualTo(ApprovalDocStatus.WITHDRAWN);
        verify(approvalDocRepository).lockById(1L);
    }

    @DisplayName("기안자 본인이 아니면 회수할 수 없다")
    @Test
    void withdraw_notOwner_throwsNotYourApproval() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> approvalService.withdraw(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_YOUR_APPROVAL);
    }

    @DisplayName("IN_PROGRESS 상태가 아닌 문서는 회수할 수 없다")
    @Test
    void withdraw_notInProgress_throwsNotWithdrawable() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.APPROVED)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> approvalService.withdraw(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPROVAL_NOT_WITHDRAWABLE);
    }

    @DisplayName("이미 한 명이라도 승인한 문서는 회수할 수 없다")
    @Test
    void withdraw_alreadyApproved_throwsApprovalEditForbidden() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver = buildEmployee(2L, "결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        ApprovalLine line = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver).stepOrder(1).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.APPROVED).build();
        doc.getApprovalLines().add(line);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> approvalService.withdraw(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPROVAL_EDIT_FORBIDDEN);
    }

    @DisplayName("기안자가 회수된 문서를 재상신하면 IN_PROGRESS로 복귀하고 첫 결재자에게 알림이 발송된다")
    @Test
    void resubmit_success() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        Employee approver = buildEmployee(2L, "결재자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.WITHDRAWN)
                .build();

        ApprovalLine line = ApprovalLine.builder()
                .id(1L).doc(doc).approver(approver).stepOrder(1).stepType(StepType.APPROVE)
                .status(ApprovalLineStatus.WAITING).build();
        doc.getApprovalLines().add(line);

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when
        ApprovalDto.DetailResponse result = approvalService.resubmit(1L, 1L);

        // then
        assertThat(result.getStatus()).isEqualTo(ApprovalDocStatus.IN_PROGRESS);
        verify(notificationService).send(eq(2L), eq(NotificationType.APPROVAL), anyString(), eq("APPROVAL"), eq(1L));
        verify(approvalDocRepository).lockById(1L);
    }

    @DisplayName("기안자 본인이 아니면 재상신할 수 없다")
    @Test
    void resubmit_notOwner_throwsNotYourApproval() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.WITHDRAWN)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> approvalService.resubmit(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_YOUR_APPROVAL);
    }

    @DisplayName("WITHDRAWN 상태가 아닌 문서는 재상신할 수 없다")
    @Test
    void resubmit_notWithdrawn_throwsNotResubmittable() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> approvalService.resubmit(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPROVAL_NOT_RESUBMITTABLE);
    }

    @DisplayName("WITHDRAWN 상태의 문서도 수정할 수 있다 (회귀)")
    @Test
    void updateDoc_withdrawnStatus_success() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.WITHDRAWN)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("출장 신청서 (수정)");

        // when
        ApprovalDto.DetailResponse result = approvalService.updateDoc(1L, 1L, request);

        // then
        assertThat(result.getTitle()).isEqualTo("출장 신청서 (수정)");
        verify(approvalDocRepository).lockById(1L);
    }

    @DisplayName("LEAVE 문서를 같은 연도 안에서 날짜만 변경하면 잔여일수가 재동기화된다")
    @Test
    void updateDoc_leaveType_sameYear_syncsBalance() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("연차 신청")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("leaveType").itemValue("ANNUAL").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("startDate").itemValue("2026-08-10").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("endDate").itemValue("2026-08-11").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("reason").itemValue("개인 사유").build());

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(drafter).approvalDoc(doc)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .reason("개인 사유")
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15)).pendingDays(BigDecimal.valueOf(2)).build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        Map<String, String> newItems = new HashMap<>();
        newItems.put("leaveType", "ANNUAL");
        newItems.put("startDate", "2026-08-12");
        newItems.put("endDate", "2026-08-13");
        newItems.put("reason", "사유 변경");

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("연차 신청 (수정)");
        request.setItems(newItems);

        // when
        ApprovalDto.DetailResponse result = approvalService.updateDoc(1L, 1L, request);

        // then
        assertThat(balance.getPendingDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(leaveRequest.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(leaveRequest.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 13));
        assertThat(leaveRequest.getReason()).isEqualTo("사유 변경");
        assertThat(result.getItems().get("startDate")).isEqualTo("2026-08-12");
        verify(leaveRequestRepository).save(leaveRequest);
        verify(annualLeaveBalanceRepository, times(2)).save(balance);
    }

    @DisplayName("LEAVE 문서 수정으로 연도가 바뀌면 옛 연도와 새 연도 잔여일수가 각각 갱신된다")
    @Test
    void updateDoc_leaveType_yearBoundary_movesBalanceToNewYear() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("연차 신청")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("leaveType").itemValue("ANNUAL").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("startDate").itemValue("2026-12-30").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("endDate").itemValue("2026-12-31").build());

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(drafter).approvalDoc(doc)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 12, 30))
                .endDate(LocalDate.of(2026, 12, 31))
                .daysCount(BigDecimal.valueOf(2))
                .build();

        AnnualLeaveBalance oldYearBalance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15)).pendingDays(BigDecimal.valueOf(2)).build();
        AnnualLeaveBalance newYearBalance = AnnualLeaveBalance.builder()
                .id(2L).employee(drafter).year((short) 2027)
                .totalDays(BigDecimal.valueOf(15)).pendingDays(BigDecimal.ZERO).build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(oldYearBalance));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2027))
                .willReturn(Optional.of(newYearBalance));

        Map<String, String> newItems = new HashMap<>();
        newItems.put("leaveType", "ANNUAL");
        newItems.put("startDate", "2027-01-01");
        newItems.put("endDate", "2027-01-02");

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("연차 신청 (수정)");
        request.setItems(newItems);

        // when
        approvalService.updateDoc(1L, 1L, request);

        // then
        assertThat(oldYearBalance.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(newYearBalance.getPendingDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
        verify(annualLeaveBalanceRepository).findByEmployeeIdAndYear(1L, (short) 2026);
        verify(annualLeaveBalanceRepository).findByEmployeeIdAndYear(1L, (short) 2027);
        verify(annualLeaveBalanceRepository).save(oldYearBalance);
        verify(annualLeaveBalanceRepository).save(newYearBalance);
    }

    @DisplayName("변경된 날짜 기준 잔여일수가 부족하면 예외가 발생한다")
    @Test
    void updateDoc_leaveType_insufficientNewBalance_throwsInsufficientLeaveBalance() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("연차 신청")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("leaveType").itemValue("ANNUAL").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("startDate").itemValue("2026-08-10").build());
        doc.getApprovalDocItems().add(ApprovalDocItem.builder()
                .doc(doc).itemKey("endDate").itemValue("2026-08-11").build());

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(drafter).approvalDoc(doc)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .build();

        // 옛 대기일수(2일) 복구 후 남는 잔여일수는 3일 -> 새로 요청하는 4일보다 부족
        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(3)).pendingDays(BigDecimal.valueOf(2)).build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        Map<String, String> newItems = new HashMap<>();
        newItems.put("leaveType", "ANNUAL");
        newItems.put("startDate", "2026-08-15");
        newItems.put("endDate", "2026-08-18");

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("연차 신청 (수정)");
        request.setItems(newItems);

        // when & then
        assertThatThrownBy(() -> approvalService.updateDoc(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
    }

    @DisplayName("연결된 휴가 신청을 찾을 수 없으면 예외가 발생한다")
    @Test
    void updateDoc_leaveType_missingLeaveRequest_throwsLeaveRequestNotFound() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("연차 신청")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.empty());

        Map<String, String> newItems = new HashMap<>();
        newItems.put("leaveType", "ANNUAL");
        newItems.put("startDate", "2026-08-15");
        newItems.put("endDate", "2026-08-16");

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("연차 신청 (수정)");
        request.setItems(newItems);

        // when & then
        assertThatThrownBy(() -> approvalService.updateDoc(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEAVE_REQUEST_NOT_FOUND);
    }

    @DisplayName("WITHDRAWN 상태의 문서도 삭제할 수 있다 (회귀)")
    @Test
    void deleteDoc_withdrawnStatus_success() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("출장 신청서").formType("BUSINESS_TRIP").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("출장 신청서")
                .status(ApprovalDocStatus.WITHDRAWN)
                .build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        // when
        approvalService.deleteDoc(1L, 1L);

        // then
        verify(approvalDocRepository).delete(doc);
        verify(approvalDocRepository).lockById(1L);
    }

    @DisplayName("회수된 LEAVE 문서를 삭제하면 대기 중이던 연차 잔여일수가 복구된다")
    @Test
    void deleteDoc_leaveType_withdrawnStatus_restoresPendingDays() {
        // given
        Employee drafter = buildEmployee(1L, "기안자");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("연차 신청")
                .status(ApprovalDocStatus.WITHDRAWN)
                .build();

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(drafter).approvalDoc(doc)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(drafter).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15)).pendingDays(BigDecimal.valueOf(2)).build();

        given(approvalDocRepository.lockById(1L)).willReturn(Optional.of(doc));
        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when
        approvalService.deleteDoc(1L, 1L);

        // then
        assertThat(balance.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(leaveRequestRepository).delete(leaveRequest);
    }
}
