package com.worksync.domain.employee.service;

import com.worksync.domain.department.entity.Department;
import com.worksync.domain.department.repository.DepartmentRepository;
import com.worksync.domain.employee.dto.EmployeeCreateRequest;
import com.worksync.domain.employee.dto.EmployeeResponse;
import com.worksync.domain.employee.dto.EmployeeUpdateRequest;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    // 직원 목록 조회 (이름·부서·상태 필터링)
    public List<EmployeeResponse> getEmployees(String name, Long departmentId, EmployeeStatus status) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (status != null) {
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
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
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

        return EmployeeResponse.from(employeeRepository.save(employee));
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
    public EmployeeResponse updateMyStatus(Long id, EmployeeStatus status) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        employee.changeStatus(status);
        return EmployeeResponse.from(employee);
    }
}
