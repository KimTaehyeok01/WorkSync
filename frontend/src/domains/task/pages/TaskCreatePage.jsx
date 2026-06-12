import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Paperclip, CheckCircle, Send, X } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { createTask, getEmployees, getMyInfo } from "../services/taskApi";
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
import useFileUpload from "../../../hooks/useFileUpload";
import { saveFile, deleteFile } from "../../file/services/FileApi";

const STATUS_OPTIONS = [
  { key: "TODO", label: "대기중" },
  { key: "IN_PROGRESS", label: "진행중" },
  { key: "DONE", label: "완료" },
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
  const { accessToken } = useAuthContext();
  const [members, setMembers] = useState([]);
  const [role, setRole] = useState(null);
  const [form, setForm] = useState({
    title: "",
    description: "",
    status: "TODO",
    progress: "0",
    assigneeId: "",
    departmentId: null,
    startDate: "",
    dueDate: "",
  });
  const [submitted, setSubmitted] = useState(false);
  const [saved, setSaved] = useState(false);
  const [myDepartmentId, setMyDepartmentId] = useState(null);
  const [myId, setMyId] = useState(null);

  useEffect(() => {
    if (!accessToken) return;

    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setMyId(data.id);
      setRole(data.role);
      setMyDepartmentId(data.departmentId);
    });

    // 전체 직원 호출
    getEmployees(accessToken).then((data) => {
      if (!data) return;
      setMembers(data);
    });
  }, [accessToken]);

  const isValid = form.title.trim().length > 0;
  const isTitleTooLong = form.title.length > 30;

  // 파일 선언
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "TASK");

  async function handleSubmit() {
    if (!isValid) return;

    const data = {
      title: form.title,
      description: form.description,
      status: form.status,
      progress: Number(form.progress),
      assigneeId: Number(form.assigneeId),
      departmentId: form.departmentId,
      startDate: form.startDate,
      dueDate: form.dueDate,
    };

    try {
      const response = await createTask(accessToken, data);
      const taskId = Number(response.id);

      // 파일 저장
      if (uploadedFile?.filePath) {
        await saveFile(accessToken, {
          ...uploadedFile,
          refType: "TASK",
          refId: taskId,
        });
      }

      setSubmitted(true);

      setTimeout(() => navigate("/tasks"), 1800);
    } catch (error) {
      console.error("업무 등록 실패", error);
    }

    // 파일 초기화
    clearFiles();
  }

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} className={s.successIconGlyph} />
          </div>
          <div>
            <p className={s.successTitle}>업무 등록되었습니다</p>
            <p className={s.successDesc}>업무 목록으로 이동합니다...</p>
          </div>
          <div className={s.successBadge}>업무 목록으로 이동 중...</div>
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
            <h1 className={s.pageTitle}>새 업무 등록</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <WSCard
            title="업무 기본 정보"
            subtitle="새로운 업무 항목의 기본 정보를 입력하세요"
          >
            <div className={s.formGrid}>
              <div className={s.row2}>
                <div>
                  <label className={s.label}>담당자</label>
                  <WSSelect
                    placeholder="담당자 선택"
                    value={form.assigneeId}
                    onChange={(e) => {
                      const selected = members.find(
                        (m) => m.id === Number(e.target.value),
                      );
                      setForm((p) => ({
                        ...p,
                        assigneeId: Number(e.target.value),
                        departmentId: selected?.departmentId ?? null,
                      }));
                    }}
                    options={(role === "ADMIN"
                      ? members.filter((m) => m.id !== myId)
                      : members.filter(
                          (m) =>
                            m.departmentId === myDepartmentId && m.id !== myId,
                        )
                    ).map((m) => ({
                      value: m.id,
                      label: `${m.name} (${m.departmentName}, ${m.jobGrade})`,
                    }))}
                  />
                </div>
                <div>
                  <label className={s.label}>프로젝트 기간</label>
                  <WSCalendarpicker
                    startValue={form.start_date}
                    endValue={form.due_date}
                    onStartChange={(e) =>
                      setForm((p) => ({ ...p, startDate: e.target.value }))
                    }
                    onEndChange={(e) =>
                      setForm((p) => ({ ...p, dueDate: e.target.value }))
                    }
                  />
                </div>
              </div>

              <div>
                <label className={s.label}>
                  업무 제목 <span className={s.required}>*</span>
                </label>
                <WSInput
                  placeholder="업무 제목을 입력하세요"
                  value={form.title}
                  onChange={(e) =>
                    setForm((p) => ({ ...p, title: e.target.value }))
                  }
                  className={s.input}
                />
                {isTitleTooLong && (
                  <p
                    style={{
                      color: `red`,
                      fontSize: `12px`,
                      marginTop: `4px`,
                    }}
                  >
                    제목을 30자 이내로 입력해주세요.
                  </p>
                )}
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
            subtitle="업무에 대한 상세 내용을 작성하세요"
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
              placeholder="업무에 대한 상세 설명을 입력하세요.&#10;예) 업무 배경, 목표, 완료 조건 등을 상세하게 기술해 주세요."
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
              label="업무 등록"
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
