package com.worksync.domain.leave.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.leave.dto.LeaveBalanceResponse;
import com.worksync.domain.leave.dto.LeaveCreateRequest;
import com.worksync.domain.leave.dto.LeaveResponse;
import com.worksync.domain.leave.entity.AnnualLeaveBalance;
import com.worksync.domain.leave.entity.LeaveRequest;
import com.worksync.domain.leave.repository.AnnualLeaveBalanceRepository;
import com.worksync.domain.leave.repository.LeaveRequestRepository;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    //휴가신청
    @Transactional
    public LeaveResponse request(Long employeeId, LeaveCreateRequest req) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Employee approver = employeeRepository.findById(req.getApproverId())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        //잔여 연차 부족 검증
        short currentYear = (short) LocalDate.now().getYear();
        AnnualLeaveBalance balance = annualLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseThrow(() -> new CustomException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE));

        if (balance.getRemainingDays().compareTo(req.getDayCount()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_LEAVE_BALANCE);
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .approver(approver)
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
                employee.getName()+"님이 휴가를 신청했습니다",
                "LEAVE",
                saved.getId()
        );
        return LeaveResponse.from(saved);
    }

    //연차 잔여 조회
    public LeaveBalanceResponse getBalance(Long employeeId){
        short currentYear=(short)LocalDate.now().getYear();

        AnnualLeaveBalance balance=annualLeaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseThrow(() -> new CustomException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));

        return LeaveBalanceResponse.from(balance);
    }
}
