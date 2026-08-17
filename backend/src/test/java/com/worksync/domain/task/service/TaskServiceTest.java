// TaskService 단위 테스트
package com.worksync.domain.task.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.entity.FileAttachment;
import com.worksync.domain.file.entity.RefType;
import com.worksync.domain.file.repository.FileAttachmentRepository;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.notification.entity.NotificationType;
import com.worksync.domain.notification.service.NotificationService;
import com.worksync.domain.task.dto.TaskDto;
import com.worksync.domain.task.entity.Task;
import com.worksync.domain.task.entity.TaskStatus;
import com.worksync.domain.task.repository.TaskRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** TaskService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TaskService taskService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    private Department buildDepartment(Long id, String name) {
        return Department.builder().id(id).name(name).build();
    }

    private Task buildTask(Long id, Employee creator, Employee assignee, Department department, TaskStatus status) {
        return Task.builder()
                .id(id)
                .creator(creator)
                .assignee(assignee)
                .department(department)
                .title("주간 보고서 작성")
                .description("이번 주 진행사항 정리")
                .status(status)
                .progress(50)
                .build();
    }

    // === create ===

    @DisplayName("정상적으로 업무를 생성하면 담당자에게 알림이 발송되고 감사 로그가 기록된다")
    @Test
    void create_success() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Department department = buildDepartment(1L, "개발팀");

        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");
        request.setDescription("이번 주 진행사항 정리");
        request.setStatus(TaskStatus.TODO);
        request.setAssigneeId(2L);
        request.setDepartmentId(1L);
        request.setProgress(50);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskDto.Response result = taskService.create(creatorId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("주간 보고서 작성");
        assertThat(result.getAssigneeName()).isEqualTo("박영희");
        assertThat(result.getDepartmentName()).isEqualTo("개발팀");

        verify(notificationService).send(eq(2L), eq(NotificationType.TASK), any(String.class), eq("TASK"), isNull());
        verify(auditLogService).log(eq(1L), eq("김철수"), eq("업무 생성"), eq("TASK"), isNull(), isNull(), isNull());
    }

    @DisplayName("생성자가 존재하지 않으면 예외가 발생한다")
    @Test
    void create_creatorNotFound_throwsEmployeeNotFound() {
        // given
        Long creatorId = 1L;
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");

        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.create(creatorId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("담당자가 존재하지 않으면 예외가 발생한다")
    @Test
    void create_assigneeNotFound_throwsEmployeeNotFound() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");
        request.setAssigneeId(2L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(employeeRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.create(creatorId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("부서가 존재하지 않으면 예외가 발생한다")
    @Test
    void create_departmentNotFound_throwsDepartmentNotFound() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");
        request.setDepartmentId(1L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(departmentRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.create(creatorId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @DisplayName("진행률이 10단위가 아니면 예외가 발생한다")
    @Test
    void create_invalidProgress_throwsInvalidProgress() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");
        request.setProgress(55);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

        // when & then
        assertThatThrownBy(() -> taskService.create(creatorId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PROGRESS);
    }

    @DisplayName("담당자를 지정하지 않으면 알림이 발송되지 않는다")
    @Test
    void create_withoutAssignee_noNotificationSent() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskDto.Response result = taskService.create(creatorId, request);

        // then
        assertThat(result.getAssigneeId()).isNull();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(result.getProgress()).isEqualTo(0);
        verifyNoInteractions(notificationService);
    }

    @DisplayName("담당자를 본인으로 지정하면 알림이 발송되지 않는다")
    @Test
    void create_assigneeIsSelf_noNotificationSent() {
        // given
        Long creatorId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setTitle("주간 보고서 작성");
        request.setAssigneeId(1L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskDto.Response result = taskService.create(creatorId, request);

        // then
        assertThat(result.getAssigneeId()).isEqualTo(1L);
        verifyNoInteractions(notificationService);
    }

    // === getById ===

    @DisplayName("업무 상세 조회 시 첨부파일 목록과 함께 반환된다")
    @Test
    void getById_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Department department = buildDepartment(1L, "개발팀");
        Task task = buildTask(10L, creator, assignee, department, TaskStatus.IN_PROGRESS);

        FileAttachment attachment = FileAttachment.builder()
                .id(1L).uploader(creator).originalName("보고서.pdf")
                .filePath("path/to/file").refType(RefType.TASK).refId(10L).build();

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of(attachment));

        // when
        TaskDto.Response result = taskService.getById(10L);

        // then
        assertThat(result.getAssigneeName()).isEqualTo("박영희");
        assertThat(result.getDepartmentName()).isEqualTo("개발팀");
        assertThat(result.getAttachments()).hasSize(1);
        assertThat(result.getAttachments().get(0).getOriginalName()).isEqualTo("보고서.pdf");
    }

    @DisplayName("담당자와 부서가 없는 업무를 조회해도 NPE 없이 반환된다")
    @Test
    void getById_withNullAssigneeAndDepartment_dtoSafe() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of());

        // when
        TaskDto.Response result = taskService.getById(10L);

        // then
        assertThat(result.getAssigneeId()).isNull();
        assertThat(result.getAssigneeName()).isNull();
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getDepartmentName()).isNull();
    }

    @DisplayName("존재하지 않는 업무를 조회하면 예외가 발생한다")
    @Test
    void getById_notFound_throwsTaskNotFound() {
        // given
        given(taskRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.getById(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    // === getAll ===

    @DisplayName("상태와 키워드로 전체 업무 목록을 조회한다")
    @Test
    void getAll_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);

        given(taskRepository.findAllWithFilter(TaskStatus.TODO, "보고서", pageable)).willReturn(page);

        // when
        Page<TaskDto.Response> result = taskService.getAll(TaskStatus.TODO, "보고서", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(task.getTitle());
    }

    // === getByAssignee ===

    @DisplayName("상태 필터가 있으면 필터가 적용된 담당자별 목록을 조회한다")
    @Test
    void getByAssignee_withStatus_delegatesWithStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());
        given(taskRepository.findByAssigneeWithFilter(2L, TaskStatus.DONE, pageable)).willReturn(page);

        // when
        taskService.getByAssignee(2L, TaskStatus.DONE, pageable);

        // then
        verify(taskRepository).findByAssigneeWithFilter(2L, TaskStatus.DONE, pageable);
    }

    @DisplayName("상태 필터가 없으면 전체 상태의 담당자별 목록을 조회한다")
    @Test
    void getByAssignee_withoutStatus_delegatesWithNullStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());
        given(taskRepository.findByAssigneeWithFilter(2L, null, pageable)).willReturn(page);

        // when
        taskService.getByAssignee(2L, null, pageable);

        // then
        verify(taskRepository).findByAssigneeWithFilter(2L, null, pageable);
    }

    // === getByCreator ===

    @DisplayName("생성자 기준으로 업무 목록을 조회한다")
    @Test
    void getByCreator_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        Page<Task> page = new PageImpl<>(List.of(task));

        given(taskRepository.findByCreatorId(1L, pageable)).willReturn(page);

        // when
        Page<TaskDto.Response> result = taskService.getByCreator(1L, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    // === getByDepartment ===

    @DisplayName("부서 기준으로 업무 목록을 조회한다")
    @Test
    void getByDepartment_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());

        given(taskRepository.findByDepartmentWithFilter(1L, TaskStatus.TODO, pageable)).willReturn(page);

        // when
        taskService.getByDepartment(1L, TaskStatus.TODO, pageable);

        // then
        verify(taskRepository).findByDepartmentWithFilter(1L, TaskStatus.TODO, pageable);
    }

    // === update ===

    @DisplayName("생성자가 업무를 수정하면 정상 반영되고 담당자에게 실시간 진행률이 갱신된다")
    @Test
    void update_success() {
        // given
        Long taskId = 10L;
        Long requesterId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(taskId, creator, assignee, null, TaskStatus.TODO);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setProgress(30);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(requesterId)).willReturn(Optional.of(creator));
        given(taskRepository.countByAssigneeIdAndStatus(2L, TaskStatus.TODO)).willReturn(0L);
        given(taskRepository.countByAssigneeIdAndStatus(2L, TaskStatus.IN_PROGRESS)).willReturn(1L);
        given(taskRepository.countByAssigneeIdAndStatus(2L, TaskStatus.DONE)).willReturn(0L);

        // when
        TaskDto.Response result = taskService.update(taskId, requesterId, request);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getProgress()).isEqualTo(30);
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/tasks/status"), anyMap());
        verify(auditLogService).log(eq(1L), eq("김철수"), eq("업무 수정"), eq("TASK"), eq(taskId), isNull(), isNull());
    }

    @DisplayName("존재하지 않는 업무를 수정하면 예외가 발생한다")
    @Test
    void update_notFound_throwsTaskNotFound() {
        // given
        given(taskRepository.findById(999L)).willReturn(Optional.empty());
        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();

        // when & then
        assertThatThrownBy(() -> taskService.update(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @DisplayName("생성자도 담당자도 아닌 사용자가 수정하면 예외가 발생한다")
    @Test
    void update_forbidden_notCreatorNorAssignee_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null, TaskStatus.TODO);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 3L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @DisplayName("담당자가 업무를 수정하면 정상 반영된다")
    @Test
    void update_asAssignee_success() {
        // given
        Long taskId = 10L;
        Long requesterId = 2L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(taskId, creator, assignee, null, TaskStatus.TODO);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setDescription("수정된 설명");

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(requesterId)).willReturn(Optional.of(assignee));

        // when
        TaskDto.Response result = taskService.update(taskId, requesterId, request);

        // then
        assertThat(result.getDescription()).isEqualTo("수정된 설명");
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("수정 시 진행률이 10단위가 아니면 예외가 발생한다")
    @Test
    void update_invalidProgress_throwsInvalidProgress() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setProgress(45);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PROGRESS);
    }

    @DisplayName("수정 시 담당자가 존재하지 않으면 예외가 발생한다")
    @Test
    void update_assigneeNotFound_throwsEmployeeNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setAssigneeId(99L);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("수정 시 부서가 존재하지 않으면 예외가 발생한다")
    @Test
    void update_departmentNotFound_throwsDepartmentNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(departmentRepository.findById(5L)).willReturn(Optional.empty());

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setDepartmentId(5L);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @DisplayName("담당자가 없는 업무는 상태를 변경해도 실시간 알림을 보내지 않는다")
    @Test
    void update_statusChangeWithoutAssignee_noWebsocketNotification() {
        // given
        Long taskId = 10L;
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(taskId, creator, null, null, TaskStatus.TODO);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

        // when
        taskService.update(taskId, 1L, request);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    // === delete ===

    @DisplayName("생성자가 업무를 삭제하면 정상 삭제되고 감사 로그가 기록된다")
    @Test
    void delete_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

        // when
        taskService.delete(10L, 1L);

        // then
        verify(taskRepository).delete(task);
        verify(auditLogService).log(eq(1L), eq("김철수"), eq("업무 삭제"), eq("TASK"), eq(10L), isNull(), isNull());
    }

    @DisplayName("존재하지 않는 업무를 삭제하면 예외가 발생한다")
    @Test
    void delete_notFound_throwsTaskNotFound() {
        // given
        given(taskRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.delete(999L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @DisplayName("생성자도 담당자도 아닌 사용자가 삭제하면 예외가 발생한다")
    @Test
    void delete_forbidden_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> taskService.delete(10L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
