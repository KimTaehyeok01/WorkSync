import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import ApprovalFormPanel from "../components/ApprovalFormPanel";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import { WSCard, WSAvatar } from "../../../components/common/CommonWidgets";
import {
  WSFileUploadZone,
  WSFileList,
  WSSelect,
} from "../../../components/common/FormComponents";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";
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
} from "lucide-react";
import {
  getMyInfo,
  getForms,
  getEmployees,
  createApproval,
  getLeaveBalance,
} from "../services/approvalApi";
import s from "./ApprovalCreatePage.module.css";

export default function ApprovalNew() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [docType, setDocType] = useState("");
  const [priority, setPriority] = useState("medium");
  const [department, setDepartment] = useState("");
  const [content, setContent] = useState("");
  const [showTemplate, setShowTemplate] = useState(false);
  const [templates, setTemplates] = useState(false);
  const [formValues, setFormValues] = useState({});
  const [employees, setEmployees] = useState([]);
  const [myInfo, setMyInfo] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [balance, setBalance] = useState(null);
  const [approvers, setApprovers] = useState([]);
  const [showMemberPicker, setShowMemberPicker] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const [submitted, setSubmitted] = useState(false);
  const [saved, setSaved] = useState(false);
  const { accessToken } = useAuthContext();
  const validateRef = useRef(null);
  const [selectedForm, setSelectedForm] = useState({
    formType: "EXPENSE",
    formName: "지출결의서",
  });
  const [attachments, setAttachments] = useState([
    { id: "f1", name: "Q3_예산_초안.xlsx", size: "1.2 MB", type: "xlsx" },
  ]);
  const isValid =
    title.trim().length > 0 && docType !== "" && approvers.length > 0;

  useEffect(() => {
    if (!accessToken) return;

    getMyInfo(accessToken).then((data) => {
      setMyInfo(data);
      if (data?.id) {
        getLeaveBalance(accessToken, data.id).then((bal) => {
          setBalance(bal);
        });
      }
    });
    getForms(accessToken).then((data) => setTemplates(data ?? []));
    getEmployees(accessToken).then((data) => {
      setEmployees(data ?? []);
    });
  }, [accessToken]);

  useEffect(() => {
    if (templates.length > 0) {
      setDocType(templates[0].formType);
      setSelectedForm(templates[0]);
    }
  }, [templates]);

  const handleAddApprover = (member) => {
    if (approvers.find((a) => a.member.id === member.id)) return;
    const roles = ["REVIEW", "APPROVE", "REFERENCE"];
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

  // 파일 선언
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFile,
    uploadedFileRef,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "APPROVAL");

  const handleSubmit = async () => {
    if (!accessToken || !isValid || isLoading) return;

    // items 중 amount는 숫자 items는 배열로 보낼 시 오류터짐 방지
    const rawItems = {
      ...formValues,
      name: myInfo?.name,
      departmentName: myInfo?.departmentName,
    };

    // Map<String, String> 맞게 모든 값을 문자열로 변환
    const stringifiedItems = {};

    for (const [key, value] of Object.entries(rawItems)) {
      if (Array.isArray(value) || typeof value === "object") {
        // 배열이나 객체는 JSON 문자열로 변환
        stringifiedItems[key] = JSON.stringify(value);
      } else {
        // 숫자, 문자열 등은 String()으로 변환
        stringifiedItems[key] = String(value ?? "");
      }
    }

    // LEAVE 양식인데 leaveType이 없으면 기본값 보장
    if (selectedForm?.formType === "LEAVE" && !stringifiedItems.leaveType) {
      stringifiedItems.leaveType = "ANNUAL";
    }

    // LEAVE 양식일 때 잔여일 추가
    if (selectedForm?.formType === "LEAVE") {
      stringifiedItems.remainingDays = String(balance?.remainingDays ?? "");
    }

    const body = {
      formId: selectedForm?.id,
      title,
      approvalLines: approvers.map((a, idx) => ({
        approverId: a.member.id,
        stepOrder: idx + 1,
        stepType: a.role,
      })),
      items: stringifiedItems,
    };

    setIsLoading(true);
    try {
      // 전자결제 저장
      const result = await createApproval(accessToken, body);
      const approvalId = result.data.id;

      // 파일 저장
      for (const file of uploadedFile) {
        if (file?.filePath) {
          await saveFile(accessToken, {
            ...file,
            refType: "APPROVAL",
            refId: approvalId,
          });
        }
      }

      if (approvalId) {
        setSubmitted(true);
        setTimeout(() => navigate("/approval"), 1600);
      }
    } catch (error) {
      console.error("게시글 등록 실패", err);
    } finally {
      setIsLoading(false);
      // 파일 초기화
      clearFiles();
    }
  };

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
            <h1 className={s.pageTitle}>{selectedForm?.formName}</h1>
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
              {templates.map((tpl) => (
                <button
                  key={tpl.id}
                  className={s.tplItem}
                  onClick={() => {
                    setDocType(tpl.formType);
                    setTitle(
                      tpl.formName +
                        " - " +
                        new Date().toLocaleDateString("ko-KR"),
                    );
                    setSelectedForm(tpl);
                    setFormValues({});
                    setShowTemplate(false);
                  }}
                  type="button"
                  role="menuitem"
                >
                  <FileText size={14} color="#9CA3AF" />
                  <div>
                    <p className={s.tplItemName}>{tpl.formName}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <ApprovalFormPanel
            selectedForm={selectedForm}
            formValues={formValues}
            setFormValues={setFormValues}
            myInfo={myInfo}
            title={title}
            setTitle={setTitle}
            employees={employees}
            validateRef={validateRef}
            isEditMode={false}
            leaveBalance={balance}
          />
          <WSCard
            title="첨부 파일"
            subtitle={`${attachments.length}개 파일 첨부됨`}
          >
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
              <WSAvatar src={null} name={myInfo?.name} size={30} />
              <div className={s.applicantBody}>
                <p className={s.applicantName}>{myInfo?.name}</p>
                <p className={s.applicantRole}>{myInfo?.department}</p>
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
                      <WSSelect
                        value={a.role}
                        onChange={(e) => handleRoleChange(a.id, e.target.value)}
                        className={s.roleSelect}
                        options={[
                          { value: "REVIEW", label: "검토자" },
                          { value: "APPROVE", label: "최종승인자" },
                          { value: "REFERENCE", label: "참조자" },
                        ]}
                      />
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
                {employees
                  .filter((m) => m.id !== 4)
                  .map((member) => {
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

          <div className={s.actionsCol}>
            <button
              onClick={() => {
                // 결재자 체크
                if (approvers.length === 0) {
                  alert("결재자를 최소 1명 추가하세요.");
                  return;
                }
                // 폼 유효성 체크
                if (validateRef.current && !validateRef.current()) return;
                // 제출
                handleSubmit();
              }}
              disabled={isLoading || !isValid}
              className={s.submitBtn}
            >
              <Send size={16} />
              {isLoading ? "등록 중..." : "결재 요청 제출"}
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
