const BASE_URL = "http://localhost:8080/api";

export async function uploadFile(accessToken, fileData) {
  return await fetch(`${BASE_URL}/files/upload`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: fileData,
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

export async function saveFile(accessToken, file) {
  return await fetch(`${BASE_URL}/files/save`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(file),
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

export async function getFile(accessToken, refType, refId) {
  return await fetch(`${BASE_URL}/files?refType=${refType}&refId=${refId}`, {
    method: "GET",
    headers: {
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

export async function deleteFile(accessToken, id) {
  return await fetch(`${BASE_URL}/files?${id}`, {
    method: "DELETE",
    headers: {
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
