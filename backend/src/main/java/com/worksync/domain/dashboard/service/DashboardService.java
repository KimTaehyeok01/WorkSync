package com.worksync.domain.dashboard.service;

import com.worksync.domain.approval.entity.ApprovalLineStatus;
import com.worksync.domain.approval.repository.ApprovalDocRepository;
import com.worksync.domain.approval.repository.ApprovalLineRepository;
import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.repository.AttendanceRepository;
import com.worksync.domain.dashboard.dto.DashboardResponse;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.notification.repository.NotificationRepository;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

  private final AttendanceRepository attendanceRepository;
  private final ApprovalLineRepository approvalLineRepository;
  private final ApprovalDocRepository approvalDocRepository;
  private final NotificationRepository notificationRepository;
  private final TaskRepository taskRepository;
  private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;

  public DashboardResponse getDashboard(Long employeeId){

    // 오늘 근태
    Optional<Attendance> todayAttendance =
            attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now());

    // 내가 처리해야할 결재 건수
    long pendingApprovalCount =
            approvalLineRepository.findByApproverIdAndStatus(employeeId, ApprovalLineStatus.WAITING).size();

    // 내가 요청한 결재 건수
    long myRequestApprovalCount =
            approvalDocRepository.findByDrafterId(employeeId).size();

    // 안읽은 알림 건수
    long unreadNotificationCount =
            notificationRepository.countByReceiverIdAndIsReadFalse(employeeId);

    // 업무 상태별로 카운트됨
    long todoTaskCount = taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.TODO);
    long inProgressTaskCount = taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.IN_PROGRESS);
    long doneTaskCount = taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE);

    // 잔여 연차 일수 (올해기준) 대기
    short thisYear = (short) LocalDate.now().getYear();
    BigDecimal remainingLeaveDays = annualLeaveBalanceRepository
            .findByEmployeeIdAndYear(employeeId, thisYear)
            .map(b -> b.getTotalDays().subtract(b.getUsedDays()))
            .orElse(BigDecimal.ZERO);

    return DashboardResponse.builder()
            // 출근기록 있으면 상태(NORMAL/LATE), 없으면 NULL
            .todayAttendanceStatus(todayAttendance.map(Attendance::getStatus).orElse(null))
            // 출근 기록 자체가 존재하는지 확인
            .checkedIn(todayAttendance.isPresent())
            // checkOutTime이 null이 아니면 퇴근한거
            .checkedOut(todayAttendance.map(a -> a.getCheckOutTime() != null).orElse(false))
            .pendingApprovalCount(pendingApprovalCount)
            .myRequestedApprovalCount(myRequestApprovalCount)
            .unreadNotificationCount(unreadNotificationCount)
            .todoTaskCount(todoTaskCount)
            .inProgressTaskCount(inProgressTaskCount)
            .doneTaskCount(doneTaskCount)
            .remainingLeaveDays(remainingLeaveDays)
            .build();
  }
}
