import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft, Bold, Italic, Underline, List, Link, AlignLeft,
  Paperclip, CheckCircle, Image, Trash2
} from "lucide-react";
import { WSCard } from "../../../components/common/CommonWidgets";
import s from "./BoardCreatePage.module.css";

const CATEGORY_OPTIONS = [
  { value: "notice", label: "공지사항",   color: "#EF4444" },
  { value: "dept",   label: "부서 게시판", color: "#8B5CF6" },
  { value: "free",   label: "자유 게시판", color: "#10B981" },
];

const fileIconColor = {
  PDF: "#EF4444", XLSX: "#10B981", PPTX: "#F59E0B",
  DOCX: "#3B82F6", PNG: "#06B6D4", ZIP: "#F97316", default: "#6B7280",
};

const TOOLBAR_ITEMS = [
  { Icon: Bold, label: "굵게" },
  { Icon: Italic, label: "기울임" },
  { Icon: Underline, label: "밑줄" },
  { Icon: AlignLeft, label: "정렬" },
  { Icon: List, label: "목록" },
  { Icon: Link, label: "링크" },
  { Icon: Image, label: "이미지" },
];

export default function BoardNew() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("free");
  const [content, setContent] = useState("");
  const [files, setFiles] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const MAX_CHARS = 3000;

  const isValid = title.trim().length > 0 && category !== "" && content.trim().length > 0;

  function handleFileDrop(e) {
    e.preventDefault();
    setIsDragging(false);
    addFiles(Array.from(e.dataTransfer.files));
  }

  function addFiles(newFiles) {
    const mapped = newFiles.map((f, i) => ({
      id: `file-${Date.now()}-${i}`,
      name: f.name,
      size: f.size > 1024 * 1024 ? `${(f.size / 1024 / 1024).toFixed(1)} MB` : `${(f.size / 1024).toFixed(0)} KB`,
      type: f.name.split(".").pop()?.toUpperCase() || "FILE",
    }));
    setFiles((prev) => [...prev, ...mapped].slice(0, 10));
  }

  function handleFileInput(e) {
    if (e.target.files) addFiles(Array.from(e.target.files));
  }

  function handleSubmit() {
    if (!isValid) return;
    setSubmitted(true);
    setTimeout(() => navigate("/board"), 1800);
  }

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} className={s.successIconGlyph} />
          </div>
          <div className={s.successCopy}>
            <p className={s.successTitle}>게시글이 등록되었습니다</p>
            <p className={s.successDesc}>게시판으로 이동합니다...</p>
          </div>
          <div className={s.successBadge}>게시판으로 이동 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <button onClick={() => navigate("/board")} className={s.backBtn}>
        <ArrowLeft size={18} />
        게시글 작성 등록
      </button>

      <div className={s.layout}>
        <div className={s.colMain}>
          <WSCard title="게시글 기본 정보" subtitle="게시판 분류와 제목을 입력하세요">
            <div className={s.formGrid}>
              <div>
                <label className={s.label}>
                  게시판 분류 <span className={s.required}>*</span>
                </label>
                <div className={s.catRow}>
                  {CATEGORY_OPTIONS.map((opt) => {
                    const active = category === opt.value;
                    return (
                      <button
                        key={opt.value}
                        onClick={() => setCategory(opt.value)}
                        className={`${s.catBtn} ${active ? s.catBtnActive : ""}`}
                        style={{
                          "--cat-color": opt.color,
                          "--cat-bg": opt.color + "15",
                        }}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className={s.label}>
                  제목 <span className={s.required}>*</span>
                </label>
                <input
                  type="text"
                  placeholder="게시글 제목을 입력하세요"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={100}
                  className={s.input}
                />
                <div className={s.charCount}>{title.length}/100</div>
              </div>
            </div>
          </WSCard>

          <div className={s.contentCardWrap}>
            <WSCard
              title="내용"
              subtitle="게시글 본문을 작성하세요"
              action={
                <span className={`${s.contentCount} ${content.length > MAX_CHARS * 0.9 ? s.contentCountWarn : ""}`}>
                  {content.length}/{MAX_CHARS}
                </span>
              }
            >
              <div className={s.toolbar}>
                {TOOLBAR_ITEMS.map(({ Icon, label }) => (
                  <button key={label} title={label} className={s.toolbarBtn}>
                    <Icon size={14} />
                  </button>
                ))}
              </div>
              <textarea
                placeholder="내용을 입력하세요. 팀원들과 공유하고 싶은 내용을 자유롭게 작성해 주세요."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                maxLength={MAX_CHARS}
                className={s.textarea}
              />
            </WSCard>
          </div>
        </div>

        <div className={s.colSide}>
          <WSCard title="첨부 파일" subtitle={`${files.length}개 파일 첨부됨`}>
            <div
              onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={handleFileDrop}
              onClick={() => fileInputRef.current?.click()}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  fileInputRef.current?.click();
                }
              }}
              className={`${s.dropzone} ${isDragging ? s.dropzoneActive : ""}`}
              role="button"
              tabIndex={0}
              aria-label="게시글 첨부 파일 추가"
            >
              <Paperclip size={28} className={`${s.dropzoneIcon} ${isDragging ? s.dropzoneIconActive : ""}`} />
              <p className={s.dropzoneText}>
                파일을 드래그하거나 <span className={s.dropzoneAccent}>클릭하여 업로드</span>
              </p>
              <p className={s.dropzoneHint}>PDF, DOCX, XLSX, PPTX - 최대 50MB</p>
            </div>
            <input ref={fileInputRef} type="file" multiple className={s.hiddenInput} onChange={handleFileInput} />

            {files.length > 0 && (
              <div className={s.fileList}>
                {files.map((f) => (
                  <div key={f.id} className={s.fileRow}>
                    <div className={s.fileIcon} style={{ "--file-color": fileIconColor[f.type] || fileIconColor.default }}>
                      {f.type}
                    </div>
                    <div className={s.fileBody}>
                      <p className={s.fileName}>{f.name}</p>
                      <p className={s.fileSize}>{f.size}</p>
                    </div>
                    <button
                      onClick={() => setFiles(files.filter((x) => x.id !== f.id))}
                      className={s.fileDel}
                      title="삭제"
                      type="button"
                      aria-label={`${f.name} 삭제`}
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className={s.actionsCol}>
              <button onClick={handleSubmit} disabled={!isValid} className={s.submitBtn}>
                등록
              </button>
              <button onClick={() => navigate("/board")} className={s.cancelBtn}>
                취소하고 돌아가기
              </button>
            </div>
          </WSCard>
        </div>
      </div>
    </div>
  );
}
