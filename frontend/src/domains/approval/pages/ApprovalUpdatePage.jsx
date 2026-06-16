import useAuthContext from "../../../store/AuthContext";
import ApprovalFormPanel from "../components/ApprovalFormPanel";
import { useState, useRef, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { WSCard } from "../../../components/common/CommonWidgets";
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
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";
import s from "./ApprovalCreatePage.module.css";

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

    // 파일 데이터 불러오기
    getFile(accessToken, "APPROVAL", id).then((data) => {
      const fileList = Array.isArray(data.data) ? data.data : [];
      // console.log(fileList);
      setFiles(
        fileList.map((f) => ({
          file: {
            name: f.originalName,
            size: f.fileSize,
          },
          url: f.filePath,
          refType: f.refType,
          refId: f.refId,
        })),
      );
    });
  }, [accessToken, id]);

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
    try {
      // 전자결제 업로드
      const result = await updateApproval(accessToken, id, body);

      // 파일 경로가 있으면 파일 저장
      for (const file of uploadedFile) {
        if (file?.filePath && file?.isNew) {
          // 파일 저장
          await saveFile(accessToken, {
            ...file,
            refType: "APPROVAL",
            refId: id,
          });
        }
      }

      if (result?.status === 200) {
        setSubmitted(true);
        setTimeout(() => navigate("/approval"), 1600);
      }
      setIsLoading(false);
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
