// 날짜를 화면·API용 문자열로 변환하는 유틸

// 로컬 날짜를 yyyy-MM-dd 로 변환 (toISOString은 UTC라 날짜가 밀릴 수 있음)
export function toDateString(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// 해당 월을 포함하는 6주(42일)의 날짜 문자열 목록 — 일요일 시작
export function buildMonthGrid(year, month) {
  const first = new Date(year, month - 1, 1);
  return Array.from({ length: 42 }, (_, i) =>
    toDateString(new Date(year, month - 1, 1 - first.getDay() + i)),
  );
}
