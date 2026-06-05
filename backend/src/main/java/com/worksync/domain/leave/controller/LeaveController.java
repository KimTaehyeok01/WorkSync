package com.worksync.domain.leave.controller;


import com.worksync.domain.leave.dto.LeaveBalanceResponse;
import com.worksync.domain.leave.dto.LeaveCreateRequest;
import com.worksync.domain.leave.dto.LeaveResponse;
import com.worksync.domain.leave.service.LeaveService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;

    //휴가신청
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveResponse>> request(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid@RequestBody LeaveCreateRequest request){

        return ResponseEntity.status(201)
                .body(ApiResponse.created(leaveService.request(userDetails.getId(),request)));
    }

    //연차 잔여 조회
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails){

        return ResponseEntity.ok(ApiResponse.ok(leaveService.getBalance(userDetails.getId())));
    }
}
