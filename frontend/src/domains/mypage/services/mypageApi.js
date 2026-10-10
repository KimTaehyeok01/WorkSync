// 마이페이지에서 사용하는 API 호출 모음
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

// 내 프로필 수정 (연락처·프로필 이미지)
export function updateMyProfile(accessToken, profile) {
  return request(accessToken, "/employees/me/profile", "POST", profile);
}

// 내 비밀번호 변경
export function changeMyPassword(accessToken, passwords) {
  return request(accessToken, "/employees/me/password", "POST", passwords);
}
