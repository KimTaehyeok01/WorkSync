import { isOk } from "../../../api/apiRequest";
import { trackEvent } from "../../../utils/matomo";

const BASE_URL = "/api";

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
      if (isOk(json)) trackEvent("file", "upload");
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
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
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

export async function deleteFile(accessToken, refType, refId) {
  return await fetch(
    `${BASE_URL}/files/delete?refType=${refType}&refId=${refId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )
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

export async function getFileId(accessToken, fileId) {
  return await fetch(`${BASE_URL}/files/${fileId}`, {
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
