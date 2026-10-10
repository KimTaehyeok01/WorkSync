// TaskService 단위 테스트
package com.worksync.domain.task.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
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
import com.worksync.global.security.CurrentEmployeeProvider;
import com.worksync.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

    private TaskService taskService;

    // 권한 규칙(TaskAccessPolicy)과 로그인 직원 조회(CurrentEmployeeProvider)는 mock 이 아닌 실제 객체로 검증한다
    // 기본 로그인 사용자는 대부분의 테스트에서 업무 생성자로 쓰는 1번 직원
    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, employeeRepository, departmentRepository,
                fileAttachmentRepository, notificationService, auditLogService, messagingTemplate,
                new CurrentEmployeeProvider(employeeRepository), new TaskAccessPolicy());
        loginAs(buildEmployee(1L, "김철수"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Employee employee) {
        CustomUserDetails principal = new CustomUserDetails(employee);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    private Employee buildEmployee(Long id, String name, EmployeeRole role, Department department) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .role(role)
                .department(department)
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
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
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
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
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

    @DisplayName("작성자·담당자가 아니고 부서도 다른 직원이 조회하면 존재하지 않는 업무로 응답한다")
    @Test
    void getById_unrelatedEmployee_throwsTaskNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee outsider = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀"));
        loginAs(outsider);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(outsider));

        // when & then
        assertThatThrownBy(() -> taskService.getById(10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
        verifyNoInteractions(fileAttachmentRepository);
    }

    @DisplayName("부서가 없는 직원은 부서가 없는 남의 업무를 조회할 수 없다")
    @Test
    void getById_bothWithoutDepartment_throwsTaskNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        Employee outsider = buildEmployee(3L, "이민수", EmployeeRole.USER, null);
        loginAs(outsider);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(outsider));

        // when & then
        assertThatThrownBy(() -> taskService.getById(10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @DisplayName("업무에 지정된 부서의 소속 직원은 작성자·담당자가 아니어도 조회할 수 있다")
    @Test
    void getById_sameDepartmentEmployee_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Department department = buildDepartment(1L, "개발팀");
        Task task = buildTask(10L, creator, null, department, TaskStatus.TODO);
        Employee colleague = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(1L, "개발팀"));
        loginAs(colleague);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(colleague));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of());

        // when
        TaskDto.Response result = taskService.getById(10L);

        // then
        assertThat(result.getId()).isEqualTo(10L);
    }

    @DisplayName("담당자는 부서가 달라도 업무를 조회할 수 있다")
    @Test
    void getById_asAssignee_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희", EmployeeRole.USER, buildDepartment(2L, "영업팀"));
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        loginAs(assignee);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of());

        // when
        TaskDto.Response result = taskService.getById(10L);

        // then
        assertThat(result.getId()).isEqualTo(10L);
    }

    @DisplayName("관리자는 관련 없는 업무도 조회할 수 있다")
    @Test
    void getById_admin_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        loginAs(admin);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, 10L))
                .willReturn(List.of());

        // when
        TaskDto.Response result = taskService.getById(10L);

        // then
        assertThat(result.getId()).isEqualTo(10L);
    }

    @DisplayName("인증 정보 없이 업무를 조회하면 예외가 발생한다")
    @Test
    void getById_withoutAuthentication_throwsInvalidToken() {
        // given
        SecurityContextHolder.clearContext();
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));

        // when & then
        assertThatThrownBy(() -> taskService.getById(10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    // === getAll ===

    @DisplayName("상태와 키워드로 전체 업무 목록을 조회하면 내 부서와 직원 ID 로 조회 범위가 제한된다")
    @Test
    void getAll_success() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee me = buildEmployee(1L, "김철수", EmployeeRole.USER, buildDepartment(7L, "개발팀"));
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(taskRepository.findAllWithFilter(TaskStatus.TODO, "보고서", false, 7L, 1L, pageable)).willReturn(page);

        // when
        Page<TaskDto.Response> result = taskService.getAll(TaskStatus.TODO, "보고서", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo(task.getTitle());
    }

    @DisplayName("관리자가 전체 업무 목록을 조회하면 관리자 여부가 전달된다")
    @Test
    void getAll_admin_passesAdminFlag() {
        // given
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, buildDepartment(7L, "개발팀"));
        loginAs(admin);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());

        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        given(taskRepository.findAllWithFilter(null, null, true, 7L, 9L, pageable)).willReturn(page);

        // when
        taskService.getAll(null, null, pageable);

        // then
        verify(taskRepository).findAllWithFilter(null, null, true, 7L, 9L, pageable);
    }

    @DisplayName("부서가 없는 직원이 전체 업무 목록을 조회하면 부서 조건 없이 본인 ID 만 전달된다")
    @Test
    void getAll_withoutDepartment_passesNullDepartment() {
        // given
        Employee me = buildEmployee(1L, "김철수", EmployeeRole.USER, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());

        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(taskRepository.findAllWithFilter(null, null, false, null, 1L, pageable)).willReturn(page);

        // when
        taskService.getAll(null, null, pageable);

        // then
        verify(taskRepository).findAllWithFilter(null, null, false, null, 1L, pageable);
    }

    @DisplayName("인증 정보 없이 전체 업무 목록을 조회하면 예외가 발생한다")
    @Test
    void getAll_withoutAuthentication_throwsInvalidToken() {
        // given
        SecurityContextHolder.clearContext();
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> taskService.getAll(null, null, pageable))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(taskRepository);
    }

    // === getByAssignee ===

    @DisplayName("상태 필터가 있으면 필터가 적용된 담당자별 목록을 조회한다")
    @Test
    void getByAssignee_withStatus_delegatesWithStatus() {
        // given
        Employee me = buildEmployee(1L, "김철수", EmployeeRole.USER, buildDepartment(7L, "개발팀"));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(taskRepository.findByAssigneeWithFilter(2L, TaskStatus.DONE, false, 7L, 1L, pageable)).willReturn(page);

        // when
        taskService.getByAssignee(2L, TaskStatus.DONE, pageable);

        // then
        verify(taskRepository).findByAssigneeWithFilter(2L, TaskStatus.DONE, false, 7L, 1L, pageable);
    }

    @DisplayName("상태 필터가 없으면 전체 상태의 담당자별 목록을 조회한다")
    @Test
    void getByAssignee_withoutStatus_delegatesWithNullStatus() {
        // given
        Employee me = buildEmployee(1L, "김철수", EmployeeRole.USER, buildDepartment(7L, "개발팀"));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(taskRepository.findByAssigneeWithFilter(2L, null, false, 7L, 1L, pageable)).willReturn(page);

        // when
        taskService.getByAssignee(2L, null, pageable);

        // then
        verify(taskRepository).findByAssigneeWithFilter(2L, null, false, 7L, 1L, pageable);
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

    @DisplayName("부서 기준으로 업무 목록을 조회하면 조회할 부서와 내 부서가 구분되어 전달된다")
    @Test
    void getByDepartment_success() {
        // given
        Employee me = buildEmployee(1L, "김철수", EmployeeRole.USER, buildDepartment(7L, "개발팀"));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of());

        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(taskRepository.findByDepartmentWithFilter(5L, TaskStatus.TODO, false, 7L, 1L, pageable)).willReturn(page);

        // when
        taskService.getByDepartment(5L, TaskStatus.TODO, pageable);

        // then
        verify(taskRepository).findByDepartmentWithFilter(5L, TaskStatus.TODO, false, 7L, 1L, pageable);
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

    @DisplayName("작성자·담당자가 아니고 부서도 다른 직원이 수정하면 존재하지 않는 업무로 응답한다")
    @Test
    void update_unrelatedEmployee_throwsTaskNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee outsider = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀"));
        loginAs(outsider);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(outsider));
        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 3L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("같은 부서 동료는 업무를 볼 수 있어도 수정할 수는 없다")
    @Test
    void update_sameDepartmentColleague_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee colleague = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(1L, "개발팀"));
        loginAs(colleague);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(colleague));
        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 3L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("관리자라도 작성자가 아니면 업무를 수정할 수 없다")
    @Test
    void update_asAdmin_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        loginAs(admin);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 9L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("담당자가 상태만 변경하면 정상 반영되고 담당자·부서는 그대로 유지된다")
    @Test
    void update_asAssignee_statusOnly_success() {
        // given
        Long taskId = 10L;
        Long requesterId = 2L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Department department = buildDepartment(1L, "개발팀");
        Task task = buildTask(taskId, creator, assignee, department, TaskStatus.TODO);
        loginAs(assignee);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(requesterId)).willReturn(Optional.of(assignee));

        // when
        TaskDto.Response result = taskService.update(taskId, requesterId, request);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getTitle()).isEqualTo("주간 보고서 작성");
        assertThat(result.getAssigneeId()).isEqualTo(2L);
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/tasks/status"), anyMap());
    }

    @DisplayName("담당자가 진행률만 변경하면 정상 반영된다")
    @Test
    void update_asAssignee_progressOnly_success() {
        // given
        Long taskId = 10L;
        Long requesterId = 2L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(taskId, creator, assignee, null, TaskStatus.TODO);
        loginAs(assignee);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setProgress(80);

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(requesterId)).willReturn(Optional.of(assignee));

        // when
        TaskDto.Response result = taskService.update(taskId, requesterId, request);

        // then
        assertThat(result.getProgress()).isEqualTo(80);
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("담당자가 제목을 함께 바꾸려 하면 예외가 발생하고 업무는 변경되지 않는다")
    @Test
    void update_asAssignee_withTitle_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null, TaskStatus.TODO);
        loginAs(assignee);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setTitle("제목 변경");

        // when & then
        assertThatThrownBy(() -> taskService.update(10L, 2L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(task.getTitle()).isEqualTo("주간 보고서 작성");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("담당자가 내용·담당자·부서·기간 중 하나라도 바꾸려 하면 예외가 발생한다")
    @Test
    void update_asAssignee_withRestrictedField_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null, TaskStatus.TODO);
        loginAs(assignee);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));

        Map<String, Consumer<TaskDto.UpdateRequest>> restrictedFields = Map.of(
                "description", request -> request.setDescription("수정된 설명"),
                "assigneeId", request -> request.setAssigneeId(3L),
                "departmentId", request -> request.setDepartmentId(1L),
                "startDate", request -> request.setStartDate(LocalDate.of(2026, 8, 1)),
                "dueDate", request -> request.setDueDate(LocalDate.of(2026, 8, 15)));

        restrictedFields.forEach((field, setField) -> {
            TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
            request.setStatus(TaskStatus.IN_PROGRESS);
            setField.accept(request);

            // when & then
            assertThatThrownBy(() -> taskService.update(10L, 2L, request))
                    .as(field)
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        });
        verify(employeeRepository, never()).findById(3L);
        verifyNoInteractions(departmentRepository, auditLogService);
    }

    @DisplayName("생성자는 제목·내용·담당자·부서·기간을 모두 수정할 수 있다")
    @Test
    void update_asCreator_allFields_success() {
        // given
        Long taskId = 10L;
        Long requesterId = 1L;
        Employee creator = buildEmployee(1L, "김철수");
        Employee newAssignee = buildEmployee(3L, "이민수");
        Department department = buildDepartment(1L, "개발팀");
        Task task = buildTask(taskId, creator, null, null, TaskStatus.TODO);

        TaskDto.UpdateRequest request = new TaskDto.UpdateRequest();
        request.setTitle("월간 보고서 작성");
        request.setDescription("수정된 설명");
        request.setAssigneeId(3L);
        request.setDepartmentId(1L);
        request.setStartDate(LocalDate.of(2026, 8, 1));
        request.setDueDate(LocalDate.of(2026, 8, 15));

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(newAssignee));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(fileAttachmentRepository.findByRefTypeAndRefId(RefType.TASK, taskId)).willReturn(List.of());
        given(employeeRepository.findById(requesterId)).willReturn(Optional.of(creator));

        // when
        TaskDto.Response result = taskService.update(taskId, requesterId, request);

        // then
        assertThat(result.getTitle()).isEqualTo("월간 보고서 작성");
        assertThat(result.getDescription()).isEqualTo("수정된 설명");
        assertThat(result.getAssigneeId()).isEqualTo(3L);
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @DisplayName("수정 시 진행률이 10단위가 아니면 예외가 발생한다")
    @Test
    void update_invalidProgress_throwsInvalidProgress() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, null, TaskStatus.TODO);
        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));

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
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
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
        given(employeeRepository.findById(1L)).willReturn(Optional.of(creator));
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

    @DisplayName("작성자·담당자가 아니고 부서도 다른 직원이 삭제하면 존재하지 않는 업무로 응답한다")
    @Test
    void delete_unrelatedEmployee_throwsTaskNotFound() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee outsider = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(2L, "영업팀"));
        loginAs(outsider);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(outsider));

        // when & then
        assertThatThrownBy(() -> taskService.delete(10L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("담당자가 업무를 삭제하면 예외가 발생하고 업무는 삭제되지 않는다")
    @Test
    void delete_asAssignee_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Employee assignee = buildEmployee(2L, "박영희");
        Task task = buildTask(10L, creator, assignee, null, TaskStatus.TODO);
        loginAs(assignee);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(assignee));

        // when & then
        assertThatThrownBy(() -> taskService.delete(10L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("같은 부서 동료는 업무를 볼 수 있어도 삭제할 수는 없다")
    @Test
    void delete_sameDepartmentColleague_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee colleague = buildEmployee(3L, "이민수", EmployeeRole.USER, buildDepartment(1L, "개발팀"));
        loginAs(colleague);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(3L)).willReturn(Optional.of(colleague));

        // when & then
        assertThatThrownBy(() -> taskService.delete(10L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("관리자라도 작성자가 아니면 업무를 삭제할 수 없다")
    @Test
    void delete_asAdmin_throwsForbidden() {
        // given
        Employee creator = buildEmployee(1L, "김철수");
        Task task = buildTask(10L, creator, null, buildDepartment(1L, "개발팀"), TaskStatus.TODO);
        Employee admin = buildEmployee(9L, "관리자", EmployeeRole.ADMIN, null);
        loginAs(admin);

        given(taskRepository.findById(10L)).willReturn(Optional.of(task));
        given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> taskService.delete(10L, 9L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(taskRepository, never()).delete(any(Task.class));
        verifyNoInteractions(auditLogService);
    }
}
