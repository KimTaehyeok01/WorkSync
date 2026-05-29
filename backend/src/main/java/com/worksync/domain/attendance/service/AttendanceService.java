package com.worksync.domain.attendance.service;

import com.worksync.domain.attendance.dto.AttendanceResponse;
import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.attendance.repository.AttendanceRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

  private final AttendanceRepository attendanceRepository;
  private final EmployeeRepository employeeRepository;

  @Transactional
  //출근체크 - 오늘 출근햇으면 예외처리 던져
  public AttendanceResponse checkIn(Long employeeId, String clientIp){

    // 사원조회 없으면 404
    Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

    LocalDate today = LocalDate.now(); // 오늘날짜
    LocalDateTime now = LocalDateTime.now(); // 현재시간

    // 오늘 출근했는지 확인
    if (attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today).isPresent()){
      throw new CustomException(ErrorCode.ALREADY_CHECKED_IN);
    }

    // 09:00시 이후출근이면 지각처리 1초라도 늦으면 지각 얄짤없음
    AttendanceStatus status = now.getHour() >= 9 ? AttendanceStatus.LATE : AttendanceStatus.NORMAL;

    // 출근기록 생성
    Attendance attendance = Attendance.builder()
            .employee(employee)
            .workDate(today)
            .checkInTime(now)
            .status(status)
            .clientIp(clientIp)
            .build();

    //DB 저장후 dto로 변환해서 반환
    return AttendanceResponse.from(attendanceRepository.save(attendance));
  }

  // 퇴근 체크
  // 오늘 출근 기록 없으면 예외 던지고 이미 퇴근해도 예외 던지고
  @Transactional
  public AttendanceResponse checkOut(Long employeeId){

    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    // 오늘 출근 기록조회
    Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId,today)
            .orElseThrow(()-> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));

    // 퇴근했는지 확인
    if (attendance.getCheckOutTime() != null){
      throw new CustomException(ErrorCode.ALREADY_CHECKED_OUT);
    }

    // 퇴근 시간 업데이트 자동으로 update실행
    attendance.checkOut(now);

    return AttendanceResponse.from(attendance);
  }

  // 내 근태 조회
  public List<AttendanceResponse> getMyAttendance(Long employeeId, int year, int month){
    LocalDate start = LocalDate.of(year, month, 1); // 해당월 첫째날 조회
    LocalDate end = start.withDayOfMonth(start.lengthOfMonth()); // 해당월 마지막날, lengthOfMonth() 해당월의 총 일수 반환

    return attendanceRepository.findByEmployeeIdAndWorkDateBetween(employeeId, start, end)
            .stream()
            .map(AttendanceResponse::from)
            .toList();
  }

  // ADMIN 전체 근태 조회
  public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
    return attendanceRepository.findByWorkDate(date)
            .stream()
            .map(AttendanceResponse::from)
            .toList();
  }
  // 단건 조회
  public AttendanceResponse findById(Long id) {
    Attendance attendance = attendanceRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));
    return AttendanceResponse.from(attendance);
  }
}
