// 자원 예약 화면에서 사용하는 API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

export { getMyInfo } from "../../../api/apiRequest";

// 자원 목록 조회
export function getResources(accessToken) {
  return apiRequest(accessToken, "/resources");
}

// 자원 등록 (ADMIN 전용)
export function createResource(accessToken, resource) {
  return apiRequest(accessToken, "/resources", "POST", resource);
}

// 자원 수정 (ADMIN 전용)
export function updateResource(accessToken, resource) {
  return apiRequest(accessToken, "/resources/update", "POST", resource);
}

// 일자별 전체 예약 조회
export function getReservations(accessToken, date) {
  return apiRequest(accessToken, `/reservations?date=${date}`);
}

// 내 예정 예약 조회
export function getMyReservations(accessToken) {
  return apiRequest(accessToken, "/reservations/my");
}

// 예약
export function reserve(accessToken, reservation) {
  return apiRequest(accessToken, "/reservations", "POST", reservation);
}

// 예약 취소
export function cancelReservation(accessToken, id) {
  return apiRequest(accessToken, "/reservations/cancel", "POST", { id });
}
