const BASE_URL = "http://localhost:8080/api";

// 알림 목록 조회
export async function getNotifications(accessToken) {
  return await fetch(`${BASE_URL}/notifications`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => {
      // console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 안읽은 알림갯수 조회
export async function getUnreadCount(accessToken) {
  return await fetch(`${BASE_URL}/notifications/unread-count`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  })
    .then((response) => response.json())
    .then((json) => {
      // console.log(json);
      return json.data;
    })
    .catch((error) => {
      console.log("에러발생: " + error);
    });
}

// 알림 읽음 처리
export async function putNotifications(accessToken, notif) {
  return await fetch(`${BASE_URL}/notifications/read`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      targetType: notif.targetType,
      targetId: notif.targetId,
    }),
  }).catch((error) => {
    console.log("에러발생: " + error);
  });
}
