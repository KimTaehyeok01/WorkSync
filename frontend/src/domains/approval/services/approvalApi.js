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
      return response.json();
    })
    .then((text) => {
      return Array.isArray(text.data) ? text.data : (text.data?.content ?? []);
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 내가 기안한 문서 목록(작성자 기준)
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
      return json.data ?? [];
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

// 결재함 - 내가 결재선에 포함된 문서 전체 (상태 필터링 가능)
export async function getApprovalInbox(accessToken, status) {
  const url =
    status && status !== "all"
      ? `${BASE_URL}/approvals/inbox?status=${status}`
      : `${BASE_URL}/approvals/inbox`;

  return fetch(url, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      return json.data ?? [];
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 참조함 - 내가 REFERENCE로 지정된 문서
export async function getReferenceApprovals(accessToken) {
  return await fetch(`${BASE_URL}/approvals/reference`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((res) => res.json())
    .then((json) => {
      return json.data ?? [];
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 전자결재 상세조회
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
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

// 양식 목록 가져오기
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
      return json.data ?? [];
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 결재선
export async function processApproval(accessToken, id, status, comment = "") {
  return await fetch(`${BASE_URL}/approvals/${id}/process`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ status, comment }),
  })
    .then((res) => res.json())
    .then((json) => {
      return json;
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
      return json;
    })
    .catch((error) => console.log("에러 발생 : " + error));
}

// 전자결재 수정
export function updateApproval(accessToken, id, body) {
  return fetch(`${BASE_URL}/approvals/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(body),
  })
    .then((res) => res.json())
    .then((json) => {
      return json;
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 전자결재 삭제
export function deleteApproval(accessToken, id) {
  return fetch(`${BASE_URL}/approvals/${id}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      if (!response.ok) throw new Error(response.status);
      return response;
    })
    .catch((error) => {
      console.log("에러 발생 : " + error);
      throw error; // onClick의 try-catch 전달용
    });
}

// 내가 결재해야 할 문서 목록
export function getPendingApproval(accessToken) {
  return fetch(`${BASE_URL}/approvals/pending`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => {
      return json.data;
    })
    .catch((error) => {
      console.log("에러 발생 : ", error);
    });
}

// 잔여일 조회
export async function getLeaveBalance(accessToken, employeeId) {
  // employeeId가 있어야 본인외 잔여일 확인 가능
  const url = employeeId
    ? `${BASE_URL}/leave/balance?employeeId=${employeeId}`
    : `${BASE_URL}/leave/balance`;

  return fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
    .then((res) => {
      if (!res.ok) return null;
      return res.json();
    })
    .then((json) => json.data ?? null)
    .catch((error) => console.log("에러발생 : ", error));
}
