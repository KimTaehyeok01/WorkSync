import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft, Flag, Calendar, User,
  Plus, X, ChevronDown, CheckCircle, Info, Save, Send
} from "lucide-react";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import { WSCard, WSAvatar } from "../../../components/common/CommonWidgets";
import s from "./TaskCreatePage.module.css";

const PRIORITIES = [
  { id: "urgent", label: "긴급", color: "#BE185D", bg: "#FCE7F3" },
  { id: "high",   label: "높음", color: "#DC2626", bg: "#FEE2E2" },
  { id: "medium", label: "보통", color: "#D97706", bg: "#FEF3C7" },
  { id: "low",    label: "낮음", color: "#1D4ED8", bg: "#DBEAFE" },
];

const STATUSES = [
  { id: "todo",       label: "할 일",   color: "#6B7280", bg: "#F3F4F6" },
  { id: "inProgress", label: "진행 중", color: "#1A73E8", bg: "#DBEAFE" },
  { id: "review",     label: "검토 중", color: "#8B5CF6", bg: "#EDE9FE" },
  { id: "done",       label: "완료",    color: "#10B981", bg: "#D1FAE5" },
];

const SUGGESTED_TAGS = ["Frontend", "Backend", "Design", "API", "Bug", "Feature", "Docs", "DevOps", "Security", "UX"];

const TOOLBAR_ITEMS = ["굵게", "기울임", "밑줄", "|", "목록", "번호목록", "|", "링크"];

function SummaryRow({ label, value, empty }) {
  return (
    <div className={s.summaryRow}>
      <span className={s.summaryLabel}>{label}</span>
      <span className={`${s.summaryValue} ${empty ? s.summaryValueEmpty : ""}`}>
        {value}
      </span>
    </div>
  );
}

export default function TaskNew() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: "",
    description: "",
    status: "todo",
    priority: "medium",
    assignee: "",
    due: "",
    sprint: "스프린트 7",
    tags: [],
    newTag: "",
  });
  const [submitted, setSubmitted] = useState(false);
  const [saved, setSaved] = useState(false);

  const isValid = form.title.trim().length > 0;

  function addTag(tag) {
    if (tag && !form.tags.includes(tag)) {
      setForm((p) => ({ ...p, tags: [...p.tags, tag], newTag: "" }));
    }
  }
  function removeTag(tag) {
    setForm((p) => ({ ...p, tags: p.tags.filter((t) => t !== tag) }));
  }
  function handleSubmit() {
    if (!isValid) return;
    setSubmitted(true);
    setTimeout(() => navigate("/tasks"), 1600);
  }
  function handleSaveDraft() {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  }

  const selectedPriority = PRIORITIES.find((p) => p.id === form.priority);
  const selectedStatus   = STATUSES.find((st) => st.id === form.status);
  const selectedAssignee = TEAM_MEMBERS.find((m) => String(m.id) === form.assignee);

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
            <p className={s.pageBreadcrumb}>
              홈 &nbsp;/&nbsp; 업무 보드 &nbsp;/&nbsp;
              <span className={s.bcCurrent}>새 작업</span>
            </p>
          </div>
        </div>
      </div>

      {saved && (
        <div className={s.savedAlert}>
          <CheckCircle size={15} className={s.savedAlertIcon} />
          <span>임시 저장되었습니다.</span>
        </div>
      )}

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <WSCard title="작업 기본 정보" subtitle="새로운 업무 항목의 기본 정보를 입력하세요">
            <div className={s.formGrid}>
              <div>
                <label className={s.label}>
                  작업 제목 <span className={s.required}>*</span>
                </label>
                <input
                  type="text"
                  placeholder="작업 제목을 입력하세요"
                  value={form.title}
                  onChange={(e) => setForm((p) => ({ ...p, title: e.target.value }))}
                  className={s.input}
                />
              </div>

              <div className={s.row2}>
                <div>
                  <label className={s.label}>상태</label>
                  <div className={s.chipRow}>
                    {STATUSES.map((st) => {
                      const active = form.status === st.id;
                      return (
                        <button
                          key={st.id}
                          type="button"
                          onClick={() => setForm((p) => ({ ...p, status: st.id }))}
                          className={`${s.chipBtn} ${active ? s.chipBtnActive : ""}`}
                          style={{
                            "--chip-bg": st.bg,
                            "--chip-color": st.color,
                          }}
                        >
                          {st.label}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <div>
                  <label className={s.label}>
                    <Flag size={12} /> 우선순위
                  </label>
                  <div className={s.chipRow}>
                    {PRIORITIES.map((p) => {
                      const active = form.priority === p.id;
                      return (
                        <button
                          key={p.id}
                          type="button"
                          onClick={() => setForm((prev) => ({ ...prev, priority: p.id }))}
                          className={`${s.chipBtn} ${active ? s.chipBtnActive : ""}`}
                          style={{
                            "--chip-bg": p.bg,
                            "--chip-color": p.color,
                          }}
                        >
                          {p.label}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className={s.row2}>
                <div>
                  <label className={s.label}>
                    <User size={12} /> 담당자
                  </label>
                  <div className={s.selectWrap}>
                    <select
                      value={form.assignee}
                      onChange={(e) => setForm((p) => ({ ...p, assignee: e.target.value }))}
                      className={s.select}
                    >
                      <option value="">담당자 선택</option>
                      {TEAM_MEMBERS.map((m) => (
                        <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                      ))}
                    </select>
                    <ChevronDown size={14} className={s.selectChevron} />
                  </div>
                </div>
                <div>
                  <label className={s.label}>
                    <Calendar size={12} /> 마감일
                  </label>
                  <input
                    type="date"
                    value={form.due}
                    onChange={(e) => setForm((p) => ({ ...p, due: e.target.value }))}
                    className={s.dateInput}
                  />
                </div>
              </div>

              <div>
                <label className={s.label}>스프린트</label>
                <div className={s.selectWrap}>
                  <select
                    value={form.sprint}
                    onChange={(e) => setForm((p) => ({ ...p, sprint: e.target.value }))}
                    className={s.select}
                  >
                    <option>스프린트 7</option>
                    <option>스프린트 8</option>
                    <option>백로그</option>
                  </select>
                  <ChevronDown size={14} className={s.selectChevron} />
                </div>
              </div>
            </div>
          </WSCard>

          <WSCard
            title="상세 설명"
            subtitle="작업에 대한 상세 내용을 작성하세요"
            action={<span className={s.charCount}>{form.description.length} / 2000자</span>}
          >
            <div className={s.toolbar}>
              {TOOLBAR_ITEMS.map((btn, i) =>
                btn === "|"
                  ? <div key={i} className={s.toolbarSep} />
                  : <button key={i} className={s.toolbarBtn}>{btn}</button>
              )}
            </div>
            <textarea
              placeholder="작업에 대한 상세 설명을 입력하세요.&#10;&#10;예) 작업 배경, 목표, 완료 조건 등을 상세하게 기술해 주세요."
              value={form.description}
              onChange={(e) => setForm((p) => ({ ...p, description: e.target.value.slice(0, 2000) }))}
              className={s.textarea}
            />
          </WSCard>

          <WSCard title="태그" subtitle="작업 분류를 위한 태그를 추가하세요">
            {form.tags.length > 0 && (
              <div className={s.tagAppliedRow}>
                {form.tags.map((tag) => (
                  <span key={tag} className={s.tagApplied}>
                    {tag}
                    <button type="button" onClick={() => removeTag(tag)} className={s.tagAppliedBtn}>
                      <X size={10} />
                    </button>
                  </span>
                ))}
              </div>
            )}
            <div className={s.tagSuggRow}>
              {SUGGESTED_TAGS.filter((t) => !form.tags.includes(t)).map((tag) => (
                <button key={tag} type="button" onClick={() => addTag(tag)} className={s.tagSugg}>
                  <Plus size={10} /> {tag}
                </button>
              ))}
            </div>
            <div className={s.tagInputRow}>
              <input
                type="text"
                placeholder="직접 입력..."
                value={form.newTag}
                onChange={(e) => setForm((p) => ({ ...p, newTag: e.target.value }))}
                onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); addTag(form.newTag); } }}
                className={s.tagInput}
              />
              <button type="button" onClick={() => addTag(form.newTag)} className={s.tagAddBtn}>
                추가
              </button>
            </div>
          </WSCard>
        </div>

        <div className={`${s.col} ${s.colSide}`}>
          {selectedAssignee && (
            <WSCard title="담당자">
              <div className={s.assigneePreview}>
                <WSAvatar src={selectedAssignee.avatar} name={selectedAssignee.name} size={36} />
                <div className={s.assigneePreviewBody}>
                  <p className={s.assigneeName}>{selectedAssignee.name}</p>
                  <p className={s.assigneeRole}>{selectedAssignee.role}</p>
                </div>
              </div>
            </WSCard>
          )}

          <WSCard title="등록 요약">
            <div className={s.summaryGrid}>
              <SummaryRow label="작업 제목" value={form.title || "—"} empty={!form.title} />
              <SummaryRow
                label="상태"
                value={
                  <span className={s.priorityBadge} style={{ "--badge-bg": selectedStatus.bg, "--badge-color": selectedStatus.color }}>
                    {selectedStatus.label}
                  </span>
                }
              />
              <SummaryRow
                label="우선순위"
                value={
                  <span className={s.priorityBadge} style={{ "--badge-bg": selectedPriority.bg, "--badge-color": selectedPriority.color }}>
                    {selectedPriority.label}
                  </span>
                }
              />
              <SummaryRow label="담당자" value={selectedAssignee ? selectedAssignee.name : "—"} empty={!selectedAssignee} />
              <SummaryRow label="마감일" value={form.due || "—"} empty={!form.due} />
              <SummaryRow label="스프린트" value={form.sprint} />
              <SummaryRow label="태그 수" value={`${form.tags.length}개`} empty={form.tags.length === 0} />
              <div className={s.summaryDivider}>
                <div className={s.summaryNote}>
                  <Info size={13} className={s.summaryNoteIcon} />
                  <p className={s.summaryNoteText}>
                    등록 후 업무 보드에서 상태를 변경하거나 댓글을 추가할 수 있습니다.
                  </p>
                </div>
              </div>
            </div>
          </WSCard>

          <div className={s.actionsCol}>
            <button onClick={handleSubmit} disabled={!isValid} className={s.submitBtn}>
              <Send size={16} />
              작업 등록
            </button>
            <button onClick={handleSaveDraft} className={s.draftBtn}>
              <Save size={15} />
              임시 저장
            </button>
            <button onClick={() => navigate("/tasks")} className={s.cancelBtn}>
              취소하고 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
