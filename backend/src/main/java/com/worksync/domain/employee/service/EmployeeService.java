package com.worksync.domain.employee.service;

import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.dto.EmployeeCreateRequest;
import com.worksync.domain.employee.dto.EmployeeResponse;
import com.worksync.domain.employee.dto.EmployeeUpdateRequest;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.file.service.FileService;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogService auditLogService;

    // 감사 로그 카테고리 / 액션명
    private static final String CATEGORY_HR = "HR";
    private static final String ACTION_HIRE = "입사 등록";
    private static final String ACTION_RESIGN = "퇴사 처리";

    // 직원 목록 조회 (이름·부서·상태 필터링)
    public List<EmployeeResponse> getEmployees(String name, Long departmentId, EmployeeStatus status) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(name != null && !name.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if(departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if(status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return employeeRepository.findAll(spec).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    // 직원 단건 조회 (관리자 + 일반 사용자)
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return EmployeeResponse.from(employee);
    }

    // 내 정보 조회 (본인)
    public EmployeeResponse getMyInfo(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return EmployeeResponse.from(employee);
    }

    // 직원 등록 (ADMIN)
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request, Long actorId) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (employeeRepository.existsByEmpNo(request.getEmpNo())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMP_NO);
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        Employee employee = Employee.builder()
                .empNo(request.getEmpNo())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .jobGrade(request.getJobGrade())
                .role(request.getRole() != null ? request.getRole() : EmployeeRole.USER)
                .department(department)
                .profileImage(request.getProfileImage())
                .hireDate(request.getHireDate())
                .build();

        Employee saved = employeeRepository.save(employee);

        // 감사 로그 — 입사 등록 (actor=등록 관리자, target=신규 직원)
        String actorName = employeeRepository.findById(actorId)
                .map(Employee::getName).orElse(null);
        auditLogService.log(actorId, actorName, ACTION_HIRE, CATEGORY_HR, saved.getId(), null, null);

        return EmployeeResponse.from(saved);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        String encodedPassword = request.getPassword() != null
                ? passwordEncoder.encode(request.getPassword())
                : null;

        employee.update(
                request.getName(),
                request.getPhone(),
                request.getJobGrade(),
                request.getRole(),
                department,
                request.getEmail(),
                request.getHireDate(),
                request.getProfileImage(),
                encodedPassword
        );

        return EmployeeResponse.from(employee);
    }

    // 직원 삭제 (ADMIN)
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        employeeRepository.delete(employee);
    }

    @Transactional
    public EmployeeResponse updateMyStatus(Long id, EmployeeStatus status, Long actorId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        employee.changeStatus(status);

        // 상태 변경을 사용자에게 실시간 전파 (WebSocket)
        messagingTemplate.convertAndSend(
                "/topic/status",
                Map.of("employeeId", employee.getId(), "status", status));

        // 감사 로그 — 퇴사 처리 (INACTIVE 전환 시에만 기록)
        if (status == EmployeeStatus.INACTIVE) {
            String actorName = employeeRepository.findById(actorId)
                    .map(Employee::getName).orElse(null);
            auditLogService.log(actorId, actorName, ACTION_RESIGN, CATEGORY_HR, employee.getId(), null, null);
        }

        return EmployeeResponse.from(employee);
    }
}
