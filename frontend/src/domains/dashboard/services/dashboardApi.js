const BASE_URL = "http://localhost:8080/api";

// 대시보드 통계 조회 (GET /api/dashboard)
export async function getDashboard(accessToken) {
  return await fetch(`${BASE_URL}/dashboard`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      console.log("[Dashboard]", json);
      return json.data;
    })
    .catch((error) => {
      console.log("대시보드 에러발생: " + error);
    });
}

// 내가 결재해야 할 대기 문서 목록 (GET /api/approvals/pending)
// 반환: [{ id, title, formName, drafterName, status, submittedAt, createdAt }, ...]
export async function getPendingApprovals(accessToken) {
  return await fetch(`${BASE_URL}/approvals/pending`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      console.log("[PendingApprovals]", json);
      return json.data ?? [];
    })
    .catch((error) => {
      console.log("결재 대기 에러발생: " + error);
      return [];
    });
}

// 최근 게시물 조회 (공지사항 게시판 기준, 최신 4건)
// 1) GET /api/boards?boardType=NOTICE → 공지사항 게시판 ID 획득
// 2) GET /api/boards/{id}/posts?size=4&sort=createdAt,desc → 최신 게시물 조회
// 반환: [{ id, boardId, boardName, authorId, authorName, title, content, createdAt }, ...]
export async function getRecentPosts(accessToken) {
  try {
    // 1) 공지사항 게시판 조회
    const boardsRes = await fetch(`${BASE_URL}/boards?boardType=NOTICE`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
    });
    const boardsJson = await boardsRes.json();
    const boards = boardsJson.data ?? [];
    console.log("[Boards]", boards);

    if (boards.length === 0) return [];

    // 2) 첫 번째 공지사항 게시판의 최신 게시물 4건
    const boardId = boards[0].id;
    const postsRes = await fetch(
      `${BASE_URL}/boards/${boardId}/posts?size=4&sort=createdAt,desc`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );
    const postsJson = await postsRes.json();
    const posts = postsJson.data?.content ?? [];
    console.log("[RecentPosts]", posts);
    return posts;
  } catch (error) {
    console.log("최근 게시물 에러발생: " + error);
    return [];
  }
}

// 내 부서 팀원 출근 현황 (GET /api/attendance/department?date=2026-06-12)
// 반환: [{ id, employeeId, employeeName, workDate, checkInTime, checkOutTime, status, createdAt }, ...]
export async function getDepartmentAttendance(accessToken, date) {
  return await fetch(`${BASE_URL}/attendance/department?date=${date}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      console.log("[DepartmentAttendance]", json);
      return json.data ?? [];
    })
    .catch((error) => {
      console.log("부서 근태 에러발생: " + error);
      return [];
    });
}
export async function getMyPendingApprovals(accessToken) {
  return await fetch(`${BASE_URL}/approvals/my?status=IN_PROGRESS`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => json.data ?? [])
    .catch(() => []);
}