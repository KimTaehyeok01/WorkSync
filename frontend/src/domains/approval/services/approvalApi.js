const BASE_URL = "http://localhost:8080/api";

// 내 정보 조회
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

// 직원 목록 조회
export async function getEmployees(accessToken) {
  return await fetch(`${BASE_URL}/employees`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      console.log("status : ", response.status);
      return response.json();
    })
    .then((text) => {
      return Array.isArray(text.data) ? text.data : (text.data?.content ?? []);
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 전자결재 등록
export async function createApproval(accessToken, body) {
  return await fetch(`${BASE_URL}/approvals`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(body),
  })
    .then((response) => response.json())
    .then((json) => {
      console.log("등록 결과 : ", json);
      return json;
    })
    .catch((error) => console.log("에러 발생 : " + error));
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

export async function getForms(accessToken) {
  return await fetch(`${BASE_URL}/approvals/forms`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      console.log("forms : " + json);
      return json.data ?? [];
    })
    .catch((error) => console.log("에러발생 : " + error));
}
