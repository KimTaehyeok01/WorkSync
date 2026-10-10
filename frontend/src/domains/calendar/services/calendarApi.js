// 캘린더 화면에서 사용하는 API 호출 모음
const BASE_URL = "/api";

// 공통 요청 — 응답 본문({ status, message, data })을 그대로 반환
async function request(accessToken, path, method = "GET", body) {
  return await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: body ? JSON.stringify(body) : undefined,
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

// 기간 내 일정 조회 (내 개인 일정 + 내 부서 일정 + 전사 일정)
export function getSchedules(accessToken, from, to) {
  return request(accessToken, `/schedules?from=${from}&to=${to}`);
}

// 일정 등록
export function createSchedule(accessToken, schedule) {
  return request(accessToken, "/schedules", "POST", schedule);
}

// 일정 수정
export function updateSchedule(accessToken, schedule) {
  return request(accessToken, "/schedules/update", "POST", schedule);
}

// 일정 삭제
export function deleteSchedule(accessToken, id) {
  return request(accessToken, "/schedules/delete", "POST", { id });
}

// 내 부서 승인 휴가 조회
export function getDepartmentLeaves(accessToken, from, to) {
  return request(accessToken, `/leave/department?from=${from}&to=${to}`);
}

// 내가 담당자인 업무 조회 (마감일 표시용)
export function getMyTasks(accessToken, employeeId) {
  return request(accessToken, `/tasks/assignee/${employeeId}?size=200`);
}
