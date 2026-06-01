import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Paperclip, CheckCircle, Send, X } from "lucide-react";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import {
  WSInput,
  WSSelect,
  WSTextarea,
  WSFileUploadZone,
  WSCalendarpicker,
  WSFileList,
} from "../../../components/common/FormComponents";
import s from "./TaskCreatePage.module.css";

const STATUS_OPTIONS = [
  { key: "todo", label: "대기중" },
  { key: "inProgress", label: "진행중" },
  { key: "done", label: "완료" },
];

const PROGRESS_OPTIONS = [
  { key: "0", label: "0%" },
  { key: "10", label: "10%" },
  { key: "20", label: "20%" },
  { key: "30", label: "30%" },
  { key: "40", label: "40%" },
  { key: "50", label: "50%" },
  { key: "60", label: "60%" },
  { key: "70", label: "70%" },
  { key: "80", label: "80%" },
  { key: "90", label: "90%" },
  { key: "100", label: "100%" },
];

const TOOLBAR_ITEMS = ["굵게", "기울임", "밑줄", "|", "목록"];

const MAX_SIZE_MB = 50;
const ALLOWED_EXT = [".pdf", ".pptx", ".xlsx", ".docx"];

export default function TaskNew() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: "",
    description: "",
    status: "todo",
    progress: "0",
    assignee: "",
    start_date: "",
    due_date: "",
  });
  const [files, setFiles] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [saved, setSaved] = useState(false);

  function validationFile(file) {
    const errors = [];
    const ext = "." + file.name.split(".").pop().toLowerCase();

    // 확장자 검사
    if (!ALLOWED_EXT.includes(ext)) {
      errors.push("허용하지 않은 확장자입니다.");
    }
    // 용량 검사
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      errors.push(`파일 크기가 ${MAX_SIZE_MB}MB를 초과했습니다.`);
    }

    return errors;
  }

  function addFiles(newFiles) {
    if (!newFiles || newFiles.length === 0) {
      return;
    }

    const validated = newFiles.map((item) => ({
      file: item,
      errors: validationFile(item),
    }));

    const errorFiles = validated.filter((item) => item.errors.length > 0);
    errorFiles.forEach((item) => {
      alert(`${item.file.name}: ${item.errors.join(", ")}`);
    });

    const validOnly = validated.filter((item) => item.errors.length === 0);

    setFiles((prev) => {
      const merged = [...prev, ...validOnly];
      return merged.slice(0, 10);
    });
  }

  function removeFiles(index) {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  }

  const isValid = form.title.trim().length > 0;

  function handleSubmit() {
    if (!isValid) return;
    setSubmitted(true);
    setTimeout(() => navigate("/tasks"), 1600);
  }

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} className={s.successIconGlyph} />
          </div>
          <div>
            <p className={s.successTitle}>작업이 등록되었습니다</p>
            <p className={s.successDesc}>업무 보드로 이동합니다...</p>
          </div>
          <div className={s.successBadge}>업무 보드로 이동 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <div className={s.header}>
        <div className={s.headerLeft}>
          <button onClick={() => navigate("/tasks")} className={s.backBtn}>
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>새 작업 등록</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <WSCard
            title="작업 기본 정보"
            subtitle="새로운 업무 항목의 기본 정보를 입력하세요"
          >
            <div className={s.formGrid}>
              <div className={s.row2}>
                <div>
                  <label className={s.label}>담당자</label>
                  <WSSelect
                    placeholder="담당자 선택"
                    value={form.assignee}
                    onChange={(e) =>
                      setForm((p) => ({ ...p, assignee: e.target.value }))
                    }
                    options={TEAM_MEMBERS.map((m) => ({
                      value: m.id,
                      label: `${m.name} (${m.dept}, ${m.role})`,
                    }))}
                  />
                </div>
                <div>
                  <label className={s.label}>프로젝트 기간</label>
                  <WSCalendarpicker
                    startValue={form.start_date}
                    endValue={form.due_date}
                    onStartChange={(e) =>
                      setForm((p) => ({ ...p, start_date: e.target.value }))
                    }
                    onEndChange={(e) =>
                      setForm((p) => ({ ...p, due_date: e.target.value }))
                    }
                  />
                </div>
              </div>

              <div>
                <label className={s.label}>
                  작업 제목 <span className={s.required}>*</span>
                </label>
                <WSInput
                  placeholder="작업 제목을 입력하세요"
                  value={form.title}
                  onChange={(e) =>
                    setForm((p) => ({ ...p, title: e.target.value }))
                  }
                  className={s.input}
                />
              </div>

              <div className={s.row2}>
                <div>
                  <label className={s.label}>상태</label>
                  <WSSelect
                    placeholder="상태 선택"
                    value={form.status}
                    onChange={(e) =>
                      setForm((p) => ({ ...p, status: e.target.value }))
                    }
                    options={STATUS_OPTIONS.map((m) => ({
                      value: m.key,
                      label: m.label,
                    }))}
                    className={s.select}
                  />
                </div>
                <div>
                  <label className={s.label}>진행률</label>
                  <WSSelect
                    placeholder="진행률 선택"
                    value={form.progress}
                    onChange={(e) =>
                      setForm((p) => ({ ...p, progress: e.target.value }))
                    }
                    options={PROGRESS_OPTIONS.map((m) => ({
                      value: m.key,
                      label: m.label,
                    }))}
                    className={s.select}
                  />
                </div>
              </div>
            </div>
          </WSCard>

          <WSCard
            title="상세 설명"
            subtitle="작업에 대한 상세 내용을 작성하세요"
          >
            <div className={s.toolbar}>
              {TOOLBAR_ITEMS.map((btn, i) =>
                btn === "|" ? (
                  <div key={i} className={s.toolbarSep} />
                ) : (
                  <button key={i} className={s.toolbarBtn}>
                    {btn}
                  </button>
                ),
              )}
            </div>
            <WSTextarea
              placeholder="작업에 대한 상세 설명을 입력하세요.&#10;예) 작업 배경, 목표, 완료 조건 등을 상세하게 기술해 주세요."
              value={form.description}
              onChange={(e) =>
                setForm((p) => ({
                  ...p,
                  description: e.target.value.slice(0, 2000),
                }))
              }
              className={s.textarea}
            />
          </WSCard>
        </div>

        <div className={`${s.col} ${s.colSide}`}>
          <WSCard title="첨부파일">
            <WSFileUploadZone
              onFilesAdded={addFiles}
              isDragging={isDragging}
              onDragStateChange={setIsDragging}
              icon={<Paperclip size={28} />}
              accept=".pdf,.ppt,.xlsx,.pptx"
              label="파일을 드래그하거나 클릭하여 업로드"
              helperText="PDF, DOCX, XLSX, PPTX - 최대 50MB"
            />

            <WSFileList
              files={files.map(({ file }) => file)}
              onRemove={removeFiles}
            />
          </WSCard>

          <div className={s.actionsCol}>
            <WSButton
              label="작업 등록"
              icon={<Send size={16} />}
              onClick={handleSubmit}
              disabled={!isValid}
              className={s.submitBtn}
            />
            <button onClick={() => navigate("/tasks")} className={s.cancelBtn}>
              취소하고 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
