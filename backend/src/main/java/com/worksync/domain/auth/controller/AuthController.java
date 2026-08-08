package com.worksync.domain.auth.controller;

import com.worksync.domain.auth.dto.AuthDto;
import com.worksync.domain.auth.service.AuthService;
import com.worksync.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인 — 사번/비밀번호 검증 후 JWT 토큰 발급 (로그인 시 출근 자동 기록)
    @Operation(summary = "로그인 (JWT 토큰 발급, 출근 자동 기록)")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @RequestBody @Valid AuthDto.LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, clientIp, userAgent)));
    }

    // 토큰 재발급 — 리프레시 토큰으로 새 액세스/리프레시 토큰 발급
    @Operation(summary = "토큰 재발급")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> reissue(
            @RequestBody @Valid AuthDto.ReissueRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request)));
    }

    // 로그아웃
    @Operation(summary = "로그아웃")
    @DeleteMapping("/token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody AuthDto.ReissueRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        authService.logout(request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
