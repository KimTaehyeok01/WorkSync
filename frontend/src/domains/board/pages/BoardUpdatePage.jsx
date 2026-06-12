import { useState, useRef, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Send } from "lucide-react";
import { ArrowLeft, Paperclip, CheckCircle } from "lucide-react";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import {
  WSFileUploadZone,
  WSFileList,
} from "../../../components/common/FormComponents";
import s from "./BoardCreatePage.module.css";
import { getMyInfo, getPostById, getUpdatePosts } from "../services/boardApi";
import useAuthContext from "../../../store/AuthContext";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";

const CATEGORY_OPTIONS = [
  { value: "notice", label: "공지사항", color: "#EF4444" },
  { value: "dept", label: "부서게시판", color: "#8B5CF6" },
  { value: "free", label: "자유게시판", color: "#10B981" },
];

const fileIconColor = {
  PDF: "#EF4444",
  XLSX: "#10B981",
  PPTX: "#F59E0B",
  DOCX: "#3B82F6",
  PNG: "#06B6D4",
  ZIP: "#F97316",
  default: "#6B7280",
};

const TOOLBAR_ITEMS = ["굵게", "기울임", "밑줄", "목록"];

export default function BoardNew() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("free");
  const [content, setContent] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [role, setRole] = useState(null);
  const [myDepartmentName, setMyDepartmentName] = useState("");
  const { accessToken } = useAuthContext();
  const { boardId, postId } = useParams();
  const MAX_CHARS = 3000;

  const isValid =
    title.trim().length > 0 && category !== "" && content.trim().length > 0;

  // 파일 선언
  const {
    files,
    setFiles,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "POST", postId);

  const getCategoryValue = (boardName) => {
    const found = CATEGORY_OPTIONS.find(
      (opt) => opt.label.replace(/\s/g, "") === boardName.replace(/\s/g, ""),
    );
    return found ? found.value : "free";
  };

  useEffect(() => {
    getPostById(boardId, postId, accessToken).then((data) => {
      setTitle(data.title);
      setContent(data.content);
      setCategory(getCategoryValue(data.boardName));
    });
    // 내 정보 조회(부서명 세팅)
    getMyInfo(accessToken).then((data) => {
      setMyDepartmentName(data.departmentName);
      setRole(data.role);
    });
    // 파일 데이터 불러오기
    if (!accessToken || !postId) return;
    getFile(accessToken, "POST", postId).then((data) => {
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
  }, [boardId, postId, accessToken]);

  // 저장
  async function handleSubmit() {
    if (!accessToken) return;

    if (!isValid) return;
    try {
      await getUpdatePosts(
        boardId,
        postId,
        {
          title: title,
          content: content,
        },
        accessToken,
      );

      // 파일 경로가 있으면 파일 저장
      if (uploadedFile?.filePath && uploadedFile?.isNew) {
        // 파일 저장
        await saveFile(accessToken, {
          ...uploadedFile,
          refType: "POST",
          refId: postId,
        });
      }

      setSubmitted(true);
      setTimeout(() => navigate("/board"), 1800);
    } catch (err) {
      console.error("게시글 업데이트 실패", err);

      // 파일 삭제
      removeFiles();
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
          <div className={s.successCopy}>
            <p className={s.successTitle}>게시글이 수정되었습니다</p>
            <p className={s.successDesc}>게시판으로 이동합니다...</p>
          </div>
          <div className={s.successBadge}>게시판으로 이동 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={s.root}>
      <div className={s.header}>
        <div className={s.headerLeft}>
          <button
            onClick={() => {
              if (
                confirm(
                  "페이지 이동 시 작성하신 수정 내용은 사라집니다. 이동하시겠습니까?",
                )
              ) {
                navigate(-1);
              }
            }}
            className={s.backBtn}
          >
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>게시글 수정</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={s.colMain}>
          <WSCard
            title="게시글 기본 정보"
            subtitle="게시판 분류와 제목을 입력하세요"
          >
            <div className={s.formGrid}>
              <div>
                <label className={s.label}>
                  게시판 분류 <span className={s.required}>*</span>
                </label>
                <div className={s.catRow}>
                  {CATEGORY_OPTIONS.map((opt) => {
                    const active = category === opt.value;
                    const isNoticeOption = opt.label === "공지사항";
                    const isDisabled = isNoticeOption && role !== "ADMIN";

                    return (
                      <button
                        key={opt.value}
                        onClick={() => !isDisabled && setCategory(opt.value)}
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
                {category === "dept" && (
                  <div>
                    <label className={s.label}>부서명</label>
                    <input
                      type="text"
                      value={myDepartmentName}
                      readOnly
                      className={s.input}
                    />
                  </div>
                )}
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
                <span
                  className={`${s.contentCount} ${content.length > MAX_CHARS * 0.9 ? s.contentCountWarn : ""}`}
                >
                  {content.length}/{MAX_CHARS}
                </span>
              }
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
              onRemove={handleRemoveFile}
            />
          </WSCard>

          <div className={s.actionsCol}>
            <WSButton
              label="수정하기"
              icon={<Send size={16} />}
              onClick={handleSubmit}
              disabled={!isValid}
              className={s.submitBtn}
            />
            <button
              onClick={() => {
                if (
                  confirm(
                    "페이지 이동 시 작성하신 수정 내용은 사라집니다. 이동하시겠습니까?",
                  )
                )
                  navigate(-1);
              }}
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
