// API 요청이 401(Access Token 만료)로 거절되면 토큰을 재발급해 한 번 재시도하는 fetch 인터셉터

// 동시에 여러 요청이 401을 받아도 재발급은 한 번만 하도록 진행 중인 요청을 공유한다
let refreshing = null;

// 재발급 대상에서 제외 — 로그인 실패도 401이고, 재발급 요청 자체를 다시 재발급하면 무한 반복된다
function isRetryTarget(url) {
  return url.startsWith("/api/") && !url.startsWith("/api/auth/");
}

/**
 * window.fetch를 감싸 401 응답 시 getRefresh()()로 새 Access Token을 받아 재시도한다.
 * 각 domain services가 fetch를 직접 호출하므로, 호출부를 고치지 않고 한 곳에서 처리하기 위함이다.
 * 반환값은 원래 fetch로 되돌리는 해제 함수.
 */
export function installAuthInterceptor(getRefresh) {
  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input, init = {}) => {
    const response = await originalFetch(input, init);
    const url = typeof input === "string" ? input : input.url;

    if (response.status !== 401 || !isRetryTarget(url)) {
      return response;
    }

    try {
      refreshing =
        refreshing ??
        getRefresh()().finally(() => {
          refreshing = null;
        });
      const newAccessToken = await refreshing;

      return originalFetch(input, {
        ...init,
        headers: { ...init.headers, Authorization: `Bearer ${newAccessToken}` },
      });
    } catch {
      // 재발급 실패(Refresh Token 만료) — 원래의 401 응답을 그대로 돌려준다
      return response;
    }
  };

  return () => {
    window.fetch = originalFetch;
  };
}
