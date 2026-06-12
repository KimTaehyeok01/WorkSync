package com.worksync.domain.attendance.dto;

import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.JobGrade;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class DepartmentAttendanceResponse {

    private Long employeeId;
    private String employeeName;
    private JobGrade jobGrade;
    private String profileImage;
    private AttendanceStatus status;     // 출근 기록 없으면 ABSENT
    private LocalDateTime checkInTime;   // 결근이면 null
    private LocalDateTime checkOutTime;  // 미퇴근/결근이면 null

    public static DepartmentAttendanceResponse of(Employee employee, Attendance attendance) {
        return DepartmentAttendanceResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .jobGrade(employee.getJobGrade())
                .profileImage(employee.getProfileImage())
                .status(attendance != null ? attendance.getStatus() : AttendanceStatus.ABSENT)
                .checkInTime(attendance != null ? attendance.getCheckInTime() : null)
                .checkOutTime(attendance != null ? attendance.getCheckOutTime() : null)
                .build();
    }
}
