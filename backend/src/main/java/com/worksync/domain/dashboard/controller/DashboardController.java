package com.worksync.domain.dashboard.controller;

import com.worksync.domain.dashboard.dto.DashboardResponse;
import com.worksync.domain.dashboard.service.DashboardService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  // GET / api/dashboard
  @GetMapping
  public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
          // AuthenticationPrincipal = JWT토큰에서 현재 로그인한 사용자 정보를 자동으로 주입
          // 따로 토큰 코드안써도 Spring Security가 처리해줌
          @AuthenticationPrincipal CustomUserDetails userDetails) {
    // 로그인한 사원의 id를 꺼내 서비스에 넘김
    DashboardResponse response = dashboardService.getDashboard(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
