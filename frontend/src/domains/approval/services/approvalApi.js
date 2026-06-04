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
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

export async function getMyApprovals(accessToken, status = "all") {
  const url =
    status !== "all"
      ? `${BASE_URL}/approvals/my?status=${status}`
      : `${BASE_URL}/approvals/my`;

  return await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .then((json) => {
      console.log("응답 전체:", json);
      console.log("data:", json.data);
      return json.data ?? [];
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

export async function getApprovalById(accessToken, id) {
  return await fetch(`${BASE_URL}/approvals/${id}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

export async function getApprovalByForm(accessToken) {
  return await fetch(`${BASE_URL}/approvals/forms`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .then((json) => {
      console.log("data : " + json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}
