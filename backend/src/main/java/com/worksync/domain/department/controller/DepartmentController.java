package com.worksync.domain.department.controller;

import com.worksync.domain.department.dto.DepartmentRequest;
import com.worksync.domain.department.dto.DepartmentResponse;
import com.worksync.domain.department.service.DepartmentService;
import com.worksync.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //json형태로 응답반환
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;

  @GetMapping
  // 전체 부서목록 조회
  public ApiResponse<List<DepartmentResponse>> findAll(){
    return ApiResponse.ok(departmentService.findDept());
  }

  @GetMapping("/{id}")
  // 단건 부서 조회
  public ApiResponse<DepartmentResponse> findById(@PathVariable Long id){
    return ApiResponse.ok(departmentService.findByDept(id));
  }

  // 부서생성
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<DepartmentResponse> createDept(@Valid @RequestBody DepartmentRequest request){
    return ApiResponse.created(departmentService.createDept(request));
  }

  // 부서명 수정
  @PutMapping("/{id}")
  public ApiResponse<DepartmentResponse> updateDept(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request){
    return ApiResponse.ok(departmentService.updateDept(id,request));
  }

  // 부서 삭제
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id){
    departmentService.deleteDept(id);
    return ApiResponse.ok(null);
  }
}
