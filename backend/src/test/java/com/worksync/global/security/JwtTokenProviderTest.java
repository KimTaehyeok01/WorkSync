// JwtTokenProvider 단위 테스트 — 토큰 종류(access/refresh) 구분 검증
package com.worksync.global.security;

import com.worksync.global.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** JwtTokenProvider 단위 테스트. */
class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-key-for-jwt-token-provider-unit-test";

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		JwtConfig jwtConfig = new JwtConfig();
		jwtConfig.setSecret(SECRET);
		jwtConfig.setIssuer("worksync-test");
		jwtTokenProvider = new JwtTokenProvider(jwtConfig);
	}

	@DisplayName("access 토큰은 access 검증만 통과하고 refresh 검증은 통과하지 못한다")
	@Test
	void accessToken_passesOnlyAccessValidation() {
		// given
		String accessToken = jwtTokenProvider.generateAccessToken(1L, "kim@worksync.com", "USER");

		// when & then
		assertThat(jwtTokenProvider.validateAccessToken(accessToken)).isTrue();
		assertThat(jwtTokenProvider.validateRefreshToken(accessToken)).isFalse();
		assertThat(jwtTokenProvider.getEmployeeId(accessToken)).isEqualTo(1L);
	}

	@DisplayName("refresh 토큰은 refresh 검증만 통과하고 access 검증은 통과하지 못한다")
	@Test
	void refreshToken_passesOnlyRefreshValidation() {
		// given
		String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

		// when & then
		assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
		assertThat(jwtTokenProvider.validateAccessToken(refreshToken)).isFalse();
	}

	@DisplayName("종류 클레임이 없는 토큰(구버전 발급분)은 서명이 유효해도 어느 검증도 통과하지 못한다")
	@Test
	void tokenWithoutType_failsBothValidations() {
		// given
		String legacyToken = Jwts.builder()
				.subject("1")
				.expiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.compact();

		// when & then
		assertThat(jwtTokenProvider.validateAccessToken(legacyToken)).isFalse();
		assertThat(jwtTokenProvider.validateRefreshToken(legacyToken)).isFalse();
	}

	@DisplayName("만료된 토큰과 형식이 잘못된 토큰은 검증을 통과하지 못한다")
	@Test
	void expiredOrMalformedToken_failsValidation() {
		// given
		JwtConfig expiredConfig = new JwtConfig();
		expiredConfig.setSecret(SECRET);
		expiredConfig.setIssuer("worksync-test");
		expiredConfig.setAccessTokenExpMinutes(-1);
		String expiredToken = new JwtTokenProvider(expiredConfig)
				.generateAccessToken(1L, "kim@worksync.com", "USER");

		// when & then
		assertThat(jwtTokenProvider.validateAccessToken(expiredToken)).isFalse();
		assertThat(jwtTokenProvider.validateAccessToken("not-a-jwt")).isFalse();
	}
}
