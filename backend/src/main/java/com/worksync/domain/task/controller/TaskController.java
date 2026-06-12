package com.worksync.domain.task.controller;


import com.worksync.domain.task.dto.TaskCreateRequest;
import com.worksync.domain.task.dto.TaskResponse;
import com.worksync.domain.task.dto.TaskUpdateRequest;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.service.TaskService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    //업무 생성
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create (
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user){
        return ResponseEntity.status(201)
                .body(ApiResponse.created(taskService.create(user.getId(),request)));
    }

    //단건 조회(첨부파일 포함)
    @GetMapping("/{taskId}")
    public  ResponseEntity<ApiResponse<TaskResponse>>getById(@PathVariable Long taskId){
        return ResponseEntity.ok(ApiResponse.ok(taskService.getById(taskId)));
    }

    //전체 목록(상태 필터+키워드 검색+페이징)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getAll(
            @RequestParam(required = false)TaskStatus status,
            @RequestParam(required = false)String keyword,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size){
        Pageable pageable= PageRequest.of(page,size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.getAll(status,keyword,pageable)));
    }

    //내가 만든 업무
    @GetMapping("/my")
    public  ResponseEntity<ApiResponse<Page<TaskResponse>>>getMy(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size){
        Pageable pageable=PageRequest.of(page,size,Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.getByCreator(user.getId(),pageable)));
    }

    //담당자별 목록
    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>>getByAssignee(
            @PathVariable Long assigneeId,
            @RequestParam(required = false)TaskStatus status,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size){
        Pageable pageable=PageRequest.of(page,size,Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.getByAssignee(assigneeId,status,pageable)));
    }

    //부서별 목록
    @GetMapping("/department/{departmentId}")
    public  ResponseEntity<ApiResponse<Page<TaskResponse>>> getByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(required = false)TaskStatus status,
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size){
        Pageable pageable=PageRequest.of(page,size,Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.getByDepartment(departmentId,status,pageable)));
    }


    //업무 수정(작성자 또는 담당자만)

    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user){
        return ResponseEntity.ok(ApiResponse.ok(taskService.update(taskId,user.getId(),request)));
    }

    //업무 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long taskId,
         @AuthenticationPrincipal CustomUserDetails user){
        taskService.delete(taskId,user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
