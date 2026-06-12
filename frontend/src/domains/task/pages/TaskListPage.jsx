import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ClipboardList, Plus } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  getTaskList,
  getTasksByDepartment,
  getMyInfo,
  getEmployees,
} from "../services/taskApi";
import {
  WSAvatar,
  WSBadge,
  WSEmptyState,
  WSFilterBar,
  WSPagination,
  WSTableHeader,
} from "../../../components/common/CommonWidgets";
import s from "./TaskListPage.module.css";

const STATUS_CONFIG = {
  TODO: { label: "대기중" },
  IN_PROGRESS: { label: "진행중" },
  DONE: { label: "완료" },
};

const STATUS_OPTIONS = [
  { key: "all", value: "all", label: "전체" },
  { key: "TODO", value: "TODO", label: "대기중" },
  { key: "IN_PROGRESS", value: "IN_PROGRESS", label: "진행중" },
  { key: "DONE", value: "DONE", label: "완료" },
];

const TH_COL = ["상태", "업무명", "진행률(%)", "담당자", "프로젝트 기간"];
export default function Tasks() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);
  // 업무 목록 배열
  const [tasks, setTasks] = useState([]);
  // 전체 업무 개수, 페이징 처리
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const { accessToken } = useAuthContext();
  const [role, setRole] = useState(null);
  const [departmentId, setDepartmentId] = useState(null);
  const [profile, setProfile] = useState([]); // 목록 프로필이미지
  const navigate = useNavigate();

  //내 정보 불러오기
  useEffect(() => {
    if (!accessToken) return;

    // 전 직원 프로필
    getEmployees(accessToken).then((data) => {
      setProfile(Array.isArray(data) ? data : []);
    });

    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setRole(data.role);
      setDepartmentId(data.departmentId);
    });
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken || role === null) return;

    if (role === "ADMIN") {
      // ADMIN 이면 전체 목록 보이기
      getTaskList(accessToken, page - 1, 10, statusFilter).then((data) => {
        if (!data) return;
        setTasks(data.content);
        setTotalElements(data.totalElements);
      });
    } else {
      getTasksByDepartment(
        accessToken,
        departmentId,
        page - 1,
        10,
        statusFilter,
      ).then((data) => {
        if (!data) return;
        setTasks(data.content);
        setTotalElements(data.totalElements);
      });
    }
  }, [accessToken, role, departmentId, page, statusFilter]);

  const filtered = tasks.filter((task) => {
    const matchSearch =
      task.title
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase()) ||
      String(task.id).toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === "all" || task.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const perPage = 10;

  return (
    <div>
      <WSFilterBar
        filters={[{ label: "상태", key: "status", options: STATUS_OPTIONS }]}
        filterValues={{ status: statusFilter }}
        onFilterChange={(_key, value) => {
          console.log("key:", _key, "value:", value);
          setStatusFilter(value);
          setPage(1);
        }}
        searchValue={search}
        onSearchChange={(value) => {
          setSearch(value);
          setPage(1);
        }}
        searchPlaceholder="검색어를 입력하세요"
        actions={[
          {
            label: "업무 등록",
            onClick: () => navigate("/tasks/new"),
            icon: <Plus size={16} />,
            variant: "primary",
          },
        ]}
      />

      <div className={s.table}>
        <WSTableHeader
          columns={TH_COL}
          gridTemplate="100px 1fr 120px 150px 220px"
        />

        {filtered.length === 0 ? (
          <div className={s.empty}>
            <WSEmptyState
              icon={<ClipboardList size={32} />}
              title="등록된 업무가 없습니다"
              description="업무를 등록하거나 검색 조건을 변경해 보세요."
            />
          </div>
        ) : (
          filtered.map((task) => {
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
                  <WSAvatar
                    src={
                      profile.find((p) => p.id === task.assigneeId)
                        ?.profileImage ?? null
                    }
                    name={task.assigneeName}
                    size={28}
                  />
                  <span className={s.assigneeName}>
                    {task.assigneeName?.split(" ")[0]}
                  </span>
                </div>
                <p className={s.period}>
                  {task.startDate} ~ {task.dueDate}
                </p>
              </div>
            );
          })
        )}
      </div>
      <div className={s.pagination}>
        <WSPagination
          total={totalElements}
          page={page}
          perPage={perPage}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
