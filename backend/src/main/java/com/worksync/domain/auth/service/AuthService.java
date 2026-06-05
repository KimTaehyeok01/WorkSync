package com.worksync.domain.auth.service;

import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.auth.dto.LoginRequest;
import com.worksync.domain.auth.dto.LoginResponse;
import com.worksync.domain.auth.dto.ReissueRequest;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AttendanceService attendanceService;
    private final AuditLogService auditLogService;

    // 감사 로그 카테고리
    private static final String CATEGORY_AUTH = "AUTH";
    private static final String ACTION_LOGIN = "로그인";
    private static final String ACTION_LOGIN_FAIL = "로그인 실패";
    private static final String ACTION_LOGOUT = "로그아웃";

    private static final int MAX_LOGIN_FAIL = 5;
    private static final int LOCK_MINUTES = 30;

    @Transactional(noRollbackFor = CustomException.class)
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        Employee employee = employeeRepository.findByEmpNo(request.getEmpNo())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 계정 잠금 확인
        if (employee.getLockedUntil() != null &&
                employee.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 재직 상태 확인 — INACTIVE는 로그인 차단
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new CustomException(ErrorCode.EMPLOYEE_INACTIVE);
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            employee.incrementLoginFailCount(MAX_LOGIN_FAIL, LOCK_MINUTES);
            // 감사 로그 — 로그인 실패
            auditLogService.log(employee.getId(), employee.getName(),
                    ACTION_LOGIN_FAIL, CATEGORY_AUTH, null, clientIp, userAgent);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 로그인 성공 — 실패 횟수 초기화 및 상태 변경
        employee.resetLoginFailCount();
        // 상태 변경
        employee.changeStatus(EmployeeStatus.ACTIVE);

        // 로그인 = 출근 처리
        attendanceService.checkInOnLogin(employee.getId(), clientIp);

        // 감사 로그 — 로그인 성공
        auditLogService.log(employee.getId(), employee.getName(),
                ACTION_LOGIN, CATEGORY_AUTH, null, clientIp, userAgent);

        String accessToken = jwtTokenProvider.generateAccessToken(
                employee.getId(), employee.getEmail(), employee.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(employee.getId());

        return LoginResponse.builder()
                .employeeId(employee.getId())
                .empNo(employee.getEmpNo())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .status(employee.getStatus())
                .departmentName(employee.getDepartment() != null ?
                        employee.getDepartment().getName() : null)
                .profileImage(employee.getProfileImage())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Map<String, String> reissue(ReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long employeeId = jwtTokenProvider.getEmployeeId(refreshToken);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                employee.getId(), employee.getEmail(), employee.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(employee.getId());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    // 로그아웃
    @Transactional
    public void logout(ReissueRequest request, String clientIp, String userAgent) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        Long employeeId = jwtTokenProvider.getEmployeeId(request.getRefreshToken());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        // 로그아웃 = 오프라인(AWAY). INACTIVE(퇴직)로 만들면 재로그인이 차단됨
        employee.changeStatus(EmployeeStatus.AWAY);

        // 로그아웃 = 퇴근 처리
        attendanceService.checkOutOnLogout(employee.getId());

        // 감사 로그 — 로그아웃
        auditLogService.log(employee.getId(), employee.getName(),
                ACTION_LOGOUT, CATEGORY_AUTH, null, clientIp, userAgent);
    }
}
