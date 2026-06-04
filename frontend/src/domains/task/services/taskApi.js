const BASE_URL = "http://localhost:8080/api";

// 전체 업무 목록 조회
export async function getTaskList(accessToken, page = 0, size = 10, status) {
  const params = new URLSearchParams({ page, size });
  if (status && status !== "all") params.append("status", status);
  return await fetch(`${BASE_URL}/tasks?${params}`, {
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

// 내 업무 목록 조회
export async function getMyTaskList(accessToken, page = 0, size = 10) {
  return await fetch(`${BASE_URL}/tasks/my?page=${page}&size=${size}`, {
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

// 부서별 업무 목록 조회
export async function getTasksByDepartment(
  accessToken,
  departmentId,
  page = 0,
  size = 10,
) {
  return await fetch(
    `${BASE_URL}/tasks/department/${departmentId}?page=${page}&size=${size}`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )
    .then((response) => response.json())
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 업무 상세 조회
export async function getTaskById(accessToken, taskId) {
  return await fetch(`${BASE_URL}/tasks/${taskId}`, {
    methos: "GET",
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
      console.log("에러발생 : " + error);
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
    .then((response) => response.json())
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

// 업무 등록
export async function createTask(accessToken, data) {
  return await fetch(`${BASE_URL}/tasks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  })
    .then((response) => response.json())
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

// 업무 수정
export async function updateTask(accessToken, taskId, data) {
  return await fetch(`${BASE_URL}/tasks/${taskId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  })
    .then((response) => response.json())
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 업무 삭제
export async function deleteTask(accessToken, taskId, data) {
  return await fetch(`${BASE_URL}/tasks/${taskId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      console.log("삭제 상태코드 : " + response.status);
      return response.json();
    })
    .then((json) => {
      console.log("삭제 응답 : " + json);
    })
    .catch((error) => console.log("에러발생 : " + error));
}

// 내 정보 조회
export async function getMyInfo(accessToken, data) {
  return await fetch(`${BASE_URL}/employees/me`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      console.log("상태코드 : " + response.status);
      return response.json();
    })
    .then((json) => {
      console.log(json);
      return json.data;
    })
    .catch((error) => console.log("에러발생 : " + error));
}
