// 알림 센터 화면에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

// 알림 목록 — 유형·안 읽음 필터, 페이징
export function getNotificationCenter(accessToken, { type = "", unreadOnly = false, page = 0 }) {
  const params = new URLSearchParams({ page, size: 20 });
  if (type) params.append("type", type);
  if (unreadOnly) params.append("unreadOnly", "true");
  return apiRequest(accessToken, `/notifications/center?${params}`);
}

// 알림 1건 읽음 처리
export function readNotification(accessToken, id) {
  return apiRequest(accessToken, "/notifications/center/read", "POST", { id });
}

// 전체 읽음 처리
export function readAllNotifications(accessToken) {
  return apiRequest(accessToken, "/notifications/center/read-all", "POST");
}

// 알림을 눌렀을 때 이동할 경로 — 상단 종 아이콘과 알림 센터가 같이 쓴다
export function getNotificationPath(notification) {
  switch (notification.type) {
    case "APPROVAL":
      return `/approval/${notification.targetId}`;
    case "TASK":
      return `/tasks/${notification.targetId}`;
    case "MESSAGE":
      return "/messenger";
    case "MAIL":
      return `/mail?id=${notification.targetId}`;
    case "SCHEDULE":
      return "/calendar";
    case "RESERVATION":
      return "/reservation";
    case "BOARD":
      return "/board";
    default:
      return null;
  }
}
