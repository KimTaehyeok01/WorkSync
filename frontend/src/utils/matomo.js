// SPA 화면 이동을 Matomo 페이지뷰로 기록하는 유틸 (index.html의 _paq 스니펫이 로드되어 있어야 동작)
export function trackPageView(pathname) {
  const paq = (window._paq = window._paq || []);
  paq.push(["setCustomUrl", window.location.origin + pathname]);
  paq.push(["setDocumentTitle", document.title]);
  paq.push(["trackPageView"]);
}

// 사용자 행동을 Matomo 이벤트로 기록한다 (name 이 없으면 인자를 생략)
export function trackEvent(category, action, name) {
  const paq = (window._paq = window._paq || []);
  paq.push(
    name
      ? ["trackEvent", category, action, name]
      : ["trackEvent", category, action],
  );
}
