import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { ArrowLeft, Download, Pencil } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  getTaskById,
  deleteTask,
  getMyInfo,
  getEmployees,
} from "../services/taskApi";
import {
  WSAvatar,
  WSButton,
  WSCard,
  WSProgress,
} from "../../../components/common/CommonWidgets";
import { WSFileList } from "../../../components/common/FormComponents";
import s from "./TaskDetailPage.module.css";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";

export default function TaskDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [task, setTask] = useState(null);
  const [myId, setMyId] = useState(null);
  const [role, setRole] = useState(null);
  const [profile, setProfile] = useState([]); // 목록 프로필이미지

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

    getTaskById(accessToken, id).then((data) => {
      if (!data) return;

      setTask(data);
    });

    // 파일 데이터 불러오기
    if (!accessToken || !id) return;
    getFile(accessToken, "TASK", id).then((data) => {
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
            {(role === "ADMIN" ||
              task.creatorId === myId ||
              task.assigneeId === myId) && (
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
    </div>
  );
}
