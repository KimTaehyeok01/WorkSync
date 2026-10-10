// 자원 예약 화면에서 사용하는 API 호출 모음
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

// 자원 목록 조회
export function getResources(accessToken) {
  return request(accessToken, "/resources");
}

// 자원 등록 (ADMIN 전용)
export function createResource(accessToken, resource) {
  return request(accessToken, "/resources", "POST", resource);
}

// 자원 수정 (ADMIN 전용)
export function updateResource(accessToken, resource) {
  return request(accessToken, "/resources/update", "POST", resource);
}

// 일자별 전체 예약 조회
export function getReservations(accessToken, date) {
  return request(accessToken, `/reservations?date=${date}`);
}

// 내 예정 예약 조회
export function getMyReservations(accessToken) {
  return request(accessToken, "/reservations/my");
}

// 예약
export function reserve(accessToken, reservation) {
  return request(accessToken, "/reservations", "POST", reservation);
}

// 예약 취소
export function cancelReservation(accessToken, id) {
  return request(accessToken, "/reservations/cancel", "POST", { id });
}
