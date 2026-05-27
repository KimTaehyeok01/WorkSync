import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import { Plus, ChevronRight, Building2, ClipboardList } from "lucide-react";
import {
  WSAvatar,
  WSPagination,
  WSFilterBar,
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
import DeptModal from "../components/DeptModal";
import { getDepartments, getEmployee } from "../services/organizationListApi";

const TH_COL = ["부서명", "직급", "이름", "이메일", "연락처", "입사일"];
const GRID_TEMPLATE = "1fr 1fr 1fr 1fr 1fr 1fr";

export default function OrganizationListPage() {
  const { accessToken } = useAuthContext();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("all");
  const [page, setPage] = useState(1);
  const [deptModal, setDeptModal] = useState(false);
  const [departments, setDepartments] = useState([]);
  const [employee, setEmployee] = useState([]);

  useEffect(() => {
    if (!accessToken) return;
    getDepartments(accessToken).then((data) => {
      // console.log(data);
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
      // console.log(data);
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
            onClick: () => setDeptModal(true),
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

      <DeptModal
        departments={departments}
        setDepartments={setDepartments}
        isOpen={deptModal}
        onClose={() => setDeptModal(false)}
        accessToken={accessToken}
      />
    </div>
  );
}
