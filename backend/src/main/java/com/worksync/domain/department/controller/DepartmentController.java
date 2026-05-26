package com.worksync.domain.department.controller;

import com.worksync.domain.department.dto.DepartmentRequest;
import com.worksync.domain.department.dto.DepartmentResponse;
import com.worksync.domain.department.service.DepartmentService;
import com.worksync.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;

  // 전체 부서목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<DepartmentResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.findDept()));
  }

  // 단건 부서 조회
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DepartmentResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.findByDept(id)));
  }

  // 부서 생성 (ADMIN 전용)
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<DepartmentResponse>> createDept(@Valid @RequestBody DepartmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(departmentService.createDept(request)));
  }

  // 부서명 수정 (ADMIN 전용)
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<DepartmentResponse>> updateDept(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.updateDept(id, request)));
  }

  // 부서 삭제 (ADMIN 전용)
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    departmentService.deleteDept(id);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
