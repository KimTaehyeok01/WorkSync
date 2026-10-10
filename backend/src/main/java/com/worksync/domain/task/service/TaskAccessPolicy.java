// 업무를 누가 볼 수 있고 누가 작성자·담당자인지 판정하는 규칙 모음 (TaskService·FileService 공용)
package com.worksync.domain.task.service;

import com.worksync.domain.employee.entity.Employee;
import com.worksync.domain.employee.entity.EmployeeRole;
import com.worksync.domain.task.entity.Task;
import org.springframework.stereotype.Component;

/** 업무 접근 권한 규칙. */
@Component
public class TaskAccessPolicy {

	// 조회 권한 — 작성자, 담당자, 업무에 지정된 부서의 소속 직원, 관리자
	public boolean canView(Task task, Employee employee) {
		if (employee.getRole() == EmployeeRole.ADMIN
				|| isCreator(task, employee.getId())
				|| isAssignee(task, employee.getId())) {
			return true;
		}
		// 부서가 없는 업무와 부서가 없는 직원이 같은 부서로 묶이지 않게 null 을 먼저 거른다
		return task.getDepartment() != null
				&& employee.getDepartment() != null
				&& task.getDepartment().getId().equals(employee.getDepartment().getId());
	}

	public boolean isCreator(Task task, Long employeeId) {
		return task.getCreator().getId().equals(employeeId);
	}

	public boolean isAssignee(Task task, Long employeeId) {
		return task.getAssignee() != null
				&& task.getAssignee().getId().equals(employeeId);
	}
}
