package com.worksync.domain.task.controller;


import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.task.dto.TaskCreateRequest;
import com.worksync.domain.task.dto.TaskResponse;
import com.worksync.domain.task.dto.TaskUpdateRequest;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.service.TaskService;
import com.worksync.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    // TODO: auth 브랜치 머지 후 @AuthenticationPrincipal CustomUserDetails 로 교체
    private static final Long TEMP_USER_ID = 1L;

    private final TaskService taskService;
    //업무 생성
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create (
            @Valid @RequestBody TaskCreateRequest request){
        return ResponseEntity.status(201)
                .body(ApiResponse.created(taskService.create(TEMP_USER_ID,request)));
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
            @RequestParam(defaultValue = "0")int page,
            @RequestParam(defaultValue = "10")int size){
        Pageable pageable=PageRequest.of(page,size,Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(taskService.getByCreator(TEMP_USER_ID,pageable)));
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

    //담당자 후보 목록(부서/직급 필터)
    @GetMapping("/candidates")
    public  ResponseEntity<ApiResponse<List<Map<String,Object>>>>getCandidates(
            @RequestParam(required = false)Long departmentId,
            @RequestParam(required = false)JobGrade jobGrade){
                List<Map<String,Object>> result=taskService.getCandidates(departmentId,jobGrade)
                        .stream()
                        .map(e->Map.<String,Object>of(
                                "id",e.getId(),
                                "name",e.getName(),
                                "jobgrade",e.getJobGrade().name(),
                                "departmentId",e.getDepartment() !=null ? e.getDepartment().getId():"",
                                "departmentName",e.getDepartment() !=null ? e.getDepartment().getName():""
                        ))
                        .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.ok(result));
    }

    //업무 수정(작성자 또는 담당자만)
    @Transactional
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request){
        return ResponseEntity.ok(ApiResponse.ok(taskService.update(taskId,TEMP_USER_ID,request)));
    }

    //업무 삭제
    @Transactional
    @DeleteMapping("/{taskId}")
    public  ResponseEntity<Void>delete(@PathVariable Long taskId){
        taskService.delete(taskId,TEMP_USER_ID);
        return ResponseEntity.noContent().build();
    }
}
