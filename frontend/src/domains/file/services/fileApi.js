const BASE_URL = "http://localhost:8080/api";

export async function uploadFile(accessToken, fileData, refType) {
  return await fetch(`${BASE_URL}/files/upload?refType=${refType}`, {
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
