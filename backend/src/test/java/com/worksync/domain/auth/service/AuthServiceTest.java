// AuthService 단위 테스트
package com.worksync.domain.auth.service;

import com.worksync.domain.attendance.service.AttendanceService;
import com.worksync.domain.audit.service.AuditLogService;
import com.worksync.domain.auth.dto.AuthDto;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import com.worksync.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** AuthService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @DisplayName("정상 로그인 시 토큰이 발급되고 상태가 ACTIVE로 전환된다")
    @Test
    void login_success() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd").role(EmployeeRole.USER)
                .status(EmployeeStatus.AWAY)
                .loginFailCount(3)
                .build();

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmpNo("EMP001");
        request.setPassword("plain-pwd");

        given(employeeRepository.findByEmpNo("EMP001")).willReturn(Optional.of(employee));
        given(passwordEncoder.matches("plain-pwd", "encoded-pwd")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(1L, "hong@worksync.com", "USER")).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        // when
        AuthDto.LoginResponse result = authService.login(request, "127.0.0.1", "JUnit");

        // then
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.getLoginFailCount()).isZero();
        verify(attendanceService).checkInOnLogin(1L, "127.0.0.1");
        verify(auditLogService).log(anyLong(), anyString(), eq("로그인"), eq("AUTH"), any(), anyString(), anyString());
    }

    @DisplayName("존재하지 않는 사번으로 로그인하면 예외가 발생한다")
    @Test
    void login_notFound_throwsInvalidCredentials() {
        // given
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmpNo("EMP999");
        request.setPassword("plain-pwd");

        given(employeeRepository.findByEmpNo("EMP999")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @DisplayName("계정이 잠겨 있으면 예외가 발생한다")
    @Test
    void login_locked_throwsAccountLocked() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd")
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmpNo("EMP001");
        request.setPassword("plain-pwd");

        given(employeeRepository.findByEmpNo("EMP001")).willReturn(Optional.of(employee));

        // when & then
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    @DisplayName("퇴직 상태의 사원은 로그인할 수 없다")
    @Test
    void login_inactive_throwsEmployeeInactive() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd")
                .status(EmployeeStatus.INACTIVE)
                .build();

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmpNo("EMP001");
        request.setPassword("plain-pwd");

        given(employeeRepository.findByEmpNo("EMP001")).willReturn(Optional.of(employee));

        // when & then
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_INACTIVE);
    }

    @DisplayName("비밀번호가 틀리면 실패 횟수가 증가하고 예외가 발생한다")
    @Test
    void login_wrongPassword_incrementsFailCountAndThrows() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd")
                .status(EmployeeStatus.ACTIVE)
                .loginFailCount(0)
                .build();

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmpNo("EMP001");
        request.setPassword("wrong-pwd");

        given(employeeRepository.findByEmpNo("EMP001")).willReturn(Optional.of(employee));
        given(passwordEncoder.matches("wrong-pwd", "encoded-pwd")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(employee.getLoginFailCount()).isEqualTo(1);
        verify(auditLogService).log(anyLong(), anyString(), eq("로그인 실패"), eq("AUTH"), any(), anyString(), anyString());
    }

    @DisplayName("유효한 리프레시 토큰으로 재발급하면 새 토큰이 반환된다")
    @Test
    void reissue_success() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd").role(EmployeeRole.USER)
                .build();

        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("valid-refresh");

        given(jwtTokenProvider.validateToken("valid-refresh")).willReturn(true);
        given(jwtTokenProvider.getEmployeeId("valid-refresh")).willReturn(1L);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(jwtTokenProvider.generateAccessToken(1L, "hong@worksync.com", "USER")).willReturn("new-access");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh");

        // when
        Map<String, String> result = authService.reissue(request);

        // then
        assertThat(result.get("accessToken")).isEqualTo("new-access");
        assertThat(result.get("refreshToken")).isEqualTo("new-refresh");
    }

    @DisplayName("유효하지 않은 리프레시 토큰이면 예외가 발생한다")
    @Test
    void reissue_invalidToken_throwsInvalidToken() {
        // given
        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("invalid-refresh");

        given(jwtTokenProvider.validateToken("invalid-refresh")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @DisplayName("리프레시 토큰은 유효하지만 사원이 존재하지 않으면 재발급 시 예외가 발생한다")
    @Test
    void reissue_employeeNotFound_throwsEmployeeNotFound() {
        // given
        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("valid-refresh");

        given(jwtTokenProvider.validateToken("valid-refresh")).willReturn(true);
        given(jwtTokenProvider.getEmployeeId("valid-refresh")).willReturn(999L);
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @DisplayName("로그아웃 시 퇴근 처리되고 상태가 AWAY로 전환된다")
    @Test
    void logout_success() {
        // given
        Employee employee = Employee.builder()
                .id(1L).empNo("EMP001").name("홍길동").email("hong@worksync.com")
                .password("encoded-pwd")
                .status(EmployeeStatus.ACTIVE)
                .build();

        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("valid-refresh");

        given(jwtTokenProvider.validateToken("valid-refresh")).willReturn(true);
        given(jwtTokenProvider.getEmployeeId("valid-refresh")).willReturn(1L);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        authService.logout(request, "127.0.0.1", "JUnit");

        // then
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.AWAY);
        verify(attendanceService).checkOutOnLogout(1L);
        verify(auditLogService).log(anyLong(), anyString(), eq("로그아웃"), eq("AUTH"), any(), anyString(), anyString());
    }

    @DisplayName("유효하지 않은 토큰으로 로그아웃하면 예외가 발생한다")
    @Test
    void logout_invalidToken_throwsInvalidToken() {
        // given
        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("invalid-refresh");

        given(jwtTokenProvider.validateToken("invalid-refresh")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.logout(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(attendanceService, never()).checkOutOnLogout(anyLong());
    }

    @DisplayName("리프레시 토큰은 유효하지만 사원이 존재하지 않으면 로그아웃 시 예외가 발생한다")
    @Test
    void logout_employeeNotFound_throwsEmployeeNotFound() {
        // given
        AuthDto.ReissueRequest request = new AuthDto.ReissueRequest();
        request.setRefreshToken("valid-refresh");

        given(jwtTokenProvider.validateToken("valid-refresh")).willReturn(true);
        given(jwtTokenProvider.getEmployeeId("valid-refresh")).willReturn(999L);
        given(employeeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.logout(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

        verify(attendanceService, never()).checkOutOnLogout(anyLong());
    }
}
