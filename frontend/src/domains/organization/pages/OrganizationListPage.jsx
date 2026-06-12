import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import { Plus, ChevronRight, Building2, ClipboardList } from "lucide-react";
import {
  WSAvatar,
  WSPagination,
  WSEmptyState,
  WSFormField,
  WSInput,
  WSButton,
} from "../../../components/common/CommonWidgets";
import {
  WSFilterBar,
  WSTableHeader,
  WSTableRow,
} from "../../../components/common/LayoutComponents";
import style from "./OrganizationListPage.module.css";
import DeptModal from "../components/DeptModal";
import { getMyInfo } from "../../../components/service/TopBarApi";
import {
  getDepartments,
  getEmployee,
  createDepartments,
  deleteDepartments,
  editDepartments,
} from "../services/organizationListApi";

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
  const [addDeptName, setAddDeptName] = useState("");
  const [editDeptName, setEditDeptName] = useState("");
  const [editDeptId, setEditDeptId] = useState("");
  const [modalView, setModalView] = useState("dept");
  const [myRole, setMyRole] = useState("");

  // 사용자 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      // console.log(data);
      setMyRole(data.data.role);
    });
  }, [accessToken]);

  // 부서 불러오기
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

  // 직원 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getEmployee(accessToken).then((data) => {
      // console.log(data);
      setEmployee(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  // 직급
  const JOB_GRADE = {
    STAFF: "사원",
    SENIOR: "주임",
    ASSISTANT_MANAGER: "대리",
    MANAGER: "과장",
    GENERAL_MANAGER: "부장",
    DIRECTOR: "이사",
    CEO: "대표",
  };

  // 목록 필터
  const filtered = employee.filter((item) => {
    const matchSearch =
      item.name.toLowerCase().includes(search.toLowerCase()) ||
      item.departmentName.toLowerCase().includes(search.toLowerCase());
    const matchDept = deptFilter === "all" || item.departmentId === deptFilter;
    return matchSearch && matchDept;
  });

  const perPage = 10;
  const paginatedData = filtered.slice((page - 1) * perPage, page * perPage);

  // 부서 추가 API
  async function handleAdd() {
    if (addDeptName.trim()) {
      await createDepartments(accessToken, addDeptName).then((response) => {
        // console.log("추가콘솔: " + response.data);
        // console.log("추가콘솔: " + departments);
        setDepartments([...departments, response.data]);
      });

      setModalView("dept");
    }
  }

  // 부서 삭제 API
  async function handleDelete() {
    try {
      const confirmText = confirm(
        "삭제 시 해당 부서 직원의 부서도 삭제됩니다. 삭제하시겠습니까?",
      );

      if (!confirmText) {
        return;
      }

      if (editDeptId) {
        await deleteDepartments(accessToken, editDeptId).then((response) => {
          // console.log("삭제콘솔: " + editDeptId);
          // console.log("삭제콘솔: " + departments);
          setDepartments(
            departments.filter((dept) => dept.id !== Number(editDeptId)),
          );
          setModalView("dept");
        });
      }
    } catch (error) {
      alert("삭제 오류입니다.");
    }
  }

  // 부서 수정 API
  async function handleEdit() {
    if (editDeptName.trim()) {
      try {
        const response = await editDepartments(
          accessToken,
          editDeptName,
          editDeptId,
        );
        if (!response.data) {
          alert("이미 존재하는 부서명입니다.");
          return;
        }
        setDepartments(
          departments.map((dept) =>
            dept.id === response.data.id ? response.data : dept,
          ),
        );
        setModalView("dept");
      } catch (error) {
        alert("이미 존재하는 부서입니다.");
      }
    }
  }

  return (
    <div>
      <WSFilterBar
        filters={[{ label: "부서 선택", key: "dept", options: DEPT_OPTIONS }]}
        filterValues={{ dept: deptFilter }}
        onFilterChange={(key, value) => {
          setDeptFilter(value);
          setPage(1);
        }}
        searchValue={search}
        onSearchChange={setSearch}
        searchPlaceholder="부서 또는 이름으로 검색하세요."
        actions={
          myRole === "ADMIN"
            ? [
                {
                  label: "부서 관리",
                  onClick: () => setDeptModal(true),
                  icon: <Plus size={16} />,
                  variant: "primary",
                },
                {
                  label: "직원 추가",
                  onClick: () => navigate("/organization/new"),
                  icon: <Plus size={16} />,
                  variant: "primary",
                },
              ]
            : []
        }
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
            <WSTableRow key={item.id} gridTemplate={GRID_TEMPLATE}>
              <p className={style.dept}>
                {item.departmentName ? item.departmentName : "-"}
              </p>
              <p className={style.rank}>{JOB_GRADE[item.jobGrade] || "-"}</p>
              {myRole === "ADMIN" ? (
                <div
                  className={style.nameCell}
                  onClick={() => navigate(`/organization/edit/${item.id}`)}
                  style={{ cursor: "pointer" }}
                >
                  <WSAvatar
                    src={item.profileImage}
                    name={item.name}
                    size={28}
                  />
                  <span className={style.name}>
                    {item.name ? item.name : "-"}
                  </span>
                </div>
              ) : (
                <div className={style.nameCell}>
                  <WSAvatar
                    src={item.profileImage}
                    name={item.name}
                    size={28}
                  />
                  <span className={style.name}>
                    {item.name ? item.name : "-"}
                  </span>
                </div>
              )}
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
        addDeptName={addDeptName}
        setAddDeptName={setAddDeptName}
        editDeptName={editDeptName}
        setEditDeptName={setEditDeptName}
        editDeptId={editDeptId}
        setEditDeptId={setEditDeptId}
        handleAdd={handleAdd}
        handleDelete={handleDelete}
        handleEdit={handleEdit}
        modalView={modalView}
        setModalView={setModalView}
      />
    </div>
  );
}
