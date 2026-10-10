// 마이페이지에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

export { getMyInfo } from "../../../api/apiRequest";

// 내 프로필 수정 (연락처·프로필 이미지)
export function updateMyProfile(accessToken, profile) {
  return apiRequest(accessToken, "/employees/me/profile", "POST", profile);
}

// 내 비밀번호 변경
export function changeMyPassword(accessToken, passwords) {
  return apiRequest(accessToken, "/employees/me/password", "POST", passwords);
}

// 알림 수신 설정 조회
export function getNotificationSettings(accessToken) {
  return apiRequest(accessToken, "/notifications/center/settings");
}

// 알림 수신 설정 변경
export function updateNotificationSetting(accessToken, type, isEnabled) {
  return apiRequest(accessToken, "/notifications/center/settings", "POST", {
    type,
    isEnabled,
  });
}
