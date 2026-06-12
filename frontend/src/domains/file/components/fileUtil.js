// 확장자별 색상과 라벨 정의 - 파일 아이콘 뱃지에 사용
export const EXT_MAP = {
  pdf: { color: "#F40F02", label: "PDF" },
  xlsx: { color: "#217346", label: "XLS" },
  docx: { color: "#2B5797", label: "DOC" },
  pptx: { color: "#D04423", label: "PPT" },
  png: { color: "#0EA5E9", label: "IMG" },
  jpg: { color: "#0EA5E9", label: "IMG" },
};

// 파일명에서 확장자 추출 - "report.pdf" -> "pdf"
export function getExt(filename) {
  return filename.split(".").pop().toLowerCase();
}

// 바이트 단위 파일 크기를 읽기 좋게 변환 - 1048576 -> "1.0MB"
export function getSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// 파일명을 받아서 해당 확장자의 색상+라벨 반환
export function getFileMeta(filename) {
  const ext = getExt(filename);
  return (
    EXT_MAP[ext] || { color: "#6B7280", label: ext.toUpperCase().slice(0, 4) }
  );
}
