// 사내 메일 화면에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

export { getMyInfo, getEmployees } from "../../../api/apiRequest";

// 받은편지함·보낸편지함 조회 — box: "inbox" | "sent"
export function getMails(accessToken, box, { page = 0, unreadOnly = false, keyword = "" }) {
  const params = new URLSearchParams({ page, size: 20 });
  if (box === "inbox" && unreadOnly) params.append("unreadOnly", "true");
  if (keyword) params.append("keyword", keyword);
  return apiRequest(accessToken, `/mails/${box}?${params}`);
}

// 메일 상세 조회 — 수신자가 처음 열면 읽음 처리된다
export function getMail(accessToken, id) {
  return apiRequest(accessToken, `/mails/detail?id=${id}`);
}

// 안 읽은 메일 수
export function getUnreadMailCount(accessToken) {
  return apiRequest(accessToken, "/mails/unread-count");
}

// 메일 보내기 — { toIds, ccIds, subject, content }
export function sendMail(accessToken, mail) {
  return apiRequest(accessToken, "/mails", "POST", mail);
}

// 메일 삭제 (내 메일함에서만) — box: "INBOX" | "SENT"
export function deleteMail(accessToken, id, box) {
  return apiRequest(accessToken, "/mails/delete", "POST", { id, box });
}
