import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ClipboardList, Plus } from "lucide-react";
import { KANBAN_TASKS } from "../../../constants/mockData";
import {
  WSAvatar,
  WSBadge,
  WSEmptyState,
  WSFilterBar,
  WSPagination,
  WSTableHeader
} from "../../../components/common/CommonWidgets";
import s from "./TaskListPage.module.css";

const STATUS_CONFIG = {
  todo: { label: "대기중" },
  inProgress: { label: "진행중" },
  done: { label: "완료" },
};

const STATUS_OPTIONS = [
  { key: "all", label: "전체" },
  { key: "todo", label: "대기중" },
  { key: "inProgress", label: "진행중" },
  { key: "done", label: "완료" },
];

const TH_COL = ["상태", "작업명", "진행률(%)", "담당자", "프로젝트 기간"]

export default function Tasks() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);
  const navigate = useNavigate();

  const allTasks = [
    ...KANBAN_TASKS.todo.map((t) => ({ ...t, status: "todo" })),
    ...KANBAN_TASKS.inProgress.map((t) => ({ ...t, status: "inProgress" })),
    ...KANBAN_TASKS.done.map((t) => ({ ...t, status: "done" })),
  ];

  const filtered = allTasks.filter((task) => {
    const matchSearch =
      task.title.toLowerCase().includes(search.toLowerCase()) ||
      task.id.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === "all" || task.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const perPage = 10;
  const paginatedTasks = filtered.slice((page - 1) * perPage, page * perPage);

  return (
    <div>
      <WSFilterBar
        filters={[{ label: "상태", key: "status", options: STATUS_OPTIONS }]}
        filterValues={{ status: statusFilter }}
        onFilterChange={(_key, value) => { setStatusFilter(value); setPage(1); }}
        searchValue={search}
        onSearchChange={(value) => { setSearch(value); setPage(1); }}
        searchPlaceholder="검색어를 입력하세요"
        actions={[
          { label: "업무 등록", onClick: () => navigate("/tasks/new"), icon: <Plus size={16} />, variant: "primary" },
        ]}
      />

      <div className={s.table}>
        <WSTableHeader
          columns={TH_COL}
          gridTemplate="100px 1fr 120px 150px 220px"
         />

        {paginatedTasks.length === 0 ? (
          <div className={s.empty}>
            <WSEmptyState
              icon={<ClipboardList size={32} />}
              title="등록된 업무가 없습니다"
              description="업무를 등록하거나 검색 조건을 변경해 보세요."
            />
          </div>
        ) : (
          paginatedTasks.map((task) => {
            const config = STATUS_CONFIG[task.status];
            return (
              <div
                key={task.id}
                onClick={() => navigate(`/tasks/${task.id}`)}
                className={s.row}
              >
                <div className={s.statusBadge}>
                  <WSBadge status={task.status} label={config.label} />
                </div>
                <p className={s.title}>{task.title}</p>
                <p className={s.progress}>{task.progress}%</p>
                <div className={s.assignee}>
                  <WSAvatar src={task.assignee.avatar} name={task.assignee.name} size={28} />
                  <span className={s.assigneeName}>{task.assignee.name.split(" ")[0]}</span>
                </div>
                <p className={s.period}>{task.startDate} ~ {task.endDate}</p>
              </div>
            );
          })
        )}
      </div>

      <div className={s.pagination}>
        <WSPagination total={filtered.length} page={page} perPage={perPage} onPageChange={setPage} />
      </div>
    </div>
  );
}
