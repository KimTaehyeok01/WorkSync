// 날짜를 화면·API용 문자열로 변환하는 유틸

// 로컬 날짜를 yyyy-MM-dd 로 변환 (toISOString은 UTC라 날짜가 밀릴 수 있음)
export function toDateString(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// ISO 일시 문자열(yyyy-MM-ddTHH:mm:ss)에서 HH:mm 만 잘라낸다
export function formatTime(dateTime, fallback = "-") {
  return dateTime ? dateTime.slice(11, 16) : fallback;
}

// ISO 일시 문자열을 "yyyy-MM-dd HH:mm" 으로
export function formatDateTime(dateTime) {
  return dateTime ? `${dateTime.slice(0, 10)} ${dateTime.slice(11, 16)}` : "";
}

// 오늘이면 시각만, 올해면 월·일, 그 외에는 연·월·일 — 메일·알림 목록용
export function formatRelative(dateTime) {
  if (!dateTime) return "";
  const today = toDateString(new Date());
  if (dateTime.slice(0, 10) === today) return dateTime.slice(11, 16);
  if (dateTime.slice(0, 4) === today.slice(0, 4)) return dateTime.slice(5, 10).replace("-", ".");
  return dateTime.slice(0, 10).replaceAll("-", ".");
}

// CSV 파일로 내려받기 — 엑셀에서 한글이 깨지지 않도록 BOM 을 붙인다
export function downloadCsv(fileName, rows) {
  const escape = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;
  const csv = rows.map((row) => row.map(escape).join(",")).join("\r\n");
  const url = URL.createObjectURL(
    new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

// 해당 월을 포함하는 6주(42일)의 날짜 문자열 목록 — 일요일 시작
export function buildMonthGrid(year, month) {
  const first = new Date(year, month - 1, 1);
  return Array.from({ length: 42 }, (_, i) =>
    toDateString(new Date(year, month - 1, 1 - first.getDay() + i)),
  );
}
