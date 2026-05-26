const BASE_URL = "http://localhost:8080/api";

// 게시글 목록 조회 (카테고리 드롭다운용)
export async function getBoards(accessToken) {
  return await fetch(`${BASE_URL}/boards`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}
