// JwtAuthenticationFilter 단위 테스트 — 재직 상태에 따른 인증 등록 여부 검증
package com.worksync.global.security;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** JwtAuthenticationFilter 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private CustomUserDetailsService customUserDetailsService;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private Employee buildEmployee(EmployeeStatus status) {
		return Employee.builder()
				.id(1L).empNo("EMP1").name("김철수").email("kim@worksync.com")
				.password("encoded-password").status(status).build();
	}

	@DisplayName("퇴직(INACTIVE) 직원의 토큰이면 유효해도 SecurityContext에 인증이 등록되지 않는다")
	@Test
	void doFilter_inactiveEmployee_doesNotAuthenticate() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);

		given(jwtTokenProvider.validateAccessToken("valid-token")).willReturn(true);
		given(jwtTokenProvider.getEmployeeId("valid-token")).willReturn(1L);
		given(customUserDetailsService.loadUserById(1L))
				.willReturn(new CustomUserDetails(buildEmployee(EmployeeStatus.INACTIVE)));

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then — 인증은 등록되지 않지만 필터 체인은 계속 진행된다(이후 인증 없음 → 401)
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@DisplayName("재직 중인 직원의 토큰이면 SecurityContext에 인증이 등록된다")
	@Test
	void doFilter_activeEmployee_authenticates() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);

		CustomUserDetails principal = new CustomUserDetails(buildEmployee(EmployeeStatus.ACTIVE));
		given(jwtTokenProvider.validateAccessToken("valid-token")).willReturn(true);
		given(jwtTokenProvider.getEmployeeId("valid-token")).willReturn(1L);
		given(customUserDetailsService.loadUserById(1L)).willReturn(principal);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
		verify(filterChain).doFilter(request, response);
	}

	@DisplayName("토큰이 없으면 인증을 등록하지 않고 필터 체인을 계속 진행한다")
	@Test
	void doFilter_noToken_doesNotAuthenticate() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(filterChain).doFilter(request, response);
	}
}
