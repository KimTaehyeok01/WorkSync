// 근태·연차 화면에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

export { getMyInfo } from "../../../api/apiRequest";

// 출근 체크
export function checkIn(accessToken) {
  return apiRequest(accessToken, "/attendance/check-in", "POST");
}

// 퇴근 체크
export function checkOut(accessToken) {
  return apiRequest(accessToken, "/attendance/check-out", "POST");
}

// 내 근태 월별 조회
export function getMyAttendance(accessToken, year, month) {
  return apiRequest(accessToken, `/attendance/my?year=${year}&month=${month}`);
}

// 일자별 전 직원 근태 조회 — 미출근자는 결근으로 내려온다 (ADMIN 전용)
export function getAdminAttendance(accessToken, date) {
  return apiRequest(accessToken, `/attendance/admin?date=${date}`);
}

// 근태 기록 수정 (ADMIN 전용)
export function updateAttendance(accessToken, attendance) {
  return apiRequest(accessToken, "/attendance/admin/update", "POST", attendance);
}

// 내 연차 잔여 조회
export function getLeaveBalance(accessToken) {
  return apiRequest(accessToken, "/leave/balance");
}

// 내 휴가 신청 내역 조회 (연도별)
export function getMyLeaves(accessToken, year) {
  return apiRequest(accessToken, `/leave/my?year=${year}`);
}
