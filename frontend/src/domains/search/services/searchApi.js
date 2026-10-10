// 통합 검색 API 호출
import { apiRequest } from "../../../api/apiRequest";

// 직원·게시글·업무·결재·일정·메일을 종류별 최대 5건씩 검색 (검색어 2자 이상)
export function search(accessToken, keyword) {
  return apiRequest(accessToken, `/search?keyword=${encodeURIComponent(keyword)}`);
}
