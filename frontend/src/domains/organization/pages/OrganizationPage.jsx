import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, ChevronRight, Building2 } from "lucide-react";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import {
  WSAvatar,
  WSPagination,
  WSFilterBar,
  WSModal,
  WSModalActions,
  WSEmptyState,
  WSFormField,
  WSInput,
  WSButton,
} from "../../../components/common/CommonWidgets";
import s from "./OrganizationPage.module.css";

const ORGANIZATION_DATA = TEAM_MEMBERS.map((member) => ({
  dept: member.dept,
  rank: member.role,
  name: member.name,
  email: member.email,
  phone: `010-1234-56${member.id}8`,
  joinDate: "2020-03-29",
  avatar: member.avatar,
}));

const DEPT_OPTIONS = [
  { key: "전체", label: "전체" },
  { key: "경영진", label: "경영진" },
  { key: "제품팀", label: "제품팀" },
  { key: "개발팀", label: "개발팀" },
  { key: "디자인팀", label: "디자인팀" },
];

export default function Organization() {
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("전체");
  const [page, setPage] = useState(1);
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [showAddDeptModal, setShowAddDeptModal] = useState(false);
  const [departments, setDepartments] = useState(["경영진", "개발팀"]);
  const [newDeptName, setNewDeptName] = useState("");
  const navigate = useNavigate();

  const filtered = ORGANIZATION_DATA.filter((item) => {
    const matchSearch =
      item.name.toLowerCase().includes(search.toLowerCase()) ||
      item.email.toLowerCase().includes(search.toLowerCase());
    const matchDept = deptFilter === "전체" || item.dept === deptFilter;
    return matchSearch && matchDept;
  });

  const perPage = 10;
  const paginatedData = filtered.slice((page - 1) * perPage, page * perPage);

  return (
    <div>
      <WSFilterBar
        filters={[{ label: "부서", key: "dept", options: DEPT_OPTIONS }]}
        filterValues={{ dept: deptFilter }}
        onFilterChange={(_key, value) => { setDeptFilter(value); setPage(1); }}
        searchValue={search}
        onSearchChange={setSearch}
        searchPlaceholder="검색어를 입력하세요"
        actions={[
          { label: "부서 관리", onClick: () => setShowDeptModal(true), icon: <Plus size={16} />, variant: "secondary" },
          { label: "직원 추가", onClick: () => navigate("/organization/employee-add"), icon: <Plus size={16} />, variant: "primary" },
        ]}
      />

      <div className={s.table}>
        <div className={s.tableHeader}>
          <span>부서명</span>
          <span>직급</span>
          <span>이름</span>
          <span>이메일</span>
          <span>연락처</span>
          <span>입사일</span>
        </div>

        {paginatedData.length === 0 ? (
          <div className={s.empty}>
            <p className={s.emptyText}>등록된 직원이 없습니다</p>
          </div>
        ) : (
          paginatedData.map((item, index) => (
            <div key={index} className={s.row}>
              <p className={s.dept}>{item.dept}</p>
              <p className={s.rank}>{item.rank}</p>
              <div className={s.nameCell}>
                <WSAvatar src={item.avatar} name={item.name} size={28} />
                <span className={s.name}>{item.name}</span>
              </div>
              <p className={s.cell}>{item.email}</p>
              <p className={s.cell}>{item.phone}</p>
              <p className={s.cell}>{item.joinDate}</p>
            </div>
          ))
        )}
      </div>

      <div className={s.pagination}>
        <WSPagination total={filtered.length} page={page} perPage={perPage} onPageChange={setPage} />
      </div>

      <WSModal isOpen={showDeptModal} onClose={() => setShowDeptModal(false)} title="부서 관리" size="md">
        {departments.length === 0 ? (
          <WSEmptyState
            icon={<Building2 size={32} />}
            title="등록된 부서가 없습니다"
            description="새 부서를 추가하여 조직 관리 시스템을 시작해 보세요. 더욱 효율적인 업무 관리가 가능합니다."
            action={{
              label: "신규 부서 추가",
              onClick: () => { setShowAddDeptModal(true); setShowDeptModal(false); },
              icon: <Plus size={16} />,
            }}
          />
        ) : (
          <>
            <button
              onClick={() => { setShowAddDeptModal(true); setShowDeptModal(false); }}
              className={s.addDeptBtn}
            >
              <Plus size={16} />
              신규 부서 추가
            </button>

            <div className={s.deptList}>
              {departments.map((dept) => (
                <button key={dept} className={s.deptItem}>
                  <span>{dept}</span>
                  <ChevronRight size={16} className={s.deptChevron} />
                </button>
              ))}
            </div>
          </>
        )}
      </WSModal>

      <WSModal
        isOpen={showAddDeptModal}
        onClose={() => { setShowAddDeptModal(false); setShowDeptModal(true); setNewDeptName(""); }}
        title="부서 등록"
        size="md"
      >
        <WSFormField label="부서명" required>
          <WSInput
            type="text"
            value={newDeptName}
            onChange={(e) => setNewDeptName(e.target.value)}
            placeholder="부서명을 입력하세요"
          />
        </WSFormField>

        <WSModalActions>
          <WSButton
            label="취소"
            variant="secondary"
            onClick={() => { setShowAddDeptModal(false); setShowDeptModal(true); setNewDeptName(""); }}
          />
          <WSButton
            label="추가"
            variant="primary"
            disabled={!newDeptName.trim()}
            onClick={() => {
              if (newDeptName.trim()) {
                setDepartments([...departments, newDeptName.trim()]);
                setNewDeptName("");
                setShowAddDeptModal(false);
                setShowDeptModal(true);
              }
            }}
          />
        </WSModalActions>
      </WSModal>
    </div>
  );
}
