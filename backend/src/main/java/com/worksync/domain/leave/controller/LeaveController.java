package com.worksync.domain.leave.controller;


import com.worksync.domain.leave.dto.LeaveDto;
import com.worksync.domain.leave.service.LeaveService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Leave", description = "휴가 API")
@Slf4j
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;

    //휴가신청
    @Operation(summary = "휴가 신청")
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveDto.Response>> request(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid@RequestBody LeaveDto.CreateRequest request){

        return ResponseEntity.status(201)
                .body(ApiResponse.created(leaveService.request(userDetails.getId(),request)));
    }

    //연차 잔여 조회
    @Operation(summary = "연차 잔여 조회")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LeaveDto.BalanceResponse>> getBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long employeeId) { 

        // employeeId가 있으면 그 사람 잔여일, 없으면 본인 잔여일
        Long targetId = (employeeId != null) ? employeeId : userDetails.getId();

        return ResponseEntity.ok(ApiResponse.ok(leaveService.getBalance(targetId)));
    }

    //내 휴가 신청 내역 조회
    @Operation(summary = "내 휴가 신청 내역 조회 (연도별)")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LeaveDto.Response>>> getMyRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("year") int year) {

        return ResponseEntity.ok(ApiResponse.ok(leaveService.getMyRequests(userDetails.getId(), year)));
    }

    //내 부서 승인 휴가 조회 (캘린더 표시용)
    @Operation(summary = "내 부서 승인 휴가 조회 (캘린더 표시용)")
    @GetMapping("/department")
    public ResponseEntity<ApiResponse<List<LeaveDto.CalendarResponse>>> getDepartmentLeaves(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ApiResponse.ok(
                leaveService.getDepartmentLeaves(userDetails.getId(), from, to)));
    }
}
