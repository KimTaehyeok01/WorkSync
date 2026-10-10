const BASE_URL = "/api";

// 게시판 목록 조회 (카테고리 드롭다운용)
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
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

//게시글 목록 조회 (departmentId: ADMIN이 부서게시판에서 특정 부서만 필터링할 때)
export async function getPosts(boardId, accessToken, departmentId) {
  // 화면에서 페이지를 나누므로 한 번에 넉넉히 받아온다 (기본 크기로 받으면 첫 페이지 글만 보인다)
  const params = new URLSearchParams({ size: 200, sort: "createdAt,desc" });
  if (departmentId) params.append("departmentId", departmentId);
  const url = `${BASE_URL}/boards/${boardId}/posts?${params}`;
  return await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => {
      return json.data?.content ?? [];
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

//부서 목록 조회 (ADMIN 부서 필터 드롭다운용)
export async function getDepartments(accessToken) {
  return await fetch(`${BASE_URL}/departments`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => json.data ?? [])
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 직원 조회
export async function getEmployee(accessToken) {
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
    .then((json) => {
      return json;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 게시글 등록
export async function getCreatePosts(boardId, data, accessToken) {
  return await fetch(`${BASE_URL}/boards/${boardId}/posts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  })
    .then((response) => response.json())
    .then((json) => {
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 게시글 업데이트
export async function getUpdatePosts(boardId, postId, data, accessToken) {
  return await fetch(`${BASE_URL}/boards/${boardId}/posts/${postId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
  })
    .then((response) => response.json())
    .then((json) => {
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

//게시글 상세 조회
export async function getPostById(boardId, postId, accessToken) {
  return await fetch(`${BASE_URL}/boards/${boardId}/posts/${postId}`, {
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
      console.log("에러발생: " + error);
    });
}

// 게시글 삭제
export async function deletePost(boardId, postId, accessToken) {
  return await fetch(`${BASE_URL}/boards/${boardId}/posts/${postId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => {
      return response.json();
    })
    .then((json) => {
      return json;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}

// 내 부서게시판 조회 -> 사용자가 속한 부서의 게시글만 조회 가능
export async function getDepartmentBoard(accessToken) {
  return await fetch(`${BASE_URL}/boards?boardType=DEPARTMENT`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => json.data?.[0] ?? null)
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 내 정보 조회 (부서 정보 포함)
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
    .then((data) => {
      return data;
    })
    .catch((error) => {
      console.log("에러발생 : " + error);
    });
}
