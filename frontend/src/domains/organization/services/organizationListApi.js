const BASE_URL = "http://localhost:8080/api";

export async function getDepartments(accessToken) {
  return await fetch(`${BASE_URL}/departments`, {
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

export async function createDepartments(accessToken, addDeptName) {
  return await fetch(`${BASE_URL}/departments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ name: addDeptName }),
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

export async function editDepartments(accessToken, editDeptName, editDeptId) {
  return await fetch(`${BASE_URL}/departments/${editDeptId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ name: editDeptName }),
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

export async function deleteDepartments(accessToken, editDeptId) {
  const confirmText = confirm(
    "삭제 시 해당 부서 직원의 부서도 삭제됩니다. 삭제하시겠습니까?",
  );

  if (!confirmText) {
    return;
  }

  return await fetch(`${BASE_URL}/departments/${editDeptId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ id: editDeptId }),
  })
    .then((response) => {
      return response.ok;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

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

export async function getEmployeeById(accessToken, id) {
  return await fetch(`${BASE_URL}/employees/${id}`, {
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
      console.log(json.data);
      return json;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

export async function createEmployee(accessToken, form) {
  return await fetch(`${BASE_URL}/employees`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(form),
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

export async function editEmployee(accessToken, id, form) {
  return await fetch(`${BASE_URL}/employees/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(form),
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

export async function deleteEmployee(accessToken, id) {
  const confirmText = confirm("삭제 시 복구가 불가능합니다. 삭제하시겠습니까?");

  if (!confirmText) {
    return;
  }

  return await fetch(`${BASE_URL}/employees/${id}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ id: id }),
  })
    .then((response) => {
      return response.ok;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}
