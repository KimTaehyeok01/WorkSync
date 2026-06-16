package com.worksync.domain.task.service;


import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.dto.FileUploadResponse;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.domain.task.dto.TaskCreateRequest;
import com.worksync.domain.task.dto.TaskResponse;
import com.worksync.domain.task.dto.TaskUpdateRequest;
import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.repository.TaskRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SimpMessagingTemplate messagingTemplate;

    // 감사 로그 카테고리 / 액션명
    private static final String CATEGORY_TASK = "TASK";
    private static final String ACTION_CREATE = "업무 생성";
    private static final String ACTION_UPDATE = "업무 수정";
    private static final String ACTION_DELETE = "업무 삭제";


    // Task => RefType Enum 타입 변경
    RefType refTypeName = RefType.fromTypeName("TASK");

    //업무 생성
    @Transactional
    public TaskResponse create(Long creatorId, TaskCreateRequest request) {
        Employee creator = employeeRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Employee assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = employeeRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        }

        Department department=null;
        if(request.getDepartmentId() !=null){
            department=departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(()->new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        // 진행률 10단위 검증
        if (request.getProgress() !=null && request.getProgress()%10 !=0){
            throw new CustomException(ErrorCode.INVALID_PROGRESS);
        }

        Task task= Task.builder()
            .creator(creator)
            .assignee(assignee)
            .department(department)
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getStatus() !=null ? request.getStatus(): TaskStatus.TODO)
            .progress(request.getProgress() !=null ? request.getProgress() : 0)
            .startDate(request.getStartDate())
            .dueDate(request.getDueDate())
            .build();
        Task saved=taskRepository.save(task);

        //SSE담당자 알림 발송(담당자가 본인이 아닌경우)
        if(assignee !=null && !assignee.getId().equals(creatorId)){
            notificationService.send(
                    assignee.getId(),
                    NotificationType.TASK,
                    creator.getName()+"님이 업무를 등록했습니다:"+request.getTitle(),
                    "TASK",
                    saved.getId()
            );
        }

        // 감사 로그 — 업무 생성
        auditLogService.log(creator.getId(), creator.getName(),
                ACTION_CREATE, CATEGORY_TASK, saved.getId(), null, null);

        return TaskResponse.from(saved);
    }

    //단건 조회(첨부파일 포함)
    public  TaskResponse getById(Long taskId){
        Task task=taskRepository.findById(taskId)
                .orElseThrow(()->new CustomException(ErrorCode.TASK_NOT_FOUND));

        List<FileUploadResponse> attachments=fileAttachmentRepository
                .findByRefTypeAndRefId(refTypeName,taskId)
                .stream()
                .map(FileUploadResponse::from)
                .toList();

        return TaskResponse.from(task,attachments);

    }

    //전체목록(상태 필터+키워드+페이징)
    public Page<TaskResponse>getAll(TaskStatus status, String keyword, Pageable pageable){
            return taskRepository.findAllWithFilter(status,keyword,pageable)
                    .map(TaskResponse::from);
    }

    //담당자별 목록(상태 필터+페이징)
    public Page<TaskResponse>getByAssignee(Long assigneeId,TaskStatus status,Pageable pageable){
        if(status !=null){
            return taskRepository.findByAssigneeWithFilter(assigneeId,status,pageable)
                    .map(TaskResponse::from);
        }
        return taskRepository.findByAssigneeWithFilter(assigneeId,null,pageable)
                .map(TaskResponse::from);
    }

    //내가 만든 업무(페이징)
    public Page<TaskResponse>getByCreator(Long creatorId,Pageable pageable){
        return taskRepository.findByCreatorId(creatorId,pageable)
                .map(TaskResponse::from);
    }

    //부서별 목록
    public Page<TaskResponse>getByDepartment(Long departmentId,TaskStatus status,Pageable pageable){
        return taskRepository.findByDepartmentWithFilter(departmentId,status,pageable)
                .map(TaskResponse::from);
    }


    //업무 수정
    @Transactional
    public TaskResponse update(Long taskId, Long requesterId, TaskUpdateRequest request){
        Task task=taskRepository.findById(taskId)
                .orElseThrow(()->new CustomException(ErrorCode.TASK_NOT_FOUND));

        checkEditPermission(task,requesterId);

        //진행률 10단위 검증
        if (request.getProgress() !=null && request.getProgress() %10 !=0){
            throw new CustomException(ErrorCode.INVALID_PROGRESS);
        }

        Employee assignee=null;
        if(request.getAssigneeId()!=null){
            assignee=employeeRepository.findById(request.getAssigneeId())
                    .orElseThrow(()->new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        }
        Department department =null;
        if(request.getDepartmentId() !=null){
            department=departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(()->new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        task.update(request.getTitle(),
                request.getDescription(),
                assignee,
                department,
                request.getStatus(),
                request.getProgress(),
                request.getStartDate(),
                request.getDueDate()
        );

        List<FileUploadResponse> attachments=fileAttachmentRepository
                .findByRefTypeAndRefId(refTypeName,taskId)
                .stream()
                .map(FileUploadResponse::from)
                .toList();

        // 감사 로그 — 업무 수정
        String actorName = employeeRepository.findById(requesterId)
                .map(Employee::getName).orElse(null);
        auditLogService.log(requesterId, actorName, ACTION_UPDATE, CATEGORY_TASK, taskId, null, null);

        // 업무 상태 변경 시 담당자 대쉬보드 진행률 실시간 갱신 - websocket
        if (request.getStatus() != null && task.getAssignee() != null) {
            Long assigneeId = task.getAssignee().getId();
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(assigneeId),
                    "/queue/tasks/status",
                    Map.of(
                            "todoTaskCount", taskRepository.countByAssigneeIdAndStatus(assigneeId, TaskStatus.TODO),
                            "inProgressTaskCount", taskRepository.countByAssigneeIdAndStatus(assigneeId, TaskStatus.IN_PROGRESS),
                            "doneTaskCount", taskRepository.countByAssigneeIdAndStatus(assigneeId, TaskStatus.DONE)
                    )
            );
        }

        return TaskResponse.from(task,attachments);
    }

    //업무 삭제
    @Transactional
    public  void delete(Long taskId,Long requesterId){
        Task task=taskRepository.findById(taskId)
                .orElseThrow(()->new CustomException(ErrorCode.TASK_NOT_FOUND));
        checkEditPermission(task,requesterId);

        // 감사 로그 — 업무 삭제 (삭제 전 actor 이름 확보)
        String actorName = employeeRepository.findById(requesterId)
                .map(Employee::getName).orElse(null);
        auditLogService.log(requesterId, actorName, ACTION_DELETE, CATEGORY_TASK, taskId, null, null);

        taskRepository.delete(task);
    }

    //권한 체크
    private void checkEditPermission(Task task,Long requesterId){
        boolean isCreator=task.getCreator().getId().equals(requesterId);
        boolean isAssignee=task.getAssignee() !=null
                && task.getAssignee().getId().equals(requesterId);

        if(!isCreator && !isAssignee){
            throw  new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
