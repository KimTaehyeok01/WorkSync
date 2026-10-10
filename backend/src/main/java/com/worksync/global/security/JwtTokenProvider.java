package com.worksync.global.security;

import com.worksync.global.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // 토큰 종류 클레임 — access는 API 인증에만, refresh는 재발급·로그아웃에만 쓰도록 구분
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long employeeId, String email, String role) {
        long expMs = jwtConfig.getAccessTokenExpMinutes() * 60 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(employeeId))
                .claim("email", email)
                .claim("role", role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long employeeId) {
        long expMs = jwtConfig.getRefreshTokenExpDays() * 24 * 60 * 60 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(employeeId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // API 인증용 — 서명·만료가 유효하고 종류가 access인 토큰만 통과
    public boolean validateAccessToken(String token) {
        return isValidTokenOfType(token, TYPE_ACCESS);
    }

    // 재발급·로그아웃용 — 서명·만료가 유효하고 종류가 refresh인 토큰만 통과
    public boolean validateRefreshToken(String token) {
        return isValidTokenOfType(token, TYPE_REFRESH);
    }

    private boolean isValidTokenOfType(String token, String type) {
        try {
            return type.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getEmployeeId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }
}
