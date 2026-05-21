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
} from "../../../components/common/CommonWidgets";
import s from "./TaskListPage.module.css";

const STATUS_CONFIG = {
  todo: { label: "대기중", bg: "#FEF3C7", text: "#92400E" },
  inProgress: { label: "진행중", bg: "#DBEAFE", text: "#1E40AF" },
  review: { label: "검토중", bg: "#E0E7FF", text: "#4338CA" },
  done: { label: "완료", bg: "#D1FAE5", text: "#065F46" },
};

const STATUS_OPTIONS = [
  { key: "all", label: "전체" },
  { key: "todo", label: "대기중" },
  { key: "inProgress", label: "진행중" },
  { key: "review", label: "검토중" },
  { key: "done", label: "완료" },
];

export default function Tasks() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);
  const navigate = useNavigate();

  const allTasks = [
    ...KANBAN_TASKS.todo.map((t) => ({ ...t, status: "todo" })),
    ...KANBAN_TASKS.inProgress.map((t) => ({ ...t, status: "inProgress" })),
    ...KANBAN_TASKS.review.map((t) => ({ ...t, status: "review" })),
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
        <div className={s.tableHeader}>
          <span>상태</span>
          <span>작업명</span>
          <span>진행률(%)</span>
          <span>담당자</span>
          <span>프로젝트 기간</span>
        </div>

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
                <WSBadge status={task.status} label={config.label} />
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
