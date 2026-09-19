// DashboardService 단위 테스트
package com.worksync.domain.dashboard.service;

import com.worksync.domain.approval.entity.ApprovalDoc;
import com.worksync.domain.approval.entity.ApprovalLine;
import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalLineRepository;
import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.attendance.repository.AttendanceRepository;
import com.worksync.domain.dashboard.dto.DashboardDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.notification.repository.NotificationRepository;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** DashboardService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ApprovalLineRepository approvalLineRepository;
    @Mock
    private ApprovalDocRepository approvalDocRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private AnnualLeaveBalanceRepository annualLeaveBalanceRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    @DisplayName("출근만 하고 퇴근 전인 경우 오늘 근태 상태와 잔여 연차가 반영된 대시보드를 반환한다")
    @Test
    void getDashboard_checkedInNotCheckedOut_success() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");
        short thisYear = (short) LocalDate.now().getYear();

        Attendance attendance = Attendance.builder()
                .id(1L).employee(employee).workDate(LocalDate.now())
                .checkInTime(LocalDateTime.now())
                .status(AttendanceStatus.NORMAL)
                .build();

        AnnualLeaveBalance balance = AnnualLeaveBalance.builder()
                .id(1L).employee(employee).year(thisYear)
                .totalDays(BigDecimal.valueOf(15)).usedDays(BigDecimal.valueOf(3)).build();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                .willReturn(Optional.of(attendance));
        given(approvalLineRepository.findByApproverIdAndStatus(employeeId, ApprovalLineStatus.WAITING))
                .willReturn(List.of(ApprovalLine.builder().build(), ApprovalLine.builder().build()));
        given(approvalDocRepository.findByDrafterId(employeeId))
                .willReturn(List.of(ApprovalDoc.builder().build()));
        given(notificationRepository.countByReceiverIdAndIsReadFalse(employeeId)).willReturn(5L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.TODO)).willReturn(4L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.IN_PROGRESS)).willReturn(2L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE)).willReturn(7L);
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(employeeId, thisYear))
                .willReturn(Optional.of(balance));

        // when
        DashboardDto.Response result = dashboardService.getDashboard(employeeId);

        // then
        assertThat(result.getTodayAttendanceStatus()).isEqualTo(AttendanceStatus.NORMAL);
        assertThat(result.isCheckedIn()).isTrue();
        assertThat(result.isCheckedOut()).isFalse();
        assertThat(result.getPendingApprovalCount()).isEqualTo(2);
        assertThat(result.getMyRequestedApprovalCount()).isEqualTo(1);
        assertThat(result.getUnreadNotificationCount()).isEqualTo(5);
        assertThat(result.getTodoTaskCount()).isEqualTo(4);
        assertThat(result.getInProgressTaskCount()).isEqualTo(2);
        assertThat(result.getDoneTaskCount()).isEqualTo(7);
        assertThat(result.getRemainingLeaveDays()).isEqualByComparingTo(BigDecimal.valueOf(12));
    }

    @DisplayName("퇴근까지 완료한 경우 퇴근 여부가 true로 반환된다")
    @Test
    void getDashboard_checkedOut_true() {
        // given
        Long employeeId = 1L;
        Employee employee = buildEmployee(1L, "김철수");
        short thisYear = (short) LocalDate.now().getYear();

        Attendance attendance = Attendance.builder()
                .id(1L).employee(employee).workDate(LocalDate.now())
                .checkInTime(LocalDateTime.now().minusHours(9))
                .checkOutTime(LocalDateTime.now())
                .status(AttendanceStatus.NORMAL)
                .build();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                .willReturn(Optional.of(attendance));
        given(approvalLineRepository.findByApproverIdAndStatus(employeeId, ApprovalLineStatus.WAITING))
                .willReturn(List.of());
        given(approvalDocRepository.findByDrafterId(employeeId)).willReturn(List.of());
        given(notificationRepository.countByReceiverIdAndIsReadFalse(employeeId)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.TODO)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.IN_PROGRESS)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE)).willReturn(0L);
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(employeeId, thisYear))
                .willReturn(Optional.empty());

        // when
        DashboardDto.Response result = dashboardService.getDashboard(employeeId);

        // then
        assertThat(result.isCheckedIn()).isTrue();
        assertThat(result.isCheckedOut()).isTrue();
    }

    @DisplayName("오늘 출근 기록과 연차 정보가 없으면 출퇴근 여부는 false, 근태 상태와 잔여 연차는 기본값으로 반환된다")
    @Test
    void getDashboard_withoutAttendanceAndBalance_defaultsToFalseAndZero() {
        // given
        Long employeeId = 1L;
        short thisYear = (short) LocalDate.now().getYear();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now()))
                .willReturn(Optional.empty());
        given(approvalLineRepository.findByApproverIdAndStatus(employeeId, ApprovalLineStatus.WAITING))
                .willReturn(List.of());
        given(approvalDocRepository.findByDrafterId(employeeId)).willReturn(List.of());
        given(notificationRepository.countByReceiverIdAndIsReadFalse(employeeId)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.TODO)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.IN_PROGRESS)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE)).willReturn(0L);
        given(annualLeaveBalanceRepository.findByEmployeeIdAndYear(employeeId, thisYear))
                .willReturn(Optional.empty());

        // when
        DashboardDto.Response result = dashboardService.getDashboard(employeeId);

        // then
        assertThat(result.getTodayAttendanceStatus()).isNull();
        assertThat(result.isCheckedIn()).isFalse();
        assertThat(result.isCheckedOut()).isFalse();
        assertThat(result.getRemainingLeaveDays()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
