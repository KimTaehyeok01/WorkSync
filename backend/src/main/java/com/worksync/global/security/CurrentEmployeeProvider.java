// 현재 로그인한 직원을 SecurityContext 에서 꺼내 DB 에서 다시 읽어 주는 컴포넌트
package com.worksync.global.security;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.repository.EmployeeRepository;
import com.worksync.global.exception.CustomException;
import com.worksync.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 현재 로그인한 직원을 조회한다. */
@Component
@RequiredArgsConstructor
public class CurrentEmployeeProvider {

	private final EmployeeRepository employeeRepository;

	// principal 의 Employee 는 필터에서 읽고 분리된(detached) 객체라
	// 부서(LAZY)를 안전하게 읽으려고 다시 조회한다 — 호출하는 서비스의 트랜잭션 안에서 쓴다
	public Employee getCurrentEmployee() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
		return employeeRepository.findById(details.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
	}
}
