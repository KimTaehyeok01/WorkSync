// 관리자용 직원 기능(일괄 등록·발령 이력) 요청/응답 DTO 모음
package com.worksync.domain.employee.dto;

import com.worksync.domain.employee.entity.EmployeeHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자용 직원 기능 DTO 모음. */
public class EmployeeAdminDto {

	// CSV 한 행 — 형식 검증을 행 단위로 하기 위해 모두 문자열로 받는다
	@Getter @Setter
	@Schema(name = "EmployeeAdminDto.BulkRow")
	public static class BulkRow {

		@Schema(description = "사번", example = "EMP101")
		private String empNo;

		@Schema(description = "이름", example = "홍길동")
		private String name;

		@Schema(description = "이메일", example = "hong@worksync.com")
		private String email;

		@Schema(description = "초기 비밀번호")
		private String password;

		@Schema(description = "연락처", example = "010-1234-5678")
		private String phone;

		@Schema(description = "직급 (STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, VICE_PRESIDENT, PRESIDENT, CEO)", example = "STAFF")
		private String jobGrade;

		@Schema(description = "부서명 — 비우면 미배정", example = "개발팀")
		private String departmentName;

		@Schema(description = "입사일 (yyyy-MM-dd)", example = "2026-01-02")
		private String hireDate;
	}

	@Getter @Setter
	@Schema(name = "EmployeeAdminDto.BulkRequest")
	public static class BulkRequest {

		@NotEmpty
		@Size(max = 500)
		@Schema(description = "등록할 직원 행 목록 (최대 500행)")
		private List<BulkRow> rows;
	}

	@Getter @Builder
	@Schema(name = "EmployeeAdminDto.BulkRowResult")
	public static class BulkRowResult {

		@Schema(description = "행 번호 (1부터)", example = "1")
		private Integer rowNumber;

		@Schema(description = "사번", example = "EMP101")
		private String empNo;

		@Schema(description = "등록 성공 여부", example = "true")
		private Boolean isSuccess;

		@Schema(description = "실패 사유 (성공이면 null)")
		private String message;
	}

	@Getter @Builder
	@Schema(name = "EmployeeAdminDto.BulkResponse")
	public static class BulkResponse {

		@Schema(description = "등록 성공 건수", example = "9")
		private Integer successCount;

		@Schema(description = "실패 건수", example = "1")
		private Integer failCount;

		@Schema(description = "행별 결과")
		private List<BulkRowResult> results;
	}

	@Getter @Builder
	@Schema(name = "EmployeeAdminDto.HistoryResponse")
	public static class HistoryResponse {

		@Schema(description = "이력 ID", example = "1")
		private Long id;

		@Schema(description = "변경 항목 (DEPARTMENT, JOB_GRADE, ROLE)", example = "DEPARTMENT")
		private String changeType;

		@Schema(description = "변경 전 값", example = "개발팀")
		private String beforeValue;

		@Schema(description = "변경 후 값", example = "영업팀")
		private String afterValue;

		@Schema(description = "처리한 관리자 이름", example = "관리자")
		private String actorName;

		@Schema(description = "변경 일시")
		private LocalDateTime createdAt;

		public static HistoryResponse from(EmployeeHistory history) {
			return HistoryResponse.builder()
					.id(history.getId())
					.changeType(history.getChangeType())
					.beforeValue(history.getBeforeValue())
					.afterValue(history.getAfterValue())
					.actorName(history.getActorName())
					.createdAt(history.getCreatedAt())
					.build();
		}
	}
}
