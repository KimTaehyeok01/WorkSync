// 직급 문자열 형식 변환
export const JOB_GRADE = {
  STAFF: "사원",
  SENIOR: "주임",
  ASSISTANT_MANAGER: "대리",
  MANAGER: "과장",
  GENERAL_MANAGER: "부장",
  DIRECTOR: "이사",
  CEO: "대표",
};

// 상태별 css 컬러
export function statusColor(status) {
  if (status === "ACTIVE") return "#48BB78";
  if (status === "AWAY") return "#F6AD55";
  if (status === "INACTIVE") return "#A0AEC0";
  return "#A0AEC0";
}

// ISO 문자열(2026-06-01T11:01:46)을 "오전 11:01" 형식으로 변환
export function formatTime(isoString) {
  if (!isoString) return "";
  const day = new Date(isoString);
  const h = day.getHours();
  const m = String(day.getMinutes()).padStart(2, "0");
  return h < 12 ? `오전 ${h || 12}:${m}` : `오후 ${h - 12 || 12}:${m}`;
}

// ISO 문자열(2026-06-01T11:01:46)을 "0월 0일" 형식으로 변환
export function formatDay(isoString) {
  if (!isoString) return "";
  const day = new Date(isoString);
  const m = day.getMonth() + 1;
  const d = day.getDate();
  return `${m}월 ${d}일`;
}

// ISO 문자열(2026-06-01T11:01:46) 오늘 날짜
export function isToday(isoString) {
  if (!isoString) return "";
  const day = new Date(isoString);
  const today = new Date();
  return (
    day.getFullYear() === today.getFullYear() &&
    day.getMonth() === today.getMonth() &&
    day.getDate() === today.getDate()
  );
}

// 파일명에서 확장자 추출 - "report.pdf" -> "pdf"
// export function getExt(filename) {
//   return filename.split(".").pop().toLowerCase();
// }

// 바이트 단위 파일 크기를 읽기 좋게 변환 - 1048576 -> "1.0MB"
// export function formatSize(bytes) {
//   if (bytes < 1024) return `${bytes} B`;
//   if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
//   return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
// }

// 확장자별 색상과 라벨 정의 - 파일 아이콘 뱃지에 사용
// export const EXT_MAP = {
//   pdf: { color: "#F40F02", label: "PDF" },
//   xlsx: { color: "#217346", label: "XLS" },
//   xls: { color: "#217346", label: "XLS" },
//   docx: { color: "#2B5797", label: "DOC" },
//   pptx: { color: "#D04423", label: "PPT" },
//   png: { color: "#0EA5E9", label: "IMG" },
// };

// 파일명을 받아서 해당 확장자의 색상+라벨 반환
// export function getFileMeta(filename) {
//   const ext = getExt(filename);
//   return (
//     EXT_MAP[ext] || { color: "#6B7280", label: ext.toUpperCase().slice(0, 4) }
//   );
// }
