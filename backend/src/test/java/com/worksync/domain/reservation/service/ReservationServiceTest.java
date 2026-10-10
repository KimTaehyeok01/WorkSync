// ReservationService 단위 테스트
package com.worksync.domain.reservation.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.reservation.dto.ReservationDto;
import com.worksync.domain.reservation.entity.Reservation;
import com.worksync.domain.reservation.entity.Resource;
import com.worksync.domain.reservation.entity.ResourceCategory;
import com.worksync.domain.reservation.repository.ReservationRepository;
import com.worksync.domain.reservation.repository.ResourceRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** ReservationService 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ResourceRepository resourceRepository;
	@Mock
	private ReservationRepository reservationRepository;
	@Mock
	private EmployeeRepository employeeRepository;

	@InjectMocks
	private ReservationService reservationService;

	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 12, 10, 0);
	private static final LocalDateTime END = LocalDateTime.of(2026, 10, 12, 11, 0);

	private Employee buildEmployee(Long id, EmployeeRole role) {
		return Employee.builder()
				.id(id)
				.empNo("EMP" + id)
				.name("직원" + id)
				.email("emp" + id + "@worksync.com")
				.password("encoded-password")
				.role(role)
				.build();
	}

	private Resource buildResource(Long id, boolean active) {
		return Resource.builder()
				.id(id).name("3층 대회의실").category(ResourceCategory.MEETING_ROOM).isActive(active)
				.build();
	}

	private ReservationDto.CreateRequest buildCreateRequest(LocalDateTime startAt, LocalDateTime endAt) {
		ReservationDto.CreateRequest request = new ReservationDto.CreateRequest();
		request.setResourceId(1L);
		request.setTitle("주간 회의");
		request.setStartAt(startAt);
		request.setEndAt(endAt);
		return request;
	}

	private Reservation buildReservation(Long id, Resource resource, Employee employee) {
		return Reservation.builder()
				.id(id).resource(resource).employee(employee).title("주간 회의")
				.startAt(START).endAt(END)
				.build();
	}

	@DisplayName("겹치는 예약이 없으면 자원을 락으로 조회한 뒤 예약을 저장한다")
	@Test
	void reserve_success_savesReservation() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER);
		Resource resource = buildResource(1L, true);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(resourceRepository.findByIdForUpdate(1L)).willReturn(Optional.of(resource));
		given(reservationRepository.existsOverlapping(1L, START, END)).willReturn(false);
		given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		ReservationDto.Response result = reservationService.reserve(1L, buildCreateRequest(START, END));

		// then
		assertThat(result.getResourceName()).isEqualTo("3층 대회의실");
		assertThat(result.getEmployeeId()).isEqualTo(1L);
		assertThat(result.getIsCancelable()).isTrue();
		verify(resourceRepository).findByIdForUpdate(1L);
	}

	@DisplayName("같은 자원에 시간이 겹치는 예약이 있으면 RESERVATION_CONFLICT 예외가 발생한다")
	@Test
	void reserve_overlapping_throwsConflict() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(resourceRepository.findByIdForUpdate(1L)).willReturn(Optional.of(buildResource(1L, true)));
		given(reservationRepository.existsOverlapping(1L, START, END)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> reservationService.reserve(1L, buildCreateRequest(START, END)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_CONFLICT);
		verify(reservationRepository, never()).save(any());
	}

	@DisplayName("종료 시각이 시작 시각과 같거나 빠르면 INVALID_RESERVATION_PERIOD 예외가 발생한다")
	@Test
	void reserve_endNotAfterStart_throwsInvalidPeriod() {
		// when & then
		assertThatThrownBy(() -> reservationService.reserve(1L, buildCreateRequest(START, START)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_RESERVATION_PERIOD);
		verifyNoInteractions(resourceRepository, reservationRepository);
	}

	@DisplayName("비활성 자원을 예약하면 RESOURCE_NOT_FOUND 예외가 발생한다")
	@Test
	void reserve_inactiveResource_throwsResourceNotFound() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(resourceRepository.findByIdForUpdate(1L)).willReturn(Optional.of(buildResource(1L, false)));

		// when & then
		assertThatThrownBy(() -> reservationService.reserve(1L, buildCreateRequest(START, END)))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
		verify(reservationRepository, never()).save(any());
	}

	@DisplayName("예약자가 본인 예약을 취소하면 삭제된다")
	@Test
	void cancel_byOwner_deletesReservation() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER);
		Reservation reservation = buildReservation(5L, buildResource(1L, true), me);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(reservationRepository.findById(5L)).willReturn(Optional.of(reservation));

		// when
		reservationService.cancel(1L, 5L);

		// then
		verify(reservationRepository).delete(reservation);
	}

	@DisplayName("예약자가 아닌 일반 사용자가 취소하면 FORBIDDEN 예외가 발생한다")
	@Test
	void cancel_byOtherUser_throwsForbidden() {
		// given
		Employee owner = buildEmployee(1L, EmployeeRole.USER);
		Employee other = buildEmployee(2L, EmployeeRole.USER);
		Reservation reservation = buildReservation(5L, buildResource(1L, true), owner);
		given(employeeRepository.findById(2L)).willReturn(Optional.of(other));
		given(reservationRepository.findById(5L)).willReturn(Optional.of(reservation));

		// when & then
		assertThatThrownBy(() -> reservationService.cancel(2L, 5L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(reservationRepository, never()).delete(any());
	}

	@DisplayName("ADMIN은 타인의 예약도 취소할 수 있다")
	@Test
	void cancel_byAdmin_deletesOthersReservation() {
		// given
		Employee owner = buildEmployee(1L, EmployeeRole.USER);
		Employee admin = buildEmployee(9L, EmployeeRole.ADMIN);
		Reservation reservation = buildReservation(5L, buildResource(1L, true), owner);
		given(employeeRepository.findById(9L)).willReturn(Optional.of(admin));
		given(reservationRepository.findById(5L)).willReturn(Optional.of(reservation));

		// when
		reservationService.cancel(9L, 5L);

		// then
		verify(reservationRepository).delete(reservation);
	}

	@DisplayName("존재하지 않는 예약을 취소하면 RESERVATION_NOT_FOUND 예외가 발생한다")
	@Test
	void cancel_notFound_throwsReservationNotFound() {
		// given
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(1L, EmployeeRole.USER)));
		given(reservationRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> reservationService.cancel(1L, 999L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
	}

	@DisplayName("일자별 예약을 조회하면 해당 일 0시~다음 날 0시 범위로 조회하고, 타인의 예약은 취소 불가로 표시한다")
	@Test
	void getReservations_success_marksCancelableByOwner() {
		// given
		Employee me = buildEmployee(1L, EmployeeRole.USER);
		Employee other = buildEmployee(2L, EmployeeRole.USER);
		Resource resource = buildResource(1L, true);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(me));
		given(reservationRepository.findByPeriod(
				LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 13, 0, 0)))
				.willReturn(List.of(
						buildReservation(1L, resource, me),
						buildReservation(2L, resource, other)));

		// when
		List<ReservationDto.Response> result = reservationService.getReservations(1L, LocalDate.of(2026, 10, 12));

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getIsCancelable()).isTrue();
		assertThat(result.get(1).getIsCancelable()).isFalse();
	}

	@DisplayName("자원을 수정하면 사용 여부를 포함한 값이 변경된다")
	@Test
	void updateResource_success_updatesFields() {
		// given
		Resource resource = buildResource(1L, true);
		ReservationDto.ResourceUpdateRequest request = new ReservationDto.ResourceUpdateRequest();
		request.setId(1L);
		request.setName("4층 소회의실");
		request.setCategory(ResourceCategory.MEETING_ROOM);
		request.setIsActive(false);
		given(resourceRepository.findById(1L)).willReturn(Optional.of(resource));

		// when
		ReservationDto.ResourceResponse result = reservationService.updateResource(request);

		// then
		assertThat(result.getName()).isEqualTo("4층 소회의실");
		assertThat(result.getIsActive()).isFalse();
	}
}
