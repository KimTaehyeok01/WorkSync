import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, ChevronRight, Building2, ClipboardList } from "lucide-react";
import {
  getDepartments,
  getEmployee,
  CreateDepartments,
} from "../services/organizationListApi";
import useAuthContext from "../../../store/AuthContext";
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
import {
  WSTableHeader,
  WSTableRow,
} from "../../../components/common/LayoutComponents";
import style from "./OrganizationListPage.module.css";

const TH_COL = ["부서명", "직급", "이름", "이메일", "연락처", "입사일"];
const GRID_TEMPLATE = "1fr 1fr 1fr 1fr 1fr 1fr";

export default function OrganizationListPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("all");
  const [page, setPage] = useState(1);
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [showAddDeptModal, setShowAddDeptModal] = useState(false);
  const [departments, setDepartments] = useState([]);
  const [newDeptName, setNewDeptName] = useState("");
  const [employee, setEmployee] = useState([]);

  useEffect(() => {
    if (!accessToken) return;
    getDepartments(accessToken).then((data) => {
      console.log(data);
      setDepartments(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  const DEPT_OPTIONS = [
    { key: "all", label: "전체" },
    ...departments.map((dept) => ({
      key: dept.id,
      label: dept.name,
    })),
  ];

  useEffect(() => {
    if (!accessToken) return;
    getEmployee(accessToken).then((data) => {
      console.log(data);
      setEmployee(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  const filtered = employee.filter((item) => {
    const matchSearch =
      item.name.toLowerCase().includes(search.toLowerCase()) ||
      item.email.toLowerCase().includes(search.toLowerCase());
    const matchDept = deptFilter === "all" || item.dept === deptFilter;
    return matchSearch && matchDept;
  });

  const perPage = 10;
  const paginatedData = filtered.slice((page - 1) * perPage, page * perPage);

  return (
    <div>
      <WSFilterBar
        filters={[{ label: "부서 선택", key: "dept", options: DEPT_OPTIONS }]}
        filterValues={{ dept: deptFilter }}
        onFilterChange={(_key, value) => {
          setDeptFilter(value);
          setPage(1);
        }}
        searchValue={search}
        onSearchChange={setSearch}
        searchPlaceholder="검색어를 입력하세요"
        actions={[
          {
            label: "부서 관리",
            onClick: () => setShowDeptModal(true),
            icon: <Plus size={16} />,
            variant: "primary",
          },
          {
            label: "직원 추가",
            onClick: () => navigate("/organization/employee-add"),
            icon: <Plus size={16} />,
            variant: "primary",
          },
        ]}
      />

      <div className={style.table}>
        <WSTableHeader columns={TH_COL} gridTemplate={GRID_TEMPLATE} />

        {paginatedData.length === 0 ? (
          <div className={style.empty}>
            <WSEmptyState
              icon={<ClipboardList size={32} />}
              title="등록된 직원이 없습니다"
            />
          </div>
        ) : (
          paginatedData.map((item, index) => (
            <WSTableRow key={index} gridTemplate={GRID_TEMPLATE}>
              <p className={style.dept}>
                {item.departmentName ? item.departmentName : "-"}
              </p>
              <p className={style.rank}>
                {item.jobGrade ? item.jobGrade : "-"}
              </p>
              <div className={style.nameCell}>
                <WSAvatar src={item.profileImage} name={item.name} size={28} />
                <span className={style.name}>
                  {item.name ? item.name : "-"}
                </span>
              </div>
              <p className={style.cell}>{item.email ? item.email : "-"}</p>
              <p className={style.cell}>{item.phone ? item.phone : "-"}</p>
              <p className={style.cell}>
                {item.hireDate ? item.hireDate : "-"}
              </p>
            </WSTableRow>
          ))
        )}
      </div>

      <div className={style.pagination}>
        <WSPagination
          total={filtered.length}
          page={page}
          perPage={perPage}
          onPageChange={setPage}
        />
      </div>

      <WSModal
        isOpen={showDeptModal}
        onClose={() => setShowDeptModal(false)}
        title="부서 관리"
        size="md"
      >
        {departments.length === 0 ? (
          <WSEmptyState
            icon={<Building2 size={32} />}
            title="등록된 부서가 없습니다"
            description="새 부서를 추가하여 조직 관리 시스템을 시작해 보세요. 더욱 효율적인 업무 관리가 가능합니다."
            action={{
              label: "신규 부서 추가",
              onClick: () => {
                setShowAddDeptModal(true);
                setShowDeptModal(false);
              },
              icon: <Plus size={16} />,
            }}
          />
        ) : (
          <>
            <button
              onClick={() => {
                setShowAddDeptModal(true);
                setShowDeptModal(false);
              }}
              className={style.addDeptBtn}
            >
              <Plus size={16} />
              신규 부서 추가
            </button>

            <div className={style.deptList}>
              {departments.map((dept) => (
                <button key={dept.id} className={style.deptItem}>
                  <span>{dept.name}</span>
                  <ChevronRight size={16} className={style.deptChevron} />
                </button>
              ))}
            </div>
          </>
        )}
      </WSModal>

      <WSModal
        isOpen={showAddDeptModal}
        onClose={() => {
          setShowAddDeptModal(false);
          setShowDeptModal(true);
          setNewDeptName("");
        }}
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
            onClick={() => {
              setShowAddDeptModal(false);
              setShowDeptModal(true);
              setNewDeptName("");
            }}
          />
          <WSButton
            label="추가"
            variant="primary"
            disabled={!newDeptName.trim()}
            onClick={() => {
              if (newDeptName.trim()) {
                CreateDepartments(accessToken, newDeptName).then(() => {
                  setDepartments([...departments, newDeptName.trim()]);
                  setNewDeptName("");
                  setShowAddDeptModal(false);
                  setShowDeptModal(true);
                });
              }
            }}
          />
        </WSModalActions>
      </WSModal>
    </div>
  );
}
