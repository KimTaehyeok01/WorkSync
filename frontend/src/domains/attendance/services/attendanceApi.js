// 근태·연차 화면에서 사용하는 API 호출 모음
const BASE_URL = "/api";

// 공통 요청 — 응답 본문({ status, message, data })을 그대로 반환
async function request(accessToken, path, method = "GET") {
  return await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .catch((error) => {
      console.error("에러발생: " + error);
    });
}

// 내 정보 조회
export function getMyInfo(accessToken) {
  return request(accessToken, "/employees/me");
}

// 출근 체크
export function checkIn(accessToken) {
  return request(accessToken, "/attendance/check-in", "POST");
}

// 퇴근 체크
export function checkOut(accessToken) {
  return request(accessToken, "/attendance/check-out", "POST");
}

// 내 근태 월별 조회
export function getMyAttendance(accessToken, year, month) {
  return request(accessToken, `/attendance/my?year=${year}&month=${month}`);
}

// 일자별 전체 근태 조회 (ADMIN 전용)
export function getAttendanceByDate(accessToken, date) {
  return request(accessToken, `/attendance?date=${date}`);
}

// 내 연차 잔여 조회
export function getLeaveBalance(accessToken) {
  return request(accessToken, "/leave/balance");
}

// 내 휴가 신청 내역 조회 (연도별)
export function getMyLeaves(accessToken, year) {
  return request(accessToken, `/leave/my?year=${year}`);
}
