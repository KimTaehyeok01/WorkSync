// StompHandler 단위 테스트 — WebSocket CONNECT 시 재직 상태에 따른 연결 허용·거부 검증
package com.worksync.global.config;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/** StompHandler 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private MessageChannel channel;

	@InjectMocks
	private StompHandler stompHandler;

	// 핸들러가 accessor 를 그대로 꺼내 쓰도록 mutable 로 만든 CONNECT 메시지
	private StompHeaderAccessor connectAccessor(String authorization) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorization != null) {
			accessor.addNativeHeader("Authorization", authorization);
		}
		accessor.setLeaveMutable(true);
		return accessor;
	}

	private Message<?> toMessage(StompHeaderAccessor accessor) {
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private Employee buildEmployee(EmployeeStatus status) {
		return Employee.builder()
				.id(1L).empNo("EMP1").name("김철수").email("kim@worksync.com")
				.password("encoded-password").status(status).build();
	}

	@DisplayName("퇴직(INACTIVE) 직원의 CONNECT 는 토큰이 유효해도 거부된다")
	@Test
	void preSend_connectInactiveEmployee_rejectsConnection() {
		// given
		StompHeaderAccessor accessor = connectAccessor("Bearer valid-token");
		Message<?> message = toMessage(accessor);
		given(jwtTokenProvider.validateAccessToken("valid-token")).willReturn(true);
		given(jwtTokenProvider.getEmployeeId("valid-token")).willReturn(1L);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(EmployeeStatus.INACTIVE)));

		// when & then
		assertThatThrownBy(() -> stompHandler.preSend(message, channel))
				.isInstanceOf(MessageDeliveryException.class);
		assertThat(accessor.getUser()).isNull();
	}

	@DisplayName("재직 중인 직원의 CONNECT 는 허용되고 연결에 사용자가 등록된다")
	@Test
	void preSend_connectActiveEmployee_registersUser() {
		// given
		StompHeaderAccessor accessor = connectAccessor("Bearer valid-token");
		Message<?> message = toMessage(accessor);
		given(jwtTokenProvider.validateAccessToken("valid-token")).willReturn(true);
		given(jwtTokenProvider.getEmployeeId("valid-token")).willReturn(1L);
		given(employeeRepository.findById(1L)).willReturn(Optional.of(buildEmployee(EmployeeStatus.ACTIVE)));

		// when
		Message<?> result = stompHandler.preSend(message, channel);

		// then
		assertThat(result).isSameAs(message);
		assertThat(accessor.getUser()).isNotNull();
		assertThat(accessor.getUser().getName()).isEqualTo("1");
	}

	@DisplayName("토큰이 없는 CONNECT 는 기존처럼 통과하지만 사용자는 등록되지 않는다")
	@Test
	void preSend_connectWithoutToken_passesWithoutUser() {
		// given
		StompHeaderAccessor accessor = connectAccessor(null);
		Message<?> message = toMessage(accessor);

		// when
		Message<?> result = stompHandler.preSend(message, channel);

		// then
		assertThat(result).isSameAs(message);
		assertThat(accessor.getUser()).isNull();
		verifyNoInteractions(jwtTokenProvider, employeeRepository);
	}
}
