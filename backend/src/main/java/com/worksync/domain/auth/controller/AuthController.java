package com.worksync.domain.auth.controller;

import com.worksync.domain.auth.dto.LoginRequest;
import com.worksync.domain.auth.dto.LoginResponse;
import com.worksync.domain.auth.dto.ReissueRequest;
import com.worksync.domain.auth.service.AuthService;
import com.worksync.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인 — 사번/비밀번호 검증 후 JWT 토큰 발급 (로그인 시 출근 자동 기록)
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, clientIp)));
    }

    // 토큰 재발급 — 리프레시 토큰으로 새 액세스/리프레시 토큰 발급
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> reissue(
            @RequestBody @Valid ReissueRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request)));
    }

    // 로그아웃
    @DeleteMapping("/token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody ReissueRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
