package com.worksync.domain.leave.controller;


import com.worksync.domain.leave.dto.LeaveBalanceResponse;
import com.worksync.domain.leave.dto.LeaveCreateRequest;
import com.worksync.domain.leave.dto.LeaveResponse;
import com.worksync.domain.leave.service.LeaveService;
import com.worksync.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave`")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;

    //ToDO:auth 브랜치 머지후 교체
    private static final Long TEMP_USER_ID=1L;

    //휴가신청
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveResponse>> request(
            @Valid@RequestBody LeaveCreateRequest request){
        return ResponseEntity.status(201)
                .body(ApiResponse.created(leaveService.request(TEMP_USER_ID,request)));
    }

    //연차 잔여 조회
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getBalance(){
        return ResponseEntity.ok(ApiResponse.ok(leaveService.getBalance(TEMP_USER_ID)));
    }


}
