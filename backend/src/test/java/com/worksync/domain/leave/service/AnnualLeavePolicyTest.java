// AnnualLeavePolicy 단위 테스트 — 입사일 기준 연차 일수 계산
package com.worksync.domain.leave.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** AnnualLeavePolicy 단위 테스트. */
class AnnualLeavePolicyTest {

	@DisplayName("입사일 정보가 없으면 기본 15일을 부여한다")
	@Test
	void grantDays_noHireDate_returnsBase() {
		assertThat(AnnualLeavePolicy.grantDays(null, 2026)).isEqualByComparingTo(BigDecimal.valueOf(15));
	}

	@DisplayName("근속 1년 미만이면 그 해 말까지 근무한 개월 수만큼 부여한다")
	@Test
	void grantDays_firstYear_returnsMonthsWorked() {
		// 2026-07-01 입사 → 2026-12-31 까지 5개월
		assertThat(AnnualLeavePolicy.grantDays(LocalDate.of(2026, 7, 1), 2026))
				.isEqualByComparingTo(BigDecimal.valueOf(5));
	}

	@DisplayName("근속 1년 이상이면 15일, 이후 2년마다 1일씩 늘어난다")
	@Test
	void grantDays_afterFirstYear_addsOneDayEveryTwoYears() {
		LocalDate hireDt = LocalDate.of(2020, 1, 1);

		assertThat(AnnualLeavePolicy.grantDays(hireDt, 2021)).isEqualByComparingTo(BigDecimal.valueOf(15));
		assertThat(AnnualLeavePolicy.grantDays(hireDt, 2022)).isEqualByComparingTo(BigDecimal.valueOf(15));
		assertThat(AnnualLeavePolicy.grantDays(hireDt, 2023)).isEqualByComparingTo(BigDecimal.valueOf(16));
		assertThat(AnnualLeavePolicy.grantDays(hireDt, 2025)).isEqualByComparingTo(BigDecimal.valueOf(17));
	}

	@DisplayName("연차는 최대 25일까지만 늘어난다")
	@Test
	void grantDays_longService_capsAtMax() {
		assertThat(AnnualLeavePolicy.grantDays(LocalDate.of(1990, 1, 1), 2026))
				.isEqualByComparingTo(BigDecimal.valueOf(25));
	}

	@DisplayName("입사 전 연도는 0일이다")
	@Test
	void grantDays_beforeHire_returnsZero() {
		assertThat(AnnualLeavePolicy.grantDays(LocalDate.of(2027, 3, 1), 2026))
				.isEqualByComparingTo(BigDecimal.ZERO);
	}
}
