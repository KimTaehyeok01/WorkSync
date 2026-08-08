package com.worksync.domain.employee.controller;

import com.worksync.domain.employee.dto.EmployeeDto;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.service.EmployeeService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Employee", description = "직원 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // 직원 목록 조회 — 이름·부서·상태로 필터링 (전체 사용자)
    @Operation(summary = "직원 목록 조회 (이름·부서·상태 필터)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeDto.Response>>> getEmployees(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "status", required = false) EmployeeStatus status) {

        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.getEmployees(name, departmentId, status)));
    }

    // 직원 단건 조회 — ID로 특정 사원 정보 반환 (전체 사용자)
    @Operation(summary = "직원 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployee(id)));
    }

    // 내 정보 조회 — 로그인한 본인 정보 반환
    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.getMyInfo(userDetails.getId())));
    }

    // 직원 등록 — 사원 계정 생성, 이메일·사번 중복 검사 포함 (관리자 전용)
    @Operation(summary = "직원 등록 (ADMIN 전용)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> createEmployee(
            @RequestBody @Valid EmployeeDto.CreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.status(201).body(ApiResponse.created(
                employeeService.createEmployee(request, userDetails.getId())));
    }

    // 직원 정보 수정 — 이름·직급·부서·상태 등 변경 (관리자 전용)
    @Operation(summary = "직원 정보 수정 (ADMIN 전용)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> updateEmployee(
            @PathVariable Long id,
            @RequestBody @Valid EmployeeDto.UpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(employeeService.updateEmployee(id, request)));
    }

    // 직원 삭제 — 사원 레코드 영구 삭제 (관리자 전용)
    @Operation(summary = "직원 삭제 (ADMIN 전용)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 직원 상태 변경 — 특정 사원을 ACTIVE / AWAY / INACTIVE로 변경 (관리자 전용)
    @Operation(summary = "직원 상태 변경 (ADMIN 전용)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> updateEmployeeStatus(
            @PathVariable Long id,
            @RequestParam("status") EmployeeStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.updateMyStatus(id, status, userDetails.getId())));
    }

    // 내 상태 변경 — 로그인한 본인의 상태를 ACTIVE / AWAY로 전환 (INACTIVE는 ADMIN 전용)
    @Operation(summary = "내 상태 변경 (ACTIVE/AWAY)")
    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<EmployeeDto.Response>> updateMyStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("status") EmployeeStatus status) {

        if (status == EmployeeStatus.INACTIVE) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.updateMyStatus(userDetails.getId(), status, userDetails.getId())));
    }
}
