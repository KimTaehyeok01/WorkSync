// EmployeeService 단위 테스트
package com.worksync.domain.employee.service;

import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.dto.EmployeeDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.entity.JobGrade;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** EmployeeService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee buildEmployee(Long id, String name) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .build();
    }

    @SuppressWarnings("unchecked")
    @DisplayName("이름/부서/상태 조건으로 직원 목록을 조회하면 매핑된 응답 목록을 반환한다")
    @Test
    void getEmployees_success_returnsMappedList() {
        // given
        Employee employee1 = buildEmployee(1L, "김철수");
        Employee employee2 = buildEmployee(2L, "박영희");
        given(employeeRepository.findAll(any(Specification.class))).willReturn(List.of(employee1, employee2));

        // when
        List<EmployeeDto.Response> result = employeeService.getEmployees("김", 1L, EmployeeStatus.ACTIVE);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("김철수");
        assertThat(result.get(1).getName()).isEqualTo("박영희");
    }

    @DisplayName("직원 ID로 조회하면 정상적으로 응답을 반환한다")
    @Test
    void getEmployee_success() {
        // given
        Department department = Department.builder().id(1L).name("개발팀").build();
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP1").name("김철수").email("kim1@worksync.com")
                .password("encoded-password").department(department).build();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        EmployeeDto.Response result = employeeService.getEmployee(1L);

        // then
        assertThat(result.getName()).isEqualTo("김철수");
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        assertThat(result.getDepartmentName()).isEqualTo("개발팀");
    }

    @DisplayName("존재하지 않는 직원 ID로 조회하면 예외가 발생한다")
    @Test
    void getEmployee_notFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.getEmployee(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("연관 부서가 없는 직원을 조회해도 NPE 없이 응답한다")
    @Test
    void getEmployee_withNullDepartment_dtoSafe() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        EmployeeDto.Response result = employeeService.getEmployee(1L);

        // then
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getDepartmentName()).isNull();
    }

    @DisplayName("본인 정보를 조회하면 정상적으로 응답을 반환한다")
    @Test
    void getMyInfo_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        EmployeeDto.Response result = employeeService.getMyInfo(1L);

        // then
        assertThat(result.getName()).isEqualTo("김철수");
    }

    @DisplayName("존재하지 않는 ID로 본인 정보를 조회하면 예외가 발생한다")
    @Test
    void getMyInfo_notFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.getMyInfo(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("부서와 권한을 지정해 직원을 등록하면 정상 저장되고 입사 감사 로그가 기록된다")
    @Test
    void createEmployee_success_withDepartmentAndExplicitRole() {
        // given
        Long actorId = 99L;
        Employee actor = buildEmployee(99L, "관리자");
        Department department = Department.builder().id(1L).name("개발팀").build();

        EmployeeDto.CreateRequest request = new EmployeeDto.CreateRequest();
        request.setEmpNo("EMP100");
        request.setName("홍길동");
        request.setEmail("hong@worksync.com");
        request.setPassword("plain-password");
        request.setPhone("010-1234-5678");
        request.setJobGrade(JobGrade.STAFF);
        request.setRole(EmployeeRole.ADMIN);
        request.setDepartmentId(1L);
        request.setHireDate(LocalDate.of(2026, 1, 1));

        given(employeeRepository.existsByEmail("hong@worksync.com")).willReturn(false);
        given(employeeRepository.existsByEmpNo("EMP100")).willReturn(false);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(passwordEncoder.encode("plain-password")).willReturn("encoded-pw");
        given(employeeRepository.save(any(Employee.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(employeeRepository.findById(actorId)).willReturn(Optional.of(actor));

        // when
        EmployeeDto.Response result = employeeService.createEmployee(request, actorId);

        // then
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getRole()).isEqualTo(EmployeeRole.ADMIN);
        assertThat(result.getDepartmentId()).isEqualTo(1L);
        verify(auditLogService).log(eq(actorId), eq("관리자"), eq("입사 등록"), eq("HR"), isNull(), isNull(), isNull());
    }

    @DisplayName("부서와 권한을 지정하지 않고 등록하면 부서는 null, 권한은 USER로 기본 설정된다")
    @Test
    void createEmployee_success_withoutDepartment_roleDefaultsToUser() {
        // given
        Long actorId = 99L;
        Employee actor = buildEmployee(99L, "관리자");

        EmployeeDto.CreateRequest request = new EmployeeDto.CreateRequest();
        request.setEmpNo("EMP101");
        request.setName("이몽룡");
        request.setEmail("lee@worksync.com");
        request.setPassword("plain-password");
        request.setJobGrade(JobGrade.STAFF);

        given(employeeRepository.existsByEmail("lee@worksync.com")).willReturn(false);
        given(employeeRepository.existsByEmpNo("EMP101")).willReturn(false);
        given(passwordEncoder.encode("plain-password")).willReturn("encoded-pw");
        given(employeeRepository.save(any(Employee.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(employeeRepository.findById(actorId)).willReturn(Optional.of(actor));

        // when
        EmployeeDto.Response result = employeeService.createEmployee(request, actorId);

        // then
        assertThat(result.getRole()).isEqualTo(EmployeeRole.USER);
        assertThat(result.getDepartmentId()).isNull();
        verifyNoInteractions(departmentRepository);
    }

    @DisplayName("이미 사용 중인 이메일이면 예외가 발생한다")
    @Test
    void createEmployee_duplicateEmail_throwsDuplicateEmail() {
        // given
        EmployeeDto.CreateRequest request = new EmployeeDto.CreateRequest();
        request.setEmpNo("EMP102");
        request.setEmail("dup@worksync.com");
        request.setPassword("plain-password");
        request.setJobGrade(JobGrade.STAFF);

        given(employeeRepository.existsByEmail("dup@worksync.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> employeeService.createEmployee(request, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(employeeRepository, never()).save(any());
    }

    @DisplayName("이미 사용 중인 사번이면 예외가 발생한다")
    @Test
    void createEmployee_duplicateEmpNo_throwsDuplicateEmpNo() {
        // given
        EmployeeDto.CreateRequest request = new EmployeeDto.CreateRequest();
        request.setEmpNo("EMP103");
        request.setEmail("new@worksync.com");
        request.setPassword("plain-password");
        request.setJobGrade(JobGrade.STAFF);

        given(employeeRepository.existsByEmail("new@worksync.com")).willReturn(false);
        given(employeeRepository.existsByEmpNo("EMP103")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> employeeService.createEmployee(request, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMP_NO);
        verify(employeeRepository, never()).save(any());
    }

    @DisplayName("존재하지 않는 부서로 등록하면 예외가 발생한다")
    @Test
    void createEmployee_departmentNotFound_throwsDepartmentNotFound() {
        // given
        EmployeeDto.CreateRequest request = new EmployeeDto.CreateRequest();
        request.setEmpNo("EMP104");
        request.setEmail("kim2@worksync.com");
        request.setPassword("plain-password");
        request.setJobGrade(JobGrade.STAFF);
        request.setDepartmentId(999L);

        given(employeeRepository.existsByEmail("kim2@worksync.com")).willReturn(false);
        given(employeeRepository.existsByEmpNo("EMP104")).willReturn(false);
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.createEmployee(request, 99L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
        verify(employeeRepository, never()).save(any());
    }

    @DisplayName("직원 정보를 수정하면 필드가 반영되고 비밀번호가 암호화된다")
    @Test
    void updateEmployee_success_updatesFieldsAndEncodesPassword() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        Department newDepartment = Department.builder().id(2L).name("영업팀").build();

        EmployeeDto.UpdateRequest request = new EmployeeDto.UpdateRequest();
        request.setName("김철수2");
        request.setPhone("010-9999-8888");
        request.setPassword("new-password");
        request.setJobGrade(JobGrade.MANAGER);
        request.setDepartmentId(2L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(departmentRepository.findById(2L)).willReturn(Optional.of(newDepartment));
        given(passwordEncoder.encode("new-password")).willReturn("encoded-new-password");

        // when
        EmployeeDto.Response result = employeeService.updateEmployee(1L, request);

        // then
        assertThat(result.getName()).isEqualTo("김철수2");
        assertThat(result.getDepartmentId()).isEqualTo(2L);
        assertThat(employee.getPassword()).isEqualTo("encoded-new-password");
        verify(passwordEncoder).encode("new-password");
    }

    @DisplayName("비밀번호와 부서를 지정하지 않으면 기존 값이 유지된다")
    @Test
    void updateEmployee_withoutPasswordAndDepartmentId_keepsExistingDepartmentAndPassword() {
        // given
        Department originalDepartment = Department.builder().id(5L).name("총무팀").build();
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP1").name("김철수").email("kim1@worksync.com")
                .password("original-encoded-password").department(originalDepartment).build();

        EmployeeDto.UpdateRequest request = new EmployeeDto.UpdateRequest();
        request.setName("김철수2");

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        EmployeeDto.Response result = employeeService.updateEmployee(1L, request);

        // then
        assertThat(result.getDepartmentId()).isEqualTo(5L);
        assertThat(employee.getPassword()).isEqualTo("original-encoded-password");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(departmentRepository);
    }

    @DisplayName("프로필 이미지를 지정하지 않고 수정하면 기존 이미지가 삭제된다")
    @Test
    void updateEmployee_nullProfileImage_clearsExistingImage() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP1").name("김철수").email("kim1@worksync.com")
                .password("encoded-password").profileImage("old-image.png").build();

        EmployeeDto.UpdateRequest request = new EmployeeDto.UpdateRequest();
        request.setName("김철수");

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        employeeService.updateEmployee(1L, request);

        // then
        assertThat(employee.getProfileImage()).isNull();
    }

    @DisplayName("존재하지 않는 직원을 수정하면 예외가 발생한다")
    @Test
    void updateEmployee_notFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());
        EmployeeDto.UpdateRequest request = new EmployeeDto.UpdateRequest();

        // when & then
        assertThatThrownBy(() -> employeeService.updateEmployee(999L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 부서로 수정하면 예외가 발생한다")
    @Test
    void updateEmployee_departmentNotFound_throwsDepartmentNotFound() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        EmployeeDto.UpdateRequest request = new EmployeeDto.UpdateRequest();
        request.setDepartmentId(999L);

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(departmentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @DisplayName("직원을 삭제하면 저장소에서 제거된다")
    @Test
    void deleteEmployee_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        employeeService.deleteEmployee(1L);

        // then
        verify(employeeRepository).delete(employee);
    }

    @DisplayName("존재하지 않는 직원을 삭제하면 예외가 발생한다")
    @Test
    void deleteEmployee_notFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.deleteEmployee(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @DisplayName("퇴사(INACTIVE) 상태로 변경하면 상태 전파와 함께 감사 로그가 기록된다")
    @Test
    void updateMyStatus_toInactive_logsAuditAndBroadcastsStatus() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        Employee actor = buildEmployee(2L, "박부장");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(employeeRepository.findById(2L)).willReturn(Optional.of(actor));

        // when
        EmployeeDto.Response result = employeeService.updateMyStatus(1L, EmployeeStatus.INACTIVE, 2L);

        // then
        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/status"), eq(Map.of("employeeId", 1L, "status", EmployeeStatus.INACTIVE)));
        verify(auditLogService).log(eq(2L), eq("박부장"), eq("퇴사 처리"), eq("HR"), eq(1L), isNull(), isNull());
    }

    @DisplayName("INACTIVE 이외의 상태로 변경하면 감사 로그를 기록하지 않는다")
    @Test
    void updateMyStatus_toActive_doesNotLogAudit() {
        // given
        Employee employee = buildEmployee(1L, "김철수");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        EmployeeDto.Response result = employeeService.updateMyStatus(1L, EmployeeStatus.ACTIVE, 2L);

        // then
        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/status"), eq(Map.of("employeeId", 1L, "status", EmployeeStatus.ACTIVE)));
        verifyNoInteractions(auditLogService);
    }

    @DisplayName("존재하지 않는 직원의 상태를 변경하면 예외가 발생한다")
    @Test
    void updateMyStatus_notFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> employeeService.updateMyStatus(999L, EmployeeStatus.INACTIVE, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verifyNoInteractions(messagingTemplate, auditLogService);
    }
}
