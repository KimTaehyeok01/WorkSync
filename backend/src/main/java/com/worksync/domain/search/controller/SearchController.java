// 통합 검색 API 컨트롤러
package com.worksync.domain.search.controller;

import com.worksync.domain.search.dto.SearchDto;
import com.worksync.domain.search.service.SearchService;
import com.worksync.global.response.ApiResponse;
import com.worksync.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 통합 검색 API. */
@Tag(name = "Search", description = "통합 검색 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	// 직원·게시글·업무·결재·일정·메일을 종류별 최대 5건씩 검색 (검색어 2자 이상)
	@Operation(summary = "통합 검색")
	@GetMapping
	public ResponseEntity<ApiResponse<SearchDto.Response>> search(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("keyword") String keyword) {

		return ResponseEntity.ok(ApiResponse.ok(searchService.search(userDetails.getId(), keyword)));
	}
}
