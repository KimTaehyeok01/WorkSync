package com.worksync.domain.attendance.controller;

import com.worksync.domain.attendance.dto.AttendanceDto;
import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Attendance", description = "근태 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

  private final AttendanceService attendanceService;

  // 출근 체크
  @Operation(summary = "출근 체크")
  @PostMapping("/check-in")
  public ResponseEntity<ApiResponse<AttendanceDto.Response>> checkIn(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          HttpServletRequest request) {

    String clientIp = request.getRemoteAddr();

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.checkIn(userDetails.getId(), clientIp)));
  }

  // 퇴근 체크
  @Operation(summary = "퇴근 체크")
  @PostMapping("/check-out")
  public ResponseEntity<ApiResponse<AttendanceDto.Response>> checkOut(
          @AuthenticationPrincipal CustomUserDetails userDetails) {

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.checkOut(userDetails.getId())));
  }

  // 내 근태 월별 조회
  @Operation(summary = "내 근태 월별 조회")
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<List<AttendanceDto.Response>>> getMyAttendance(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam("year") int year,
          @RequestParam("month") int month){

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.getMyAttendance(userDetails.getId(), year, month)));
  }

  // 내 부서 오늘 팀 현황 — 대시보드: 부서원 전체 + 각자 출근/지각/결근
  @Operation(summary = "내 부서 근태 현황 조회 (대시보드)")
  @GetMapping("/department")
  public ResponseEntity<ApiResponse<List<AttendanceDto.DepartmentResponse>>> getMyDepartmentStatus(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {

    return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.getMyDepartmentStatus(userDetails.getId(), date)));
  }

  // 단건 조회
  @Operation(summary = "근태 단건 조회")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AttendanceDto.Response>> findById(
          @PathVariable("id") Long id) {

    return ResponseEntity.ok(ApiResponse.ok(attendanceService.findById(id)));
  }

  // ADMIN 전용 전체 조회
  @Operation(summary = "일자별 전체 근태 조회 (ADMIN 전용)")
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<AttendanceDto.Response>>> getAttendanceByDate(
          @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {
    
    return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAttendanceByDate(date)));
  }
}
