import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { ArrowLeft, Download, Pencil } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { getTaskById, deleteTask, getMyInfo } from "../services/taskApi";
import {
  WSAvatar,
  WSButton,
  WSCard,
  WSProgress,
} from "../../../components/common/CommonWidgets";
import s from "./TaskDetailPage.module.css";

export default function TaskDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [task, setTask] = useState(null);
  const [myId, setMyId] = useState(null);
  const [role, setRole] = useState(null);

  useEffect(() => {
    if (!accessToken) return;
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
  }, [accessToken, id]);

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
                      <WSAvatar src={null} name={task.assigneeName} size={24} />
                      <span>{task.assigneeName}</span>
                    </div>
                  </div>
                  <p className={s.infoLabel}>작성자</p>
                  <div className={s.infoValue}>
                    <div className={s.assigneeValue}>
                      <WSAvatar src={null} name={task.creatorName} size={24} />
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
            <div className={s.attachRow}>
              <div className={s.attachLeft}>
                <div className={s.attachIcon}>XLSX</div>
                <div>
                  <p className={s.attachName}>Q3_예산_요청.xlsx</p>
                  <p className={s.attachSize}>1.2 MB</p>
                </div>
              </div>
              <button className={s.attachDl}>
                <Download size={18} />
              </button>
            </div>
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
