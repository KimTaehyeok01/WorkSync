const BASE_URL = "/api";

export async function getChatRoom(accessToken) {
  return await fetch(`${BASE_URL}/chat/rooms`, {
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

export async function createChatRoom(accessToken, data) {
  return await fetch(`${BASE_URL}/chat/rooms`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(data),
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

export async function getMember(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/members`, {
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

export async function getMessages(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/messages`, {
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

export async function sendMessage(
  accessToken,
  roomId,
  content,
  msgType,
  fileId = null,
) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/messages`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ content, fileId, msgType: msgType }),
  })
    .then((response) => response.json())
    .then((json) => json)
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

export async function readMessage(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/read`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ id: roomId }),
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

// 내 정보 조회 (본인 employeeId 등)
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
      return json;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 채팅방 입장
export async function enterRoom(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/enter`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  }).catch((error) => {
    console.log("에러발생: " + error);
  });
}

// 채팅방 대화 요약
export async function summarizeRoom(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/summary`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => json)
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 채팅방 퇴장
export async function leaveRoom(accessToken, roomId) {
  return await fetch(`${BASE_URL}/chat/rooms/${roomId}/leave`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  }).catch((error) => {
    console.log("에러발생: " + error);
  });
}
