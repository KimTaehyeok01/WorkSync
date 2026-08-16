// ApprovalService 단위 테스트
package com.worksync.domain.approval.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worksync.domain.approval.dto.ApprovalDto;
import com.worksync.domain.approval.dto.ApprovalFormDto;
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
import org.mockito.Spy;
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

    // ApprovalService에 실제로 주입되는 ObjectMapper — 테스트 코드에서 검증용으로도 재사용
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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

    private ApprovalFormDto.CreateRequest.FieldDef fieldDef(String key, String label, String type,
                                                             boolean required, List<String> options) {
        ApprovalFormDto.CreateRequest.FieldDef field = new ApprovalFormDto.CreateRequest.FieldDef();
        field.setKey(key);
        field.setLabel(label);
        field.setType(type);
        field.setRequired(required);
        field.setOptions(options);
        return field;
    }

    private ApprovalFormDto.CreateRequest formCreateRequest(String formName,
                                                              List<ApprovalFormDto.CreateRequest.FieldDef> fields) {
        ApprovalFormDto.CreateRequest request = new ApprovalFormDto.CreateRequest();
        request.setFormName(formName);
        request.setFields(fields);
        return request;
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

    @DisplayName("커스텀 양식 생성 시 formType은 CUSTOM으로 고정되고 formSchema가 정확히 직렬화된다")
    @Test
    void createForm_success() throws Exception {
        // given
        ApprovalFormDto.CreateRequest request = formCreateRequest("동호회 지원 신청서", List.of(
                fieldDef("reason", "사유", "TEXTAREA", true, null),
                fieldDef("category", "분류", "SELECT", false, List.of("A", "B", "C"))
        ));

        given(approvalFormRepository.save(any(ApprovalForm.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ApprovalFormDto.Response result = approvalService.createForm(request);

        // then
        assertThat(result.getFormName()).isEqualTo("동호회 지원 신청서");
        assertThat(result.getFormType()).isEqualTo("CUSTOM");

        JsonNode fields = objectMapper.readTree(result.getFormSchema()).get("fields");
        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).get("key").asText()).isEqualTo("reason");
        assertThat(fields.get(0).get("type").asText()).isEqualTo("TEXTAREA");
        assertThat(fields.get(0).get("required").asBoolean()).isTrue();
        assertThat(fields.get(1).get("key").asText()).isEqualTo("category");
        assertThat(fields.get(1).get("options").get(0).asText()).isEqualTo("A");
        assertThat(fields.get(1).get("options").get(2).asText()).isEqualTo("C");
    }

    @DisplayName("필드 키가 중복되면 예외가 발생한다")
    @Test
    void createForm_duplicateFieldKey_throwsDuplicateFieldKey() {
        // given
        ApprovalFormDto.CreateRequest request = formCreateRequest("테스트 양식", List.of(
                fieldDef("reason", "사유1", "TEXT", true, null),
                fieldDef("reason", "사유2", "TEXT", false, null)
        ));

        // when & then
        assertThatThrownBy(() -> approvalService.createForm(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_FIELD_KEY);

        verify(approvalFormRepository, never()).save(any());
    }

    @DisplayName("지원하지 않는 필드 타입이면 예외가 발생한다")
    @Test
    void createForm_invalidFieldType_throwsInvalidFieldType() {
        // given
        ApprovalFormDto.CreateRequest request = formCreateRequest("테스트 양식", List.of(
                fieldDef("attachment", "첨부파일", "FILE", false, null)
        ));

        // when & then
        assertThatThrownBy(() -> approvalService.createForm(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIELD_TYPE);

        verify(approvalFormRepository, never()).save(any());
    }

    @DisplayName("SELECT 타입 필드에 옵션이 없으면 예외가 발생한다")
    @Test
    void createForm_selectWithoutOptions_throwsError() {
        // given
        ApprovalFormDto.CreateRequest request = formCreateRequest("테스트 양식", List.of(
                fieldDef("category", "분류", "SELECT", true, List.of())
        ));

        // when & then
        assertThatThrownBy(() -> approvalService.createForm(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SELECT_OPTIONS);

        verify(approvalFormRepository, never()).save(any());
    }

    @DisplayName("사용 중이지 않은 양식은 정상 삭제된다")
    @Test
    void deleteForm_success() {
        // given
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("커스텀 양식").formType("CUSTOM").formSchema("{}").build();

        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));
        given(approvalDocRepository.existsByForm_Id(1L)).willReturn(false);

        // when
        approvalService.deleteForm(1L);

        // then
        verify(approvalFormRepository).delete(form);
    }

    @DisplayName("이미 사용 중인 양식은 삭제할 수 없다")
    @Test
    void deleteForm_inUse_throwsFormInUse() {
        // given
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("커스텀 양식").formType("CUSTOM").formSchema("{}").build();

        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));
        given(approvalDocRepository.existsByForm_Id(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> approvalService.deleteForm(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORM_IN_USE);

        verify(approvalFormRepository, never()).delete(any());
    }

    @DisplayName("사용 이력이 없어도 시스템 폼(CUSTOM이 아닌 formType)은 삭제할 수 없다")
    @Test
    void deleteForm_systemForm_throwsFormInUse() {
        // given
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("연차 신청").formType("LEAVE").formSchema("{}").build();

        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when & then
        assertThatThrownBy(() -> approvalService.deleteForm(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORM_IN_USE);

        verify(approvalDocRepository, never()).existsByForm_Id(any());
        verify(approvalFormRepository, never()).delete(any());
    }

    @DisplayName("커스텀 폼 제출 시 필수 항목이 없으면(items가 null이어도) 예외가 발생한다")
    @Test
    void submit_customForm_missingRequiredField_throwsRequiredFieldMissing() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("커스텀 양식").formType("CUSTOM")
                .formSchema("{\"fields\":[{\"key\":\"reason\",\"label\":\"사유\",\"type\":\"TEXTAREA\",\"required\":true}]}")
                .build();

        ApprovalDto.CreateRequest request = createRequest(1L, "커스텀 신청",
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
                .isEqualTo(ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @DisplayName("커스텀 폼 제출 시 필수 항목이 모두 있으면 정상 제출된다")
    @Test
    void submit_customForm_allRequiredFieldsPresent_success() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("커스텀 양식").formType("CUSTOM")
                .formSchema("{\"fields\":[{\"key\":\"reason\",\"label\":\"사유\",\"type\":\"TEXTAREA\",\"required\":true}]}")
                .build();

        Map<String, String> items = new HashMap<>();
        items.put("reason", "긴급 처리 필요");

        ApprovalDto.CreateRequest request = createRequest(1L, "커스텀 신청",
                List.of(
                        lineRequest(1L, 1, StepType.DRAFT),
                        lineRequest(2L, 2, StepType.APPROVE)
                ), items);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(drafter));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(approvalFormRepository.findById(1L)).willReturn(Optional.of(form));

        // when
        ApprovalDto.DetailResponse result = approvalService.submit(drafterId, request);

        // then
        assertThat(result).isNotNull();
        verify(approvalDocRepository).save(any(ApprovalDoc.class));
    }

    @DisplayName("커스텀 폼 문서 수정 시 필수 항목이 누락되면 예외가 발생한다")
    @Test
    void updateDoc_customForm_missingRequiredField_throwsRequiredFieldMissing() {
        // given
        Long drafterId = 1L;
        Employee drafter = buildEmployee(1L, "김철수");
        ApprovalForm form = ApprovalForm.builder()
                .id(1L).formName("커스텀 양식").formType("CUSTOM")
                .formSchema("{\"fields\":[{\"key\":\"reason\",\"label\":\"사유\",\"type\":\"TEXTAREA\",\"required\":true}]}")
                .build();

        ApprovalDoc doc = ApprovalDoc.builder()
                .id(1L).drafter(drafter).form(form)
                .title("커스텀 신청")
                .status(ApprovalDocStatus.IN_PROGRESS)
                .build();

        given(approvalDocRepository.findWithDetailsById(1L)).willReturn(Optional.of(doc));

        ApprovalDto.UpdateRequest request = new ApprovalDto.UpdateRequest();
        request.setTitle("커스텀 신청 (수정)");
        Map<String, String> items = new HashMap<>();
        items.put("reason", "");
        request.setItems(items);

        // when & then
        assertThatThrownBy(() -> approvalService.updateDoc(1L, drafterId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
