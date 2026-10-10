// 업무 칸반 보드 — 대기·진행·완료 열 사이로 카드를 끌어 놓아 상태를 바꾼다
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { TASK_STATUS_LABEL } from "../../../constants/statusLabels";
import { getTaskList, getTasksByDepartment, updateTask } from "../services/taskApi";
import s from "./TaskKanban.module.css";

const COLUMNS = ["TODO", "IN_PROGRESS", "DONE"];
// 보드에는 페이지 없이 한 번에 보여준다
const BOARD_SIZE = 200;

export default function TaskKanban({ accessToken, role, departmentId, search, profiles }) {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState([]);
  const [draggingId, setDraggingId] = useState(null);
  const [overColumn, setOverColumn] = useState(null);
  const [error, setError] = useState("");

  // 목록 화면과 같은 범위 — 관리자는 전체, 그 외에는 내 부서 업무
  const load = async () => {
    const data =
      role === "ADMIN"
        ? await getTaskList(accessToken, 0, BOARD_SIZE)
        : await getTasksByDepartment(accessToken, departmentId, 0, BOARD_SIZE);
    setTasks(data?.content ?? []);
  };

  useEffect(() => {
    if (!accessToken || role === null) return;
    load();
  }, [accessToken, role, departmentId]);

  const handleDrop = async (status) => {
    setOverColumn(null);
    const task = tasks.find((t) => t.id === draggingId);
    setDraggingId(null);
    if (!task || task.status === status) return;

    // 먼저 화면을 옮기고, 서버가 거절하면 되돌린다
    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status } : t)));
    const updated = await updateTask(accessToken, task.id, { status });
    if (!updated) {
      setError("상태를 바꾸지 못했습니다. 권한을 확인해 주세요.");
      load();
      return;
    }
    setError("");
  };

  const keyword = search.replace(/\s/g, "").toLowerCase();
  const visible = tasks.filter((t) =>
    t.title.replace(/\s/g, "").toLowerCase().includes(keyword),
  );

  return (
    <>
      {error && <p className={s.error}>{error}</p>}
      <div className={s.board}>
        {COLUMNS.map((status) => {
          const columnTasks = visible.filter((t) => t.status === status);
          return (
            <div
              key={status}
              className={`${s.column} ${overColumn === status ? s.columnOver : ""}`}
              onDragOver={(e) => {
                e.preventDefault();
                setOverColumn(status);
              }}
              onDragLeave={() => setOverColumn(null)}
              onDrop={() => handleDrop(status)}
            >
              <div className={s.columnHeader}>
                <span className={`${s.dot} ${s[`dot${status}`]}`} />
                {TASK_STATUS_LABEL[status]}
                <span className={s.count}>{columnTasks.length}</span>
              </div>

              <div className={s.cards}>
                {columnTasks.map((task) => (
                  <div
                    key={task.id}
                    className={`${s.card} ${draggingId === task.id ? s.cardDragging : ""}`}
                    draggable
                    onDragStart={() => setDraggingId(task.id)}
                    onDragEnd={() => setDraggingId(null)}
                    onClick={() => navigate(`/tasks/${task.id}`)}
                  >
                    <p className={s.cardTitle}>{task.title}</p>
                    <div className={s.progressTrack}>
                      <div
                        className={s.progressFill}
                        style={{ "--progress": `${task.progress ?? 0}%` }}
                      />
                    </div>
                    <div className={s.cardFooter}>
                      <span className={s.assignee}>
                        <WSAvatar
                          src={profiles.find((p) => p.id === task.assigneeId)?.profileImage ?? null}
                          name={task.assigneeName}
                          size={22}
                        />
                        {task.assigneeName ?? "미지정"}
                      </span>
                      {task.dueDate && <span className={s.due}>~{task.dueDate.slice(5)}</span>}
                    </div>
                  </div>
                ))}
                {columnTasks.length === 0 && <p className={s.empty}>업무가 없습니다</p>}
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
}
