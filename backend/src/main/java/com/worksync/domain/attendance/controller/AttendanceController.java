package com.worksync.domain.attendance.controller;

import com.worksync.domain.attendance.dto.AttendanceResponse;
import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

  private final AttendanceService attendanceService;

  // 출근 체크
  @PostMapping("/check-in")
  public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          HttpServletRequest request) {

    String clientIp = request.getRemoteAddr();

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.checkIn(userDetails.getId(), clientIp)));
  }

  // 퇴근 체크
  @PostMapping("/check-out")
  public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
          @AuthenticationPrincipal CustomUserDetails userDetails) {

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.checkOut(userDetails.getId())));
  }

  // 내 근태 월별 조회
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam("year") int year,
          @RequestParam("month") int month){

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.getMyAttendance(userDetails.getId(), year, month)));
  }

  // 단건 조회
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AttendanceResponse>> findById(
          @PathVariable("id") Long id) {

    return ResponseEntity.ok(ApiResponse.ok(attendanceService.findById(id)));
  }

  // ADMIN 전용 전체 조회
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByDate(
          @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    
    return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAttendanceByDate(date)));
  }
}
