import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Send } from "lucide-react";
import {
  getCreatePosts,
  getBoards,
  getMyInfo,
  getDepartmentBoard,
} from "../services/boardApi";
import { ArrowLeft, Paperclip, CheckCircle } from "lucide-react";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import {
  WSFileUploadZone,
  WSFileList,
} from "../../../components/common/FormComponents";
import s from "./BoardCreatePage.module.css";
import useAuthContext from "../../../store/AuthContext";
import useFileUpload from "../../../hooks/useFileUpload";
import { saveFile, deleteFile } from "../../file/services/FileApi";

const BOARD_COLORS = {
  1: "#EF4444", // 공지사항 - 빨강
  2: "#8B5CF6", // 부서게시판 - 보라
  3: "#10B981", // 자유게시판 - 초록
};

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
  const fileInputRef = useRef(null);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [content, setContent] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [boardId, setboardId] = useState("");
  const [role, setRole] = useState(null);
  const [boardOptions, setBoardOptions] = useState([]);
  const [myDepartmentName, setMyDepartmentName] = useState("");
  const [deptBoardId, setDeptBoardId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [employee, setEmployee] = useState([]);
  const MAX_CHARS = 3000;
  const { accessToken } = useAuthContext();

  // 유효성 검사
  const isValid =
    title.trim().length > 0 && category !== "" && content.trim().length > 0;

  // 파일 선언
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "POST");

  // 폼 저장
  async function handleSubmit() {
    if (!accessToken || !isValid || isLoading) return;

    // 부서게시판 선택 시 실제 부서 board id 사용
    const actualBoardId = category === "DEPARTMENT" ? deptBoardId : category;
    if (!actualBoardId) return;

    setIsLoading(true);
    try {
      // 게시물 저장
      const response = await getCreatePosts(
        actualBoardId,
        {
          boardId: actualBoardId,
          title: title,
          content: content,
        },
        accessToken,
      );
      const postId = response;

      // 파일 저장
      if (uploadedFile?.filePath) {
        await saveFile(accessToken, {
          ...uploadedFile,
          refType: "POST",
          refId: postId,
        });
      }

      setSubmitted(true);

      setTimeout(() => navigate("/board"), 1800);
    } catch (err) {
      console.error("게시글 등록 실패", err);
    } finally {
      setIsLoading(false);
    }

    // 파일 초기화
    clearFiles();
  }

  // API에서 받아온 게시판 목록(드롭다운)
  useEffect(() => {
    if (!accessToken) return;

    // 내 부서게시판 id 조회
    getDepartmentBoard(accessToken).then((deptBoard) => {
      if (deptBoard) setDeptBoardId(deptBoard.id);
    });

    getBoards(accessToken).then((data) => {
      if (!data) return;

      // DEPARTMENT 타입 개별 게시판 제외 → "부서게시판" 하나로 고정
      const nonDeptBoards = data
        .filter((board) => board.boardType !== "DEPARTMENT")
        .sort((a, b) => a.id - b.id)
        .map((board) => ({
          value: board.id,
          label: board.name,
          color: BOARD_COLORS[board.id],
        }));

      // 부서게시판 고정 항목 추가 (value="DEPARTMENT")
      setBoardOptions([
        ...nonDeptBoards,
        { value: "DEPARTMENT", label: "부서게시판", color: "#8B5CF6" },
      ]);
    });
  }, [accessToken]);

  // 내 데이터 저장
  useEffect(() => {
    if (!accessToken) return;

    getMyInfo(accessToken).then((data) => {
      setMyDepartmentName(data.departmentName);
      setRole(data.role);
    });
  }, [accessToken]);

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
      <div className={s.header}>
        <div className={s.headerLeft}>
          <button onClick={() => navigate("/board")} className={s.backBtn}>
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>게시글 작성 등록</h1>
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
                  {boardOptions.map((opt) => {
                    const active = category === opt.value;
                    const isNoticeOption = opt.label === "공지사항";
                    const isDisabled = isNoticeOption && role !== "ADMIN";
                    return (
                      <button
                        key={opt.value}
                        onClick={() => !isDisabled && setCategory(opt.value)}
                        className={`${s.catBtn} ${active ? s.catBtnActive : ""} ${isDisabled ? s.catBtnDisabled : ""}`}
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

                {category === "DEPARTMENT" && (
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
              onRemove={removeFiles}
            />
          </WSCard>

          <div className={s.actionsCol}>
            <WSButton
              label={isLoading ? "등록 중..." : "작업 등록"}
              icon={<Send size={16} />}
              onClick={handleSubmit}
              disabled={!isValid || isLoading}
              className={s.submitBtn}
            />
            <button onClick={() => navigate("/board")} className={s.cancelBtn}>
              취소하고 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
