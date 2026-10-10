// 통합 검색 응답 DTO 모음
package com.worksync.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 통합 검색 DTO 모음. */
public class SearchDto {

	// 종류와 무관하게 같은 모양으로 내려 화면에서 한 가지 방식으로 그린다
	@Getter @Builder
	@Schema(name = "SearchDto.Item")
	public static class Item {

		@Schema(description = "대상 ID", example = "1")
		private Long id;

		@Schema(description = "상위 ID — 게시글이면 게시판 ID, 그 외에는 null", example = "2")
		private Long parentId;

		@Schema(description = "제목 (직원이면 이름)", example = "주간 회의")
		private String title;

		@Schema(description = "부가 설명 (작성자·부서·상태 등)", example = "홍길동 · 개발팀")
		private String description;

		@Schema(description = "기준 일시 (작성·시작 일시 등)")
		private LocalDateTime dateAt;
	}

	@Getter @Builder
	@Schema(name = "SearchDto.Response")
	public static class Response {

		@Schema(description = "직원")
		private List<Item> employees;

		@Schema(description = "게시글")
		private List<Item> posts;

		@Schema(description = "업무")
		private List<Item> tasks;

		@Schema(description = "결재 문서")
		private List<Item> approvals;

		@Schema(description = "일정")
		private List<Item> schedules;

		@Schema(description = "메일")
		private List<Item> mails;
	}
}
