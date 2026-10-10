// 관리자용 직원 기능(CSV 일괄 등록·발령 이력) API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

// 직원 일괄 등록 — rows: [{ empNo, name, email, password, phone, jobGrade, departmentName, hireDate }]
export function bulkCreateEmployees(accessToken, rows) {
  return apiRequest(accessToken, "/employees/admin/bulk", "POST", { rows });
}

// 인사 발령 이력 조회
export function getEmployeeHistory(accessToken, employeeId) {
  return apiRequest(accessToken, `/employees/admin/history?employeeId=${employeeId}`);
}
