package com.worksync.domain.auth.service;

import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.auth.dto.AuthDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AttendanceService attendanceService;
    private final AuditLogService auditLogService;

    // Matomo 서버 사이드 이벤트 — url 이 비어 있으면 전송하지 않는다 (GroqService와 같은 RestTemplate 방식)
    @Value("${matomo.url:}")
    private String matomoUrl;

    @Value("${matomo.site-id:1}")
    private int matomoSiteId;

    private final RestTemplate matomoRestTemplate = createMatomoRestTemplate();

    // 감사 로그 카테고리
    private static final String CATEGORY_AUTH = "AUTH";
    private static final String ACTION_LOGIN = "로그인";
    private static final String ACTION_LOGIN_FAIL = "로그인 실패";
    private static final String ACTION_LOGOUT = "로그아웃";

    private static final int MAX_LOGIN_FAIL = 5;
    private static final int LOCK_MINUTES = 30;

    @Transactional(noRollbackFor = CustomException.class)
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request, String clientIp, String userAgent) {
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

        // Matomo 이벤트 — 비동기 전송이라 트랜잭션을 붙잡지 않고, 사용자 식별자·IP는 보내지 않는다
        trackMatomoEvent("auth", "login");

        return AuthDto.LoginResponse.builder()
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

    public Map<String, String> reissue(AuthDto.ReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long employeeId = jwtTokenProvider.getEmployeeId(refreshToken);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // INACTIVE(퇴직) 직원은 토큰을 재발급하지 않는다
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new CustomException(ErrorCode.EMPLOYEE_INACTIVE);
        }

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
    public void logout(AuthDto.ReissueRequest request, String clientIp, String userAgent) {
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        Long employeeId = jwtTokenProvider.getEmployeeId(request.getRefreshToken());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        // 로그아웃 = 오프라인(AWAY). 단, INACTIVE(퇴직) 직원은 상태를 되돌리지 않는다(재로그인 차단 유지)
        if (employee.getStatus() != EmployeeStatus.INACTIVE) {
            employee.changeStatus(EmployeeStatus.AWAY);
        }

        // 로그아웃 = 퇴근 처리
        attendanceService.checkOutOnLogout(employee.getId());

        // 감사 로그 — 로그아웃
        auditLogService.log(employee.getId(), employee.getName(),
                ACTION_LOGOUT, CATEGORY_AUTH, null, clientIp, userAgent);
    }

    // Matomo 이벤트 비동기 전송 — url 미설정 시 no-op, 실패는 로그만 남기고 호출자에게 전파하지 않는다
    private void trackMatomoEvent(String category, String action) {
        if (matomoUrl == null || matomoUrl.isBlank()) {
            return;
        }
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    // 인코딩이 끝난 URI를 그대로 넘겨 이중 인코딩을 막는다 (사용자 식별자·IP는 포함하지 않음)
                    URI uri = UriComponentsBuilder
                            .fromUriString(matomoUrl.trim().replaceAll("/+$", "") + "/matomo.php")
                            .queryParam("idsite", matomoSiteId)
                            .queryParam("rec", 1)
                            .queryParam("apiv", 1)
                            .queryParam("e_c", category)
                            .queryParam("e_a", action)
                            .build()
                            .encode()
                            .toUri();
                    matomoRestTemplate.getForEntity(uri, byte[].class);
                } catch (Exception e) {
                    log.warn("Matomo 이벤트 전송 실패 category={}, action={}, cause={}", category, action, e.toString());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Matomo 이벤트 전송 예약 실패 category={}, action={}, cause={}", category, action, e.toString());
        }
    }

    private static RestTemplate createMatomoRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        return new RestTemplate(factory);
    }
}
