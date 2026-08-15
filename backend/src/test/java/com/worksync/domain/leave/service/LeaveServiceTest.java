// LeaveService 단위 테스트
package com.worksync.domain.leave.service;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.entity.ApprovalForm;
import com.worksync.domain.approval.entity.StepType;
import com.worksync.domain.approval.event.ApprovalApprovedEvent;
import com.worksync.domain.approval.event.ApprovalRejectedEvent;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalFormRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.leave.dto.LeaveDto;
import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.entity.LeaveStatus;
import com.worksync.domain.leave.entity.LeaveType;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.leave.repository.LeaveRequestRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** LeaveService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApprovalDocRepository approvalDocRepository;
    @Mock
    private ApprovalFormRepository approvalFormRepository;

    @InjectMocks
    private LeaveService leaveService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    @DisplayName("정상적으로 휴가를 신청하면 결재 문서와 결재선이 생성된다")
    @Test
    void request_success() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");
        ApprovalForm leaveForm = ApprovalForm.builder()
                .id(1L).formName("휴가 신청서").formType("LEAVE").formSchema("{}").build();

        LeaveDto.CreateRequest request = new LeaveDto.CreateRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 10));
        request.setEndDate(LocalDate.of(2026, 8, 11));
        request.setReason("개인 사유");
        request.setDayCount(BigDecimal.valueOf(2));
        request.setApproverId(2L);

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15)).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(leaveRequestRepository.existsOverlapping(eq(1L), eq(LeaveStatus.REJECTED), any(), any()))
                .willReturn(false);
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));
        given(approvalFormRepository.findByFormType("LEAVE")).willReturn(Optional.of(leaveForm));
        given(leaveRequestRepository.save(any(LeaveRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        LeaveDto.Response result = leaveService.request(employeeId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmployeeName()).isEqualTo("김철수");

        ArgumentCaptor<ApprovalDoc> docCaptor = ArgumentCaptor.forClass(ApprovalDoc.class);
        verify(approvalDocRepository).save(docCaptor.capture());
        ApprovalDoc savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getApprovalLines()).hasSize(2);
        assertThat(savedDoc.getApprovalLines().get(0).getStepType()).isEqualTo(StepType.DRAFT);
        assertThat(savedDoc.getApprovalLines().get(0).getStatus()).isEqualTo(ApprovalLineStatus.APPROVED);
        assertThat(savedDoc.getApprovalLines().get(1).getStepType()).isEqualTo(StepType.APPROVE);
        assertThat(savedDoc.getApprovalLines().get(1).getStatus()).isEqualTo(ApprovalLineStatus.WAITING);
    }

    @DisplayName("본인을 결재자로 지정하면 예외가 발생한다")
    @Test
    void request_selfApprover_throwsSelfApprovalNotAllowed() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");

        LeaveDto.CreateRequest request = new LeaveDto.CreateRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 10));
        request.setEndDate(LocalDate.of(2026, 8, 11));
        request.setDayCount(BigDecimal.valueOf(2));
        request.setApproverId(1L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when & then
        assertThatThrownBy(() -> leaveService.request(employeeId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
    }

    @DisplayName("동일 기간에 중복 신청하면 예외가 발생한다")
    @Test
    void request_overlapping_throwsDuplicateLeaveRequest() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");

        LeaveDto.CreateRequest request = new LeaveDto.CreateRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 10));
        request.setEndDate(LocalDate.of(2026, 8, 11));
        request.setDayCount(BigDecimal.valueOf(2));
        request.setApproverId(2L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(leaveRequestRepository.existsOverlapping(eq(1L), eq(LeaveStatus.REJECTED), any(), any()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> leaveService.request(employeeId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_LEAVE_REQUEST);
    }

    @DisplayName("잔여 연차가 부족하면 예외가 발생한다")
    @Test
    void request_insufficientBalance_throwsInsufficientLeaveBalance() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");
        Employee approver = buildEmployee(2L, "박부장");

        LeaveDto.CreateRequest request = new LeaveDto.CreateRequest();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setStartDate(LocalDate.of(2026, 8, 10));
        request.setEndDate(LocalDate.of(2026, 8, 11));
        request.setDayCount(BigDecimal.valueOf(2));
        request.setApproverId(2L);

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year((short) 2026)
                .totalDays(BigDecimal.valueOf(1)).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(approver));
        given(leaveRequestRepository.existsOverlapping(eq(1L), eq(LeaveStatus.REJECTED), any(), any()))
                .willReturn(false);
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when & then
        assertThatThrownBy(() -> leaveService.request(employeeId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
    }

    @DisplayName("휴가 양식이 아니면 승인 이벤트를 무시한다")
    @Test
    void onApprovalApproved_nonLeaveFormType_noop() {
        // given
        ApprovalApprovedEvent event = new ApprovalApprovedEvent(1L, "BUSINESS_TRIP");

        // when
        leaveService.onApprovalApproved(event);

        // then
        verifyNoInteractions(leaveRequestRepository, annualLeaveBalanceRepository);
    }

    @DisplayName("휴가 결재가 최종 승인되면 연차가 차감된다")
    @Test
    void onApprovalApproved_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .status(LeaveStatus.PENDING)
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15))
                .pendingDays(BigDecimal.valueOf(2))
                .build();

        ApprovalApprovedEvent event = new ApprovalApprovedEvent(1L, "LEAVE");

        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when
        leaveService.onApprovalApproved(event);

        // then
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(balance.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @DisplayName("승인 시점 재검증에서 잔여 연차가 부족하면 예외가 발생한다")
    @Test
    void onApprovalApproved_reVerifyInsufficientBalance_throwsInsufficientLeaveBalance() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .status(LeaveStatus.PENDING)
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year((short) 2026)
                .totalDays(BigDecimal.valueOf(2))
                .pendingDays(BigDecimal.valueOf(2))
                .build();

        ApprovalApprovedEvent event = new ApprovalApprovedEvent(1L, "LEAVE");

        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when & then
        assertThatThrownBy(() -> leaveService.onApprovalApproved(event))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
    }

    @DisplayName("연결된 휴가 신청이 없으면 승인 이벤트를 무시한다 (중복 차감 방지)")
    @Test
    void onApprovalApproved_leaveRequestNull_noop() {
        // given
        ApprovalApprovedEvent event = new ApprovalApprovedEvent(1L, "LEAVE");
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.empty());

        // when
        leaveService.onApprovalApproved(event);

        // then
        verifyNoInteractions(annualLeaveBalanceRepository);
    }

    @DisplayName("이미 처리된 휴가 신청이면 승인 이벤트를 무시한다 (중복 차감 방지)")
    @Test
    void onApprovalApproved_alreadyProcessed_noop() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .status(LeaveStatus.APPROVED)
                .build();
        ApprovalApprovedEvent event = new ApprovalApprovedEvent(1L, "LEAVE");
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));

        // when
        leaveService.onApprovalApproved(event);

        // then
        verifyNoInteractions(annualLeaveBalanceRepository);
    }

    @DisplayName("휴가 양식이 아니면 반려 이벤트를 무시한다")
    @Test
    void onApprovalRejected_nonLeaveFormType_noop() {
        // given
        ApprovalRejectedEvent event = new ApprovalRejectedEvent(1L, "BUSINESS_TRIP");

        // when
        leaveService.onApprovalRejected(event);

        // then
        verifyNoInteractions(leaveRequestRepository, annualLeaveBalanceRepository);
    }

    @DisplayName("휴가 결재가 반려되면 대기일수가 복구되고 반려 처리된다")
    @Test
    void onApprovalRejected_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .status(LeaveStatus.PENDING)
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year((short) 2026)
                .totalDays(BigDecimal.valueOf(15))
                .pendingDays(BigDecimal.valueOf(2))
                .build();

        ApprovalRejectedEvent event = new ApprovalRejectedEvent(1L, "LEAVE");

        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(1L, (short) 2026))
                .willReturn(Optional.of(balance));

        // when
        leaveService.onApprovalRejected(event);

        // then
        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(balance.getPendingDays()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(annualLeaveBalanceRepository).save(balance);
    }

    @DisplayName("연결된 휴가 신청이 없으면 반려 이벤트를 무시한다 (중복 처리 방지)")
    @Test
    void onApprovalRejected_leaveRequestNull_noop() {
        // given
        ApprovalRejectedEvent event = new ApprovalRejectedEvent(1L, "LEAVE");
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.empty());

        // when
        leaveService.onApprovalRejected(event);

        // then
        verifyNoInteractions(annualLeaveBalanceRepository);
    }

    @DisplayName("이미 처리된 휴가 신청이면 반려 이벤트를 무시한다 (중복 처리 방지)")
    @Test
    void onApprovalRejected_alreadyProcessed_noop() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(1L).employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 11))
                .daysCount(BigDecimal.valueOf(2))
                .status(LeaveStatus.REJECTED)
                .build();
        ApprovalRejectedEvent event = new ApprovalRejectedEvent(1L, "LEAVE");
        given(leaveRequestRepository.findByApprovalDocId(1L)).willReturn(Optional.of(leaveRequest));

        // when
        leaveService.onApprovalRejected(event);

        // then
        verifyNoInteractions(annualLeaveBalanceRepository);
    }
}
