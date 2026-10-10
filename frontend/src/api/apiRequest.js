// 각 domain services가 공통으로 쓰는 API 요청 헬퍼
const BASE_URL = "/api";

/**
 * 인증 헤더를 붙여 요청하고 응답 본문({ status, message, data })을 그대로 반환한다.
 * 실패 응답도 본문으로 돌려주므로, 호출부는 json.status >= 400 으로 실패를 판단하고 json.message 를 보여주면 된다.
 * 네트워크 오류 등으로 본문을 받지 못하면 undefined 를 반환한다.
 */
export async function apiRequest(accessToken, path, method = "GET", body) {
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

// 응답이 성공인지 — 본문이 있고 상태 코드가 400 미만
export function isOk(json) {
  return !!json && json.status < 400;
}

// 내 정보 조회 — 여러 화면에서 권한·부서 확인용으로 쓴다
export function getMyInfo(accessToken) {
  return apiRequest(accessToken, "/employees/me");
}

// 직원 목록 조회 — 받는 사람·참석자 선택 등에 쓴다
export function getEmployees(accessToken) {
  return apiRequest(accessToken, "/employees");
}
