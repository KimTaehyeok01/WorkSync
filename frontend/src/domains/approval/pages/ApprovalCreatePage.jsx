import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  X,
  ChevronDown,
  Paperclip,
  FileText,
  Trash2,
  GripVertical,
  AlertCircle,
  CheckCircle,
  Send,
  Save,
  UserPlus,
  Info,
} from "lucide-react";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import { WSCard, WSAvatar } from "../../../components/common/CommonWidgets";
import s from "./ApprovalCreatePage.module.css";

const DOC_TYPES = [
  "예산",
  "인사 정책",
  "IT 요청",
  "구매",
  "행사",
  "인사",
  "기타",
];
const PRIORITIES = [
  { value: "low", label: "낮음", color: "#6B7280", bg: "#F3F4F6" },
  { value: "medium", label: "보통", color: "#D97706", bg: "#FEF3C7" },
  { value: "high", label: "높음", color: "#DC2626", bg: "#FEE2E2" },
  { value: "urgent", label: "긴급", color: "#7C3AED", bg: "#EDE9FE" },
];
const DEPARTMENTS = [
  "경영진",
  "제품팀",
  "개발팀",
  "디자인팀",
  "마케팅팀",
  "인사팀",
  "재무팀",
];

const MOCK_TEMPLATES = [
  { id: "tpl1", name: "예산 요청 양식", type: "예산" },
  { id: "tpl2", name: "인사 정책 변경", type: "인사 정책" },
  { id: "tpl3", name: "IT 장비 구매 신청", type: "IT 요청" },
  { id: "tpl4", name: "행사 기획안", type: "행사" },
];

const fileIconColor = {
  pdf: "#EF4444",
  xlsx: "#10B981",
  pptx: "#F59E0B",
  docx: "#3B82F6",
  default: "#6B7280",
};

export default function ApprovalNew() {
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [docType, setDocType] = useState("");
  const [priority, setPriority] = useState("medium");
  const [department, setDepartment] = useState("");
  const [content, setContent] = useState("");
  const [showTemplate, setShowTemplate] = useState(false);

  const [approvers, setApprovers] = useState([
    { id: "a1", member: TEAM_MEMBERS[0], role: "최종 결재자" },
  ]);
  const [showMemberPicker, setShowMemberPicker] = useState(false);

  const [attachments, setAttachments] = useState([
    { id: "f1", name: "Q3_예산_초안.xlsx", size: "1.2 MB", type: "xlsx" },
  ]);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const [submitted, setSubmitted] = useState(false);
  const [saved, setSaved] = useState(false);

  const isValid =
    title.trim().length > 0 &&
    docType !== "" &&
    department !== "" &&
    approvers.length > 0;

  const handleAddApprover = (member) => {
    if (approvers.find((a) => a.member.id === member.id)) return;
    const roles = ["검토자", "부서장", "협조자", "최종 결재자"];
    setApprovers((prev) => [
      ...prev,
      {
        id: "a" + Date.now(),
        member,
        role: roles[Math.min(prev.length, roles.length - 1)],
      },
    ]);
    setShowMemberPicker(false);
  };

  const handleRemoveApprover = (id) =>
    setApprovers((prev) => prev.filter((a) => a.id !== id));

  const handleRoleChange = (id, role) =>
    setApprovers((prev) => prev.map((a) => (a.id === id ? { ...a, role } : a)));

  const handleMockFile = () => {
    const names = [
      "보고서_최종.pdf",
      "예산서_v2.xlsx",
      "제안서.pptx",
      "계획서.docx",
    ];
    const sizes = ["0.8 MB", "2.1 MB", "4.5 MB", "1.3 MB"];
    const types = ["pdf", "xlsx", "pptx", "docx"];
    const idx = Math.floor(Math.random() * 4);
    setAttachments((prev) => [
      ...prev,
      {
        id: "f" + Date.now(),
        name: names[idx],
        size: sizes[idx],
        type: types[idx],
      },
    ]);
  };

  const handleRemoveFile = (id) =>
    setAttachments((prev) => prev.filter((f) => f.id !== id));

  const handleSaveDraft = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const handleSubmit = () => {
    if (!isValid) return;
    setSubmitted(true);
    setTimeout(() => navigate("/approval"), 1600);
  };

  const selectedPriority = PRIORITIES.find((p) => p.value === priority);

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} color="#10B981" />
          </div>
          <div>
            <p className={s.successTitle}>결재 문서가 제출되었습니다</p>
            <p className={s.successDesc}>
              결재선에 등록된 결재자에게 알림이 발송되었습니다.
            </p>
          </div>
          <div className={s.successBadge}>전자결재 목록으로 이동 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <div className={s.headerRow}>
        <div className={s.headerLeft}>
          <button onClick={() => navigate("/approval")} className={s.backBtn}>
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>새 결재 문서 등록</h1>
            <p className={s.pageBreadcrumb}>
              홈 &nbsp;/&nbsp; 전자결재 &nbsp;/&nbsp;
              <span className={s.bcCurrent}>새 문서</span>
            </p>
          </div>
        </div>

        <div className={s.tplWrap}>
          <button
            onClick={() => setShowTemplate((v) => !v)}
            className={s.tplBtn}
            type="button"
            aria-haspopup="menu"
            aria-expanded={showTemplate}
          >
            <FileText size={14} />
            양식 불러오기
            <ChevronDown size={13} />
          </button>
          {showTemplate && (
            <div className={s.tplMenu} role="menu" aria-label="결재 양식 선택">
              <div className={s.tplMenuHeader}>
                <p className={s.tplMenuLabel}>결재 양식 선택</p>
              </div>
              {MOCK_TEMPLATES.map((tpl) => (
                <button
                  key={tpl.id}
                  className={s.tplItem}
                  onClick={() => {
                    setDocType(tpl.type);
                    setTitle(
                      tpl.name + " - " + new Date().toLocaleDateString("ko-KR"),
                    );
                    setShowTemplate(false);
                  }}
                  type="button"
                  role="menuitem"
                >
                  <FileText size={14} color="#9CA3AF" />
                  <div>
                    <p className={s.tplItemName}>{tpl.name}</p>
                    <p className={s.tplItemType}>{tpl.type}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {saved && (
        <div className={s.savedAlert}>
          <CheckCircle size={15} color="#10B981" />
          <span>임시 저장되었습니다. 목록의 임시보관함에서 확인하세요.</span>
        </div>
      )}

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <WSCard
            title="문서 기본 정보"
            subtitle="결재 문서의 기본 정보를 입력하세요"
          >
            <div className={s.formGrid}>
              <div>
                <label className={s.label}>
                  문서 제목 <span className={s.required}>*</span>
                </label>
                <input
                  type="text"
                  placeholder="결재 문서 제목을 입력하세요"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className={s.input}
                />
              </div>

              <div className={s.row2}>
                <div>
                  <label className={s.label}>
                    문서 유형 <span className={s.required}>*</span>
                  </label>
                  <div className={s.selectWrap}>
                    <select
                      value={docType}
                      onChange={(e) => setDocType(e.target.value)}
                      className={s.select}
                    >
                      <option value="">유형 선택...</option>
                      {DOC_TYPES.map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                    <ChevronDown size={14} className={s.selectChevron} />
                  </div>
                </div>
                <div>
                  <label className={s.label}>우선순위</label>
                  <div className={s.priorityList}>
                    {PRIORITIES.map((p) => {
                      const sel = priority === p.value;
                      return (
                        <button
                          key={p.value}
                          onClick={() => setPriority(p.value)}
                          className={`${s.priorityBtn} ${sel ? s.priorityBtnActive : ""}`}
                          style={{
                            "--priority-bg": p.bg,
                            "--priority-color": p.color,
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
                    요청 부서 <span className={s.required}>*</span>
                  </label>
                  <div className={s.selectWrap}>
                    <select
                      value={department}
                      onChange={(e) => setDepartment(e.target.value)}
                      className={s.select}
                    >
                      <option value="">부서 선택...</option>
                      {DEPARTMENTS.map((d) => (
                        <option key={d} value={d}>
                          {d}
                        </option>
                      ))}
                    </select>
                    <ChevronDown size={14} className={s.selectChevron} />
                  </div>
                </div>
                <div>
                  <label className={s.label}>요청일</label>
                  <input
                    type="date"
                    defaultValue={new Date().toISOString().split("T")[0]}
                    className={s.dateInput}
                  />
                </div>
              </div>
            </div>
          </WSCard>

          <WSCard
            title="문서 내용"
            subtitle="결재 문서의 상세 내용을 작성하세요"
            action={
              <span className={s.headerCount}>{content.length} / 2000자</span>
            }
          >
            <div className={s.toolbar}>
              {[
                "굵게",
                "기울임",
                "밑줄",
                "|",
                "목록",
                "번호목록",
                "|",
                "표 삽입",
                "링크",
              ].map((btn, i) =>
                btn === "|" ? (
                  <div key={i} className={s.toolbarSep} />
                ) : (
                  <button key={i} className={s.toolbarBtn}>
                    {btn}
                  </button>
                ),
              )}
            </div>
            <textarea
              placeholder="결재 문서의 상세 내용을 작성하세요.&#10;&#10;예) 요청 배경, 목적, 예산 내역, 기대 효과 등을 상세하게 기술해 주세요."
              value={content}
              onChange={(e) => setContent(e.target.value.slice(0, 2000))}
              className={s.textarea}
            />
          </WSCard>

          <WSCard
            title="첨부 파일"
            subtitle={`${attachments.length}개 파일 첨부됨`}
          >
            <div
              className={`${s.dropzone} ${isDragOver ? s.dropzoneActive : ""}`}
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragOver(true);
              }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={(e) => {
                e.preventDefault();
                setIsDragOver(false);
                handleMockFile();
              }}
              onClick={() => fileInputRef.current?.click()}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  fileInputRef.current?.click();
                }
              }}
              role="button"
              tabIndex={0}
              aria-label="결재 첨부 파일 추가"
            >
              <Paperclip
                size={28}
                className={`${s.dropzoneIcon} ${isDragOver ? s.dropzoneIconActive : ""}`}
              />
              <p className={s.dropzoneLabel}>
                파일을 드래그하거나 클릭해서 추가
              </p>
              <p className={s.dropzoneHint}>
                PDF, DOCX, XLSX, PPTX · 최대 50MB
              </p>
              <input
                ref={fileInputRef}
                type="file"
                className={s.hiddenInput}
                onChange={handleMockFile}
              />
            </div>

            {attachments.length > 0 && (
              <div className={s.fileList}>
                {attachments.map((f) => {
                  const c = fileIconColor[f.type] || fileIconColor.default;
                  return (
                    <div key={f.id} className={s.fileRow}>
                      <div
                        className={s.fileIcon}
                        style={{ "--file-bg": c + "20" }}
                      >
                        <FileText size={16} color={c} />
                      </div>
                      <div className={s.fileBody}>
                        <p className={s.fileName}>{f.name}</p>
                        <p className={s.fileSize}>{f.size}</p>
                      </div>
                      <button
                        onClick={() => handleRemoveFile(f.id)}
                        className={s.fileDel}
                        type="button"
                        aria-label={`${f.name} 삭제`}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </WSCard>
        </div>

        <div className={`${s.col} ${s.colSide}`}>
          <WSCard
            title="결재선 설정"
            subtitle="결재자를 순서대로 추가하세요"
            action={
              <button
                onClick={() => setShowMemberPicker((v) => !v)}
                className={s.addBtn}
                type="button"
                aria-haspopup="listbox"
                aria-expanded={showMemberPicker}
              >
                <UserPlus size={13} /> 추가
              </button>
            }
          >
            <div className={s.applicantRow}>
              <WSAvatar
                src={TEAM_MEMBERS[3].avatar}
                name={TEAM_MEMBERS[3].name}
                size={30}
              />
              <div className={s.applicantBody}>
                <p className={s.applicantName}>{TEAM_MEMBERS[3].name}</p>
                <p className={s.applicantRole}>{TEAM_MEMBERS[3].role}</p>
              </div>
              <span className={s.applicantBadge}>기안자</span>
            </div>

            {approvers.length > 0 && (
              <div className={s.connector}>
                <div />
              </div>
            )}

            <div className={s.approverList}>
              {approvers.map((a, idx) => (
                <div key={a.id}>
                  <div className={s.approverRow}>
                    <GripVertical size={14} className={s.grip} />
                    <WSAvatar
                      src={a.member.avatar}
                      name={a.member.name}
                      size={30}
                    />
                    <div className={s.approverBody}>
                      <p className={s.approverName}>{a.member.name}</p>
                      <select
                        value={a.role}
                        onChange={(e) => handleRoleChange(a.id, e.target.value)}
                        className={s.roleSelect}
                      >
                        {["검토자", "부서장", "협조자", "최종 결재자"].map(
                          (r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ),
                        )}
                      </select>
                    </div>
                    <button
                      onClick={() => handleRemoveApprover(a.id)}
                      className={s.approverDel}
                      type="button"
                      aria-label={`${a.member.name} 결재자 제거`}
                    >
                      <X size={13} />
                    </button>
                  </div>
                  {idx < approvers.length - 1 && (
                    <div className={s.connector}>
                      <div />
                    </div>
                  )}
                </div>
              ))}
            </div>

            {showMemberPicker && (
              <div
                className={s.pickerBox}
                role="listbox"
                aria-label="결재자 선택"
              >
                <div className={s.pickerHeader}>
                  <p className={s.pickerHeaderLabel}>팀원 선택</p>
                </div>
                {TEAM_MEMBERS.filter((m) => m.id !== 4).map((member) => {
                  const already = approvers.some(
                    (a) => a.member.id === member.id,
                  );
                  return (
                    <button
                      key={member.id}
                      disabled={already}
                      onClick={() => handleAddApprover(member)}
                      className={s.pickerItem}
                      type="button"
                      role="option"
                      aria-selected={already}
                    >
                      <WSAvatar
                        src={member.avatar}
                        name={member.name}
                        size={28}
                      />
                      <div className={s.pickerBody}>
                        <p className={s.pickerName}>{member.name}</p>
                        <p className={s.pickerRole}>{member.role}</p>
                      </div>
                      {already && <CheckCircle size={14} color="#10B981" />}
                    </button>
                  );
                })}
              </div>
            )}

            {approvers.length === 0 && (
              <div className={s.warn}>
                <AlertCircle size={14} color="#D97706" />
                <p className={s.warnText}>결재자를 최소 1명 추가하세요</p>
              </div>
            )}
          </WSCard>

          <WSCard title="등록 요약">
            <div className={s.summaryGrid}>
              <SummaryRow
                label="문서 제목"
                value={title || "—"}
                empty={!title}
              />
              <SummaryRow
                label="문서 유형"
                value={docType || "—"}
                empty={!docType}
              />
              <SummaryRow
                label="우선순위"
                value={
                  <span
                    className={s.priorityBadge}
                    style={{
                      "--badge-bg": selectedPriority.bg,
                      "--badge-color": selectedPriority.color,
                    }}
                  >
                    {selectedPriority.label}
                  </span>
                }
              />
              <SummaryRow
                label="요청 부서"
                value={department || "—"}
                empty={!department}
              />
              <SummaryRow
                label="결재자 수"
                value={`${approvers.length}명`}
                empty={approvers.length === 0}
              />
              <SummaryRow label="첨부 파일" value={`${attachments.length}개`} />
              <div className={s.summaryDivider}>
                <div className={s.summaryNote}>
                  <Info size={13} className={s.summaryNoteIcon} />
                  <p className={s.summaryNoteText}>
                    제출 후에는 기안 취소 또는 관리자 권한이 필요합니다.
                  </p>
                </div>
              </div>
            </div>
          </WSCard>

          <div className={s.actionsCol}>
            <button
              onClick={handleSubmit}
              disabled={!isValid}
              className={s.submitBtn}
            >
              <Send size={16} />
              결재 요청 제출
            </button>
            <button onClick={handleSaveDraft} className={s.draftBtn}>
              <Save size={15} />
              임시 저장
            </button>
            <button
              onClick={() => navigate("/approval")}
              className={s.cancelBtn}
            >
              취소하고 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

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
