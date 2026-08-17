// AttendanceService 단위 테스트
package com.worksync.domain.attendance.service;

import com.worksync.domain.attendance.dto.AttendanceDto;
import com.worksync.domain.attendance.entity.Attendance;
import com.worksync.domain.attendance.entity.AttendanceStatus;
import com.worksync.domain.attendance.repository.AttendanceRepository;
import com.worksync.domain.department.entity.Department;
import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** AttendanceService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee buildEmployee(Long id, String name, Department department) {
        return Employee.builder()
                .id(id)
                .empNo("EMP" + id)
                .name(name)
                .email(name + id + "@worksync.com")
                .password("encoded-password")
                .department(department)
                .build();
    }

    private Department buildDepartment(Long id, String name) {
        return Department.builder().id(id).name(name).build();
    }

    // === checkIn ===

    @DisplayName("정상적으로 출근 체크하면 근태 기록이 생성된다 (부서 미배정 시 알림 없음)")
    @Test
    void checkIn_success_noDepartment() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        AttendanceStatus expectedStatus =
                LocalDateTime.now().getHour() >= 9 ? AttendanceStatus.LATE : AttendanceStatus.NORMAL;

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());
        given(attendanceRepository.save(any(Attendance.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AttendanceDto.Response result = attendanceService.checkIn(1L, "127.0.0.1");

        // then
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        assertThat(result.getEmployeeName()).isEqualTo("김철수");
        assertThat(result.getWorkDate()).isEqualTo(today);
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("부서가 배정된 직원이 출근 체크하면 부서원에게 웹소켓 알림을 보낸다")
    @Test
    void checkIn_withDepartment_broadcastsWebSocket() {
        // given
        Department department = buildDepartment(5L, "개발팀");
        Employee employee = buildEmployee(1L, "김철수", department);
        LocalDate today = LocalDate.now();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());
        given(attendanceRepository.save(any(Attendance.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        attendanceService.checkIn(1L, "127.0.0.1");

        // then
        verify(messagingTemplate).convertAndSend(
                "/topic/attendance/5",
                Map.of("employeeId", 1L, "status", "CHECK_IN"));
    }

    @DisplayName("존재하지 않는 사원이 출근 체크하면 예외가 발생한다")
    @Test
    void checkIn_employeeNotFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.checkIn(1L, "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verifyNoInteractions(attendanceRepository, messagingTemplate);
    }

    @DisplayName("이미 출근한 상태에서 다시 출근 체크하면 예외가 발생한다")
    @Test
    void checkIn_alreadyCheckedIn_throwsAlreadyCheckedIn() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        Attendance existing = Attendance.builder().id(10L).employee(employee).workDate(today).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> attendanceService.checkIn(1L, "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CHECKED_IN);
        verify(attendanceRepository, never()).save(any());
    }

    // === checkOut ===

    @DisplayName("정상적으로 퇴근 체크하면 퇴근 시각이 기록된다 (부서 미배정 시 알림 없음)")
    @Test
    void checkOut_success_noDepartment() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(8))
                .status(AttendanceStatus.NORMAL)
                .build();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when
        AttendanceDto.Response result = attendanceService.checkOut(1L);

        // then
        assertThat(result.getCheckOutTime()).isNotNull();
        assertThat(attendance.getCheckOutTime()).isNotNull();
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("부서가 배정된 직원이 퇴근 체크하면 부서원에게 웹소켓 알림을 보낸다")
    @Test
    void checkOut_withDepartment_broadcastsWebSocket() {
        // given
        Department department = buildDepartment(5L, "개발팀");
        Employee employee = buildEmployee(1L, "김철수", department);
        LocalDate today = LocalDate.now();
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(8))
                .status(AttendanceStatus.NORMAL)
                .build();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when
        attendanceService.checkOut(1L);

        // then
        verify(messagingTemplate).convertAndSend(
                "/topic/attendance/5",
                Map.of("employeeId", 1L, "status", "CHECK_OUT"));
    }

    @DisplayName("오늘 출근 기록이 없으면 퇴근 체크 시 예외가 발생한다")
    @Test
    void checkOut_notFound_throwsAttendanceNotFound() {
        // given
        LocalDate today = LocalDate.now();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.checkOut(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
    }

    @DisplayName("이미 퇴근한 상태에서 다시 퇴근 체크하면 예외가 발생한다")
    @Test
    void checkOut_alreadyCheckedOut_throwsAlreadyCheckedOut() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(8))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .status(AttendanceStatus.NORMAL)
                .build();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when & then
        assertThatThrownBy(() -> attendanceService.checkOut(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CHECKED_OUT);
    }

    // === checkInOnLogin ===

    @DisplayName("로그인 시 이미 오늘 출근 기록이 있으면 아무 처리도 하지 않는다")
    @Test
    void checkInOnLogin_alreadyCheckedIn_noop() {
        // given
        LocalDate today = LocalDate.now();
        Attendance existing = Attendance.builder().id(10L).workDate(today).build();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(existing));

        // when
        attendanceService.checkInOnLogin(1L, "127.0.0.1");

        // then
        verifyNoInteractions(employeeRepository, messagingTemplate);
        verify(attendanceRepository, never()).save(any());
    }

    @DisplayName("로그인 시 오늘 출근 기록이 없고 부서가 배정되어 있으면 출근 기록을 생성하고 알림을 보낸다")
    @Test
    void checkInOnLogin_success_withDepartment() {
        // given
        Department department = buildDepartment(5L, "개발팀");
        Employee employee = buildEmployee(1L, "김철수", department);
        LocalDate today = LocalDate.now();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        attendanceService.checkInOnLogin(1L, "127.0.0.1");

        // then
        verify(attendanceRepository).save(any(Attendance.class));
        verify(messagingTemplate).convertAndSend(
                "/topic/attendance/5",
                Map.of("employeeId", 1L, "status", "CHECK_IN"));
    }

    @DisplayName("로그인 시 부서 미배정 직원은 출근 기록만 생성하고 알림은 보내지 않는다")
    @Test
    void checkInOnLogin_success_noDepartment() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();

        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));

        // when
        attendanceService.checkInOnLogin(1L, "127.0.0.1");

        // then
        verify(attendanceRepository).save(any(Attendance.class));
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("로그인 연동 출근 처리 시 존재하지 않는 사원이면 예외가 발생한다")
    @Test
    void checkInOnLogin_employeeNotFound_throwsEmployeeNotFound() {
        // given
        LocalDate today = LocalDate.now();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());
        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.checkInOnLogin(1L, "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        verify(attendanceRepository, never()).save(any());
    }

    // === checkOutOnLogout ===

    @DisplayName("로그아웃 시 오늘 출근 기록이 없으면 아무 처리도 하지 않는다")
    @Test
    void checkOutOnLogout_noAttendanceToday_noop() {
        // given
        LocalDate today = LocalDate.now();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.empty());

        // when
        attendanceService.checkOutOnLogout(1L);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("로그아웃 시 이미 퇴근 처리된 기록이면 갱신하지 않는다")
    @Test
    void checkOutOnLogout_alreadyCheckedOut_noop() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        LocalDateTime existingCheckOut = LocalDateTime.now().minusHours(1);
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(9))
                .checkOutTime(existingCheckOut)
                .status(AttendanceStatus.NORMAL)
                .build();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when
        attendanceService.checkOutOnLogout(1L);

        // then
        assertThat(attendance.getCheckOutTime()).isEqualTo(existingCheckOut);
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("로그아웃 시 미퇴근 상태이고 부서가 배정되어 있으면 퇴근 처리 후 알림을 보낸다")
    @Test
    void checkOutOnLogout_success_withDepartment() {
        // given
        Department department = buildDepartment(5L, "개발팀");
        Employee employee = buildEmployee(1L, "김철수", department);
        LocalDate today = LocalDate.now();
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(9))
                .status(AttendanceStatus.NORMAL)
                .build();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when
        attendanceService.checkOutOnLogout(1L);

        // then
        assertThat(attendance.getCheckOutTime()).isNotNull();
        verify(messagingTemplate).convertAndSend(
                "/topic/attendance/5",
                Map.of("employeeId", 1L, "status", "CHECK_OUT"));
    }

    @DisplayName("로그아웃 시 부서 미배정 직원은 퇴근 처리만 하고 알림은 보내지 않는다")
    @Test
    void checkOutOnLogout_success_noDepartment() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate today = LocalDate.now();
        Attendance attendance = Attendance.builder()
                .id(10L).employee(employee).workDate(today)
                .checkInTime(LocalDateTime.now().minusHours(9))
                .status(AttendanceStatus.NORMAL)
                .build();
        given(attendanceRepository.findByEmployeeIdAndWorkDate(1L, today)).willReturn(Optional.of(attendance));

        // when
        attendanceService.checkOutOnLogout(1L);

        // then
        assertThat(attendance.getCheckOutTime()).isNotNull();
        verifyNoInteractions(messagingTemplate);
    }

    // === getMyAttendance ===

    @DisplayName("해당 월의 내 근태 기록을 조회한다")
    @Test
    void getMyAttendance_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        Attendance a1 = Attendance.builder().id(1L).employee(employee)
                .workDate(LocalDate.of(2026, 8, 3)).status(AttendanceStatus.NORMAL).build();
        Attendance a2 = Attendance.builder().id(2L).employee(employee)
                .workDate(LocalDate.of(2026, 8, 4)).status(AttendanceStatus.LATE).build();
        given(attendanceRepository.findByEmployeeIdAndWorkDateBetween(
                1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .willReturn(List.of(a1, a2));

        // when
        List<AttendanceDto.Response> result = attendanceService.getMyAttendance(1L, 2026, 8);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1).getStatus()).isEqualTo(AttendanceStatus.LATE);
    }

    // === getAttendanceByDate ===

    @DisplayName("ADMIN이 특정 날짜의 전체 근태 기록을 조회한다")
    @Test
    void getAttendanceByDate_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        LocalDate date = LocalDate.of(2026, 8, 10);
        Attendance attendance = Attendance.builder().id(1L).employee(employee)
                .workDate(date).status(AttendanceStatus.NORMAL).build();
        given(attendanceRepository.findByWorkDate(date)).willReturn(List.of(attendance));

        // when
        List<AttendanceDto.Response> result = attendanceService.getAttendanceByDate(date);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo(1L);
    }

    // === getMyDepartmentStatus ===

    @DisplayName("부서 미배정 직원은 팀 현황 조회 시 빈 목록을 반환한다")
    @Test
    void getMyDepartmentStatus_noDepartment_returnsEmptyList() {
        // given
        Employee me = buildEmployee(1L, "김철수", null);
        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));

        // when
        List<AttendanceDto.DepartmentResponse> result =
                attendanceService.getMyDepartmentStatus(1L, LocalDate.now());

        // then
        assertThat(result).isEmpty();
        verify(attendanceRepository, never()).findEmployeesByDepartment(anyLong());
    }

    @DisplayName("부서원 중 출근 기록이 없는 직원은 결근(ABSENT)으로 표시된다")
    @Test
    void getMyDepartmentStatus_withDepartment_marksMissingRecordAsAbsent() {
        // given
        Department department = buildDepartment(5L, "개발팀");
        Employee me = buildEmployee(1L, "김철수", department);
        Employee member2 = buildEmployee(2L, "박영희", department);
        LocalDate date = LocalDate.now();
        Attendance attendanceForMe = Attendance.builder().id(1L).employee(me).workDate(date)
                .status(AttendanceStatus.NORMAL).checkInTime(LocalDateTime.now()).build();

        given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
        given(attendanceRepository.findEmployeesByDepartment(5L)).willReturn(List.of(me, member2));
        given(attendanceRepository.findByDepartmentAndWorkDate(5L, date)).willReturn(List.of(attendanceForMe));

        // when
        List<AttendanceDto.DepartmentResponse> result = attendanceService.getMyDepartmentStatus(1L, date);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(AttendanceStatus.NORMAL);
        assertThat(result.get(1).getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.get(1).getCheckInTime()).isNull();
    }

    @DisplayName("팀 현황 조회 시 존재하지 않는 사원이면 예외가 발생한다")
    @Test
    void getMyDepartmentStatus_employeeNotFound_throwsEmployeeNotFound() {
        // given
        given(employeeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.getMyDepartmentStatus(1L, LocalDate.now()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    // === findById ===

    @DisplayName("근태 기록 단건 조회 - 정상")
    @Test
    void findById_success() {
        // given
        Employee employee = buildEmployee(1L, "김철수", null);
        Attendance attendance = Attendance.builder().id(1L).employee(employee)
                .workDate(LocalDate.now()).status(AttendanceStatus.NORMAL).build();
        given(attendanceRepository.findById(1L)).willReturn(Optional.of(attendance));

        // when
        AttendanceDto.Response result = attendanceService.findById(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmployeeName()).isEqualTo("김철수");
    }

    @DisplayName("존재하지 않는 근태 기록을 조회하면 예외가 발생한다")
    @Test
    void findById_notFound_throwsAttendanceNotFound() {
        // given
        given(attendanceRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> attendanceService.findById(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
    }
}
