import s from "../pages/ChatPage.module.css";

// 채팅창 안에서 파일 메시지를 보여주는 말풍선 컴포넌트
export function FileBubble({ file }) {
  const meta = getFileMeta(file.name); // 확장자에 맞는 색상 + 라벨 가져오기

  // 브라우저에서 직접 다운로드 - 백엔드 없이도 동작
  function handleDownload() {
    const url = URL.createObjectURL(file.raw); // 업로드한 File 객체로 임시 URL 생성
    const a = document.createElement("a");
    a.href = url;
    a.download = file.name;
    a.click();
    URL.revokeObjectURL(url); // 메모리 누수 방지를 위해 URL 즉시 해제
  }

  return (
    // 버튼 전체를 클릭 가능하게 — Download 버튼 제거
    <div
      className={s.fileBubble}
      onClick={handleDownload}
      style={{ cursor: "pointer" }}
      role="button"
      aria-label={`${file.name} 다운로드`}
    >
      <div className={s.fileBubbleIcon} style={{ "--file-color": meta.color }}>
        {meta.label}
      </div>
      <div className={s.fileBubbleBody}>
        <p className={s.fileBubbleName}>{file.name}</p>
        <p className={s.fileBubbleSize}>{file.size}</p>
      </div>
      {/* Download 버튼 삭제 */}
    </div>
  );
}
