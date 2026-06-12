const BASE_URL = "http://localhost:8080/api";

export async function getMyInfo(accessToken) {
  return await fetch(`${BASE_URL}/employees/me`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

export async function patchStatus(accessToken, status) {
  return await fetch(`${BASE_URL}/employees/me/status?status=${status}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}
