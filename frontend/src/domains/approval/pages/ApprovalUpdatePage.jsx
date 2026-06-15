import useAuthContext from "../../../store/AuthContext";
import ApprovalFormPanel from "../components/ApprovalFormPanel";
import { useState, useRef, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { WSCard } from "../../../components/common/CommonWidgets";
import s from "./ApprovalCreatePage.module.css";
import {
  ArrowLeft,
  Paperclip,
  FileText,
  Trash2,
  CheckCircle,
  Send,
} from "lucide-react";
import {
  getMyInfo,
  getApprovalById,
  getEmployees,
  updateApproval,
} from "../services/approvalApi";

const fileIconColor = {
  pdf: "#EF4444",
  xlsx: "#10B981",
  pptx: "#F59E0B",
  docx: "#3B82F6",
  default: "#6B7280",
};
// formType 대신 formId로 매핑
const FORM_TYPE_MAP = {
  1: "LEAVE",
  2: "EXPENSE",
  3: "PURCHASE",
  4: "BUSINESS_TRIP",
};

export default function ApprovalUpdate() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { accessToken } = useAuthContext();
  const [title, setTitle] = useState("");
  const [selectedForm, setSelectedForm] = useState(null);
  const [formValues, setFormValues] = useState({});
  const [myInfo, setMyInfo] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const validateRef = useRef(null);

  // 내 정보 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setMyInfo(data);
    });
    getEmployees(accessToken).then((data) => {
      setEmployees(data ?? []);
    });
  }, [accessToken]);

  // 기존 데이터 불러와서 폼에 채우기
  useEffect(() => {
    if (!accessToken || !id) return;
    getApprovalById(accessToken, id).then((data) => {
      if (!data) return;
      setTitle(data.title);
      setSelectedForm({
        id: data.formId,
        formName: data.formName,
        formType: FORM_TYPE_MAP[data.formId],
      });

      setFormValues(data.items ?? {});
    });
  }, [accessToken, id]);

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

  const handleSubmit = async () => {
    if (!title.trim()) return;

    // 기존 값 복사
    const rawItems = { ...formValues };
    // 변환된 값 담을 빈 객체
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

    // 수정 API 맞게 body 구성 (approvalLines 없음)
    const body = { title, items: stringifiedItems };

    setIsLoading(true);
    const result = await updateApproval(accessToken, id, body);
    if (result?.status === 200) {
      setSubmitted(true);
      setTimeout(() => navigate("/approval"), 1600);
    }
    setIsLoading(false);
  };

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} color="#10B981" />
          </div>
          <div>
            <p className={s.successTitle}>결재 문서가 수정되었습니다</p>
            <p className={s.successDesc}>전자결재 목록으로 이동합니다.</p>
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
            <h1 className={s.pageTitle}>{selectedForm?.formName} 수정</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={`${s.col} ${s.colMain}`}>
          <ApprovalFormPanel
            selectedForm={selectedForm}
            formValues={formValues}
            setFormValues={setFormValues}
            employees={employees ?? []}
            myInfo={myInfo}
            title={title}
            setTitle={setTitle}
            validateRef={validateRef}
            isEditMode={true}
          />
        </div>

        <div className={`${s.col} ${s.colSide}`}>
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
              role="button"
              tabIndex={0}
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
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </WSCard>
          <div className={s.actionsCol}>
            <button
              onClick={handleSubmit}
              disabled={!title.trim() || isLoading}
              className={s.submitBtn}
            >
              <Send size={16} />
              {isLoading ? "수정 중..." : "수정 완료"}
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
