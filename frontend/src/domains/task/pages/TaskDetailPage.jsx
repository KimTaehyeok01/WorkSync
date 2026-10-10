import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { ArrowLeft, ChevronDown, Download, Pencil } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  getTaskById,
  deleteTask,
  getMyInfo,
  getEmployees,
  getTaskList,
  updateTask,
} from "../services/taskApi";
import {
  WSAvatar,
  WSButton,
  WSCard,
  WSProgress,
} from "../../../components/common/CommonWidgets";
import {
  WSFileList,
  WSSelect,
  WSAlert,
} from "../../../components/common/FormComponents";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";
import s from "./TaskDetailPage.module.css";

// 담당자가 고르는 진행률 — 서버는 10 단위만 받는다
const PROGRESS_OPTIONS = Array.from({ length: 11 }, (_, i) => ({
  value: String(i * 10),
  label: `${i * 10}%`,
}));

export default function TaskDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [task, setTask] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false); // 볼 수 없거나 없는 업무
  const [allTasks, setAllTasks] = useState([]);
  const [myId, setMyId] = useState(null);
  const [role, setRole] = useState(null);
  const [profile, setProfile] = useState([]); // 목록 프로필이미지
  const [progressInput, setProgressInput] = useState(""); // 담당자가 고른 진행률
  const [progressError, setProgressError] = useState("");

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
  } = useFileUpload(accessToken, "TASK", id);

  // 현재 업무의 위치 찾기
  const taskIndex = allTasks.findIndex((p) => p.id === Number(id));
  // 이전 업무
  const prevTask = taskIndex > 0 ? allTasks[taskIndex - 1] : null;
  // 다음 업무
  const nextTask =
    taskIndex < allTasks.length - 1 ? allTasks[taskIndex + 1] : null;

  useEffect(() => {
    if (!accessToken) return;

    // 전 직원 프로필
    getEmployees(accessToken).then((data) => {
      setProfile(Array.isArray(data) ? data : []);
    });

    getMyInfo(accessToken).then((data) => {
      if (!data) return;

      setRole(data.role);
      setMyId(data.id);
    });
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;

    setLoadFailed(false);
    setProgressError("");
    getTaskById(accessToken, id).then((data) => {
      if (!data) {
        setLoadFailed(true);
        return;
      }
      setTask(data);
      setProgressInput(String(data.progress));
    });
    getTaskList(accessToken, 0, 9999).then((data) => {
      if (!data) return;
      setAllTasks(data.content);
    });

    // 파일 데이터 불러오기
    if (!accessToken || !id) return;
    getFile(accessToken, "TASK", id).then((data) => {
      const fileList = Array.isArray(data.data) ? data.data : [];
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

  // 파일 다운로드
  const handleDownload = async (file, idx) => {
    const response = await fetch(file.url);
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = file.name;
    a.click();

    URL.revokeObjectURL(url);
  };

  // 담당자의 진행률 저장 — 다른 필드를 함께 보내면 서버가 거절하므로 progress 만 보낸다
  const handleProgressSave = async () => {
    setProgressError("");
    const updated = await updateTask(accessToken, task.id, {
      progress: Number(progressInput),
    });
    if (!updated) {
      setProgressError("진행률을 저장하지 못했습니다.");
      return;
    }
    // 저장하는 사이 다른 업무로 넘어갔으면 그 업무의 표시는 건드리지 않는다
    setTask((prev) =>
      prev.id === updated.id ? { ...prev, progress: updated.progress } : prev,
    );
  };

  if (loadFailed) {
    return (
      <div className={s.notFound}>
        <p>볼 수 없거나 삭제된 업무입니다.</p>
        <WSButton
          label="업무 목록으로"
          variant="secondary"
          onClick={() => navigate("/tasks")}
        />
      </div>
    );
  }

  if (!task) {
    return <div>로딩 중...</div>;
  }

  return (
    <div>
      <div className={s.header}>
        <button onClick={() => navigate("/tasks")} className={s.backBtn}>
          <ArrowLeft size={16} />
        </button>
        <div>
          <div className={s.titleRow}>
            <h1 className={s.pageTitle}>업무 상세</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={s.colMain}>
          <WSCard>
            <div className={s.infoGrid}>
              <div className={s.ColWrapper}>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>업무명</p>
                  <p className={s.infoValue}>{task.title}</p>
                </div>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>기간</p>
                  <p className={s.infoValue}>
                    {task.startDate} ~ {task.dueDate}
                  </p>
                </div>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>진행률</p>
                  <div className={s.infoValue}>
                    <WSProgress value={task.progress} />
                  </div>
                </div>
                {myId !== null && task.assigneeId === myId && (
                  <div className={s.infoCol}>
                    <p className={s.infoLabel}>진행률 변경</p>
                    <div className={s.infoValue}>
                      <div className={s.progressEdit}>
                        <div className={s.progressSelect}>
                          <WSSelect
                            placeholder="진행률"
                            value={progressInput}
                            onChange={(e) => setProgressInput(e.target.value)}
                            options={PROGRESS_OPTIONS}
                          />
                        </div>
                        <WSButton
                          label="저장"
                          onClick={handleProgressSave}
                          disabled={
                            progressInput === "" ||
                            Number(progressInput) === task.progress
                          }
                        />
                      </div>
                    </div>
                  </div>
                )}
                {progressError && (
                  <div className={s.progressAlert}>
                    <WSAlert
                      type="error"
                      message={progressError}
                      onClose={() => setProgressError("")}
                    />
                  </div>
                )}
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>담당자</p>
                  <div className={s.infoValue}>
                    <div className={s.assigneeValue}>
                      <WSAvatar
                        src={
                          profile.find((p) => p.id === task.assigneeId)
                            ?.profileImage ?? null
                        }
                        name={task.assigneeName}
                        size={24}
                      />
                      <span>{task.assigneeName}</span>
                    </div>
                  </div>
                  <p className={s.infoLabel}>작성자</p>
                  <div className={s.infoValue}>
                    <div className={s.assigneeValue}>
                      <WSAvatar
                        src={
                          profile.find((p) => p.id === task.creatorId)
                            ?.profileImage ?? null
                        }
                        name={task.creatorName}
                        size={24}
                      />
                      <span>{task.creatorName}</span>
                    </div>
                  </div>
                  <p className={s.infoLabel}>작성일</p>
                  <p className={s.infoValue}>
                    {new Date(task.createdAt).toLocaleDateString("ko-KR")}
                  </p>
                </div>
              </div>
              <div className={s.details}>
                <div className={s.detailLines}>{task.description}</div>
              </div>
            </div>
          </WSCard>
        </div>

        <div className={s.colSide}>
          <WSCard title="첨부 파일" subtitle="1개 파일 업로드">
            {files ? (
              <WSFileList
                files={files.map(({ file, url }) => ({ ...file, url }))} //화면에 파일 리스트 보여줌
                onDownload={handleDownload}
              />
            ) : (
              <></>
            )}
          </WSCard>

          <div className={s.actionsCol}>
            {task.creatorId === myId && (
              <>
                <WSButton
                  label="수정"
                  icon={<Pencil size={16} />}
                  variant="secondary"
                  onClick={() => navigate(`/tasks/edit/${id}`)}
                  className={s.draftBtn}
                />
                <button
                  onClick={async () => {
                    if (confirm("업무를 삭제하시겠습니까?")) {
                      await deleteTask(accessToken, id);
                      navigate("/tasks/");
                    }
                  }}
                  className={s.cancelBtn}
                >
                  삭제하기
                </button>
              </>
            )}
          </div>
        </div>
      </div>
      {prevTask && (
        <button
          onClick={() => navigate(`/tasks/${prevTask.id}`)}
          className={s.nextBtn}
        >
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>이전 업무</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{prevTask.title}</span>
          </div>
          <span className={s.nextDate}>
            {new Date(prevTask.createdAt).toLocaleDateString("ko-KR")}
          </span>
        </button>
      )}
      {nextTask && (
        <button
          onClick={() => navigate(`/tasks/${nextTask.id}`)}
          className={s.nextBtn}
        >
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>다음 업무</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{nextTask.title}</span>
          </div>
          <span className={s.nextDate}>
            {new Date(nextTask.createdAt).toLocaleDateString("ko-KR")}
          </span>
        </button>
      )}
    </div>
  );
}
