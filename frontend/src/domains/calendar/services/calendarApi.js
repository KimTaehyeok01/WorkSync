// 캘린더 화면에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

export { getMyInfo, getEmployees } from "../../../api/apiRequest";

// 기간 내 일정 조회 (내 개인 일정 + 내 부서 일정 + 전사 일정 + 내가 참석자인 일정, 반복 일정은 회차별)
export function getSchedules(accessToken, from, to) {
  return apiRequest(accessToken, `/schedules?from=${from}&to=${to}`);
}

// 일정 등록
export function createSchedule(accessToken, schedule) {
  return apiRequest(accessToken, "/schedules", "POST", schedule);
}

// 일정 수정
export function updateSchedule(accessToken, schedule) {
  return apiRequest(accessToken, "/schedules/update", "POST", schedule);
}

// 일정 삭제
export function deleteSchedule(accessToken, id) {
  return apiRequest(accessToken, "/schedules/delete", "POST", { id });
}

// 내 부서 승인 휴가 조회
export function getDepartmentLeaves(accessToken, from, to) {
  return apiRequest(accessToken, `/leave/department?from=${from}&to=${to}`);
}

// 내가 담당자인 업무 조회 (마감일 표시용)
export function getMyTasks(accessToken, employeeId) {
  return apiRequest(accessToken, `/tasks/assignee/${employeeId}?size=200`);
}

// 내 예정 예약 조회 (달력 표시용)
export function getMyReservations(accessToken) {
  return apiRequest(accessToken, "/reservations/my");
}
