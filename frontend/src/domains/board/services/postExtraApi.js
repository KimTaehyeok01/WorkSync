// 게시글 부가 기능(상단 고정·읽음 확인·댓글) API 호출 모음
import { apiRequest } from "../../../api/apiRequest";

// 게시판의 상단 고정 글 목록
export function getPinnedPosts(accessToken, boardId) {
  return apiRequest(accessToken, `/posts/pinned?boardId=${boardId}`);
}

// 상단 고정 설정·해제 (ADMIN)
export function setPostPinned(accessToken, postId, isPinned) {
  return apiRequest(accessToken, "/posts/pin", "POST", { postId, isPinned });
}

// 읽음 기록
export function markPostRead(accessToken, postId) {
  return apiRequest(accessToken, "/posts/read", "POST", { postId });
}

// 고정 여부·읽은 수·읽은 사람(작성자·ADMIN 에게만)
export function getPostStatus(accessToken, postId) {
  return apiRequest(accessToken, `/posts/status?postId=${postId}`);
}

// 댓글 목록
export function getComments(accessToken, postId) {
  return apiRequest(accessToken, `/posts/comments?postId=${postId}`);
}

// 댓글 작성
export function createComment(accessToken, postId, content) {
  return apiRequest(accessToken, "/posts/comments", "POST", { postId, content });
}

// 댓글 삭제
export function deleteComment(accessToken, id) {
  return apiRequest(accessToken, "/posts/comments/delete", "POST", { id });
}
