// 입사일 기준 연차 부여 일수를 계산하는 정책
package com.worksync.domain.leave.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/** 입사일 기준 연차 부여 정책. */
public final class AnnualLeavePolicy {

	private static final int BASE_DAYS = 15;
	private static final int MAX_DAYS = 25;
	private static final int MAX_FIRST_YEAR_DAYS = 11;

	private AnnualLeavePolicy() {
	}

	/**
	 * 귀속 연도의 연차 일수를 계산한다 (해당 연도 말일 기준 근속).
	 * - 입사일 정보가 없으면 기본 15일
	 * - 근속 1년 미만: 근무한 개월 수만큼 월 1일 (최대 11일)
	 * - 근속 1년 이상: 15일, 이후 2년마다 1일 가산 (최대 25일)
	 */
	public static BigDecimal grantDays(LocalDate hireDt, int year) {
		if (hireDt == null) {
			return BigDecimal.valueOf(BASE_DAYS);
		}
		LocalDate yearEnd = LocalDate.of(year, 12, 31);
		if (hireDt.isAfter(yearEnd)) {
			return BigDecimal.ZERO;
		}

		Period service = Period.between(hireDt, yearEnd);
		if (service.getYears() < 1) {
			return BigDecimal.valueOf(Math.min(service.getMonths(), MAX_FIRST_YEAR_DAYS));
		}
		return BigDecimal.valueOf(Math.min(BASE_DAYS + (service.getYears() - 1) / 2, MAX_DAYS));
	}
}
