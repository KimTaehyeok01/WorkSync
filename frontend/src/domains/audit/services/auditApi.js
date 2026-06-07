const BASE_URL = "http://localhost:8080/api";

// 감사 로그 목록 조회 (카테고리 / 기간 / 키워드 / 페이지)
// category: AUTH | HR | APPROVAL | TASK | null(전체)
// period:   today | week | month | all
export async function getAuditLogs(
  { category, period, keyword, page = 0, size = 10 },
  accessToken,
) {
  const params = new URLSearchParams();
  if (category && category !== "all") params.append("category", category);
  if (period && period !== "all") params.append("period", period);
  if (keyword) params.append("keyword", keyword);
  params.append("page", page);
  params.append("size", size);

  return await fetch(`${BASE_URL}/audit-logs?${params.toString()}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => json.data ?? null)
    .catch((error) => {
      console.log("에러발생: " + error);
      return null;
    });
}

// 상단 통계 위젯 (전체 / 오늘 / 로그인실패 / 결재처리 수)
export async function getAuditSummary(accessToken) {
  return await fetch(`${BASE_URL}/audit-logs/summary`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => json.data ?? null)
    .catch((error) => {
      console.log("에러발생: " + error);
      return null;
    });
}

// 리액트 겁나 어렵네...
