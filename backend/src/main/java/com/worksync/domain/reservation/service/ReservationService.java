// 자원 관리와 자원 예약·취소를 처리하는 서비스
package com.worksync.domain.reservation.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.domain.reservation.dto.ReservationDto;
import com.worksync.domain.reservation.entity.Reservation;
import com.worksync.domain.reservation.entity.Resource;
import com.worksync.domain.reservation.repository.ReservationRepository;
import com.worksync.domain.reservation.repository.ResourceRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 자원 예약 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

	private final ResourceRepository resourceRepository;
	private final ReservationRepository reservationRepository;
	private final EmployeeRepository employeeRepository;

	// 자원 목록 조회 (비활성 자원 포함 — 화면에서 구분 표시)
	public List<ReservationDto.ResourceResponse> getResources() {
		return resourceRepository.findAllByOrderByIdAsc().stream()
				.map(ReservationDto.ResourceResponse::from)
				.toList();
	}

	// 자원 등록 (ADMIN)
	@Transactional
	public ReservationDto.ResourceResponse createResource(ReservationDto.ResourceCreateRequest request) {
		Resource resource = Resource.builder()
				.name(request.getName())
				.category(request.getCategory())
				.location(request.getLocation())
				.capacity(request.getCapacity())
				.description(request.getDescription())
				.build();
		return ReservationDto.ResourceResponse.from(resourceRepository.save(resource));
	}

	// 자원 수정 (ADMIN) — 사용 여부 변경 포함
	@Transactional
	public ReservationDto.ResourceResponse updateResource(ReservationDto.ResourceUpdateRequest request) {
		Resource resource = resourceRepository.findById(request.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		resource.update(
				request.getName(),
				request.getCategory(),
				request.getLocation(),
				request.getCapacity(),
				request.getDescription(),
				request.getIsActive());
		return ReservationDto.ResourceResponse.from(resource);
	}

	// 특정 일자의 전체 예약 조회 (시간표용)
	public List<ReservationDto.Response> getReservations(Long employeeId, LocalDate date) {
		Employee me = findEmployee(employeeId);
		return reservationRepository.findByPeriod(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
				.stream()
				.map(reservation -> ReservationDto.Response.of(reservation, canCancel(reservation, me)))
				.toList();
	}

	// 내 예정 예약 조회
	public List<ReservationDto.Response> getMyReservations(Long employeeId) {
		return reservationRepository.findUpcomingByEmployee(employeeId, LocalDateTime.now())
				.stream()
				.map(reservation -> ReservationDto.Response.of(reservation, true))
				.toList();
	}

	// 예약 — 자원 행을 비관적 락으로 잡아 같은 자원의 동시 예약을 직렬화한 뒤 시간 겹침을 검사한다
	@Transactional
	public ReservationDto.Response reserve(Long employeeId, ReservationDto.CreateRequest request) {
		if (!request.getEndAt().isAfter(request.getStartAt())) {
			throw new CustomException(ErrorCode.INVALID_RESERVATION_PERIOD);
		}
		Employee me = findEmployee(employeeId);

		Resource resource = resourceRepository.findByIdForUpdate(request.getResourceId())
				.filter(Resource::getIsActive)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (reservationRepository.existsOverlapping(resource.getId(), request.getStartAt(), request.getEndAt())) {
			throw new CustomException(ErrorCode.RESERVATION_CONFLICT);
		}

		Reservation reservation = Reservation.builder()
				.resource(resource)
				.employee(me)
				.title(request.getTitle())
				.startAt(request.getStartAt())
				.endAt(request.getEndAt())
				.build();

		return ReservationDto.Response.of(reservationRepository.save(reservation), true);
	}

	// 예약 취소 — 예약자 또는 ADMIN만 가능
	@Transactional
	public void cancel(Long employeeId, Long reservationId) {
		Employee me = findEmployee(employeeId);
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));
		if (!canCancel(reservation, me)) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}
		reservationRepository.delete(reservation);
	}

	private Employee findEmployee(Long employeeId) {
		return employeeRepository.findById(employeeId)
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
	}

	private boolean canCancel(Reservation reservation, Employee me) {
		return reservation.getEmployee().getId().equals(me.getId()) || me.getRole() == EmployeeRole.ADMIN;
	}
}
