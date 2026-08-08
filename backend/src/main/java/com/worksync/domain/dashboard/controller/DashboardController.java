package com.worksync.domain.dashboard.controller;

import com.worksync.domain.dashboard.dto.DashboardDto;
import com.worksync.domain.dashboard.service.DashboardService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "대시보드 요약 API")
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  // GET / api/dashboard
  @Operation(summary = "내 대시보드 요약 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<DashboardDto.Response>> getDashboard(
          // AuthenticationPrincipal = JWT토큰에서 현재 로그인한 사용자 정보를 자동으로 주입
          // 따로 토큰 코드안써도 Spring Security가 처리해줌
          @AuthenticationPrincipal CustomUserDetails userDetails) {
    // 로그인한 사원의 id를 꺼내 서비스에 넘김
    DashboardDto.Response response = dashboardService.getDashboard(userDetails.getId());
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
