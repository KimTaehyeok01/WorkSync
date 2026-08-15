// ApprovalService 단위 테스트
package com.worksync.domain.approval.service;

import com.worksync.domain.approval.dto.ApprovalDto;
import com.worksync.domain.approval.entity.ApprovalDoc;
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
}
