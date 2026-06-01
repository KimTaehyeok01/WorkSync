const BASE_URL = "http://localhost:8080/api";

// 전체 업무 목록 조회
export async function getTaskList(accessToken, page = 0, size = 10) {
  return await fetch(`${BASE_URL}/tasks?page=${page}&size=${size}`, {
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
