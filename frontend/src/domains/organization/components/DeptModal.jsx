import { useState } from "react";
import { Plus, ChevronRight, Building2 } from "lucide-react";
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
import style from "../pages/OrganizationListPage.module.css";
import useAuthContext from "../../../store/AuthContext";

export default function DeptModal({
  departments = [],
  setDepartments,
  isOpen,
  onClose,
  accessToken,
  addDeptName,
  setAddDeptName,
  editDeptName,
  setEditDeptName,
  editDeptId,
  setEditDeptId,
  handleAdd,
  handleDelete,
  handleEdit,
  modalView,
  setModalView,
}) {
  return (
    <>
      <WSModal
        isOpen={isOpen && modalView === "dept"}
        onClose={onClose}
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
                setModalView("add");
              },
              icon: <Plus size={16} />,
            }}
          />
        ) : (
          <>
            <button
              onClick={() => {
                setModalView("add");
              }}
              className={style.addDeptBtn}
            >
              <Plus size={16} />
              신규 부서 추가
            </button>

            <div className={style.deptList}>
              {departments.map((dept) => (
                <button
                  key={dept.id}
                  className={style.deptItem}
                  onClick={() => {
                    setModalView("edit");
                    setEditDeptId(`${dept.id}`);
                    setEditDeptName(`${dept.name}`);
                  }}
                >
                  <span>{dept.name}</span>
                  <ChevronRight size={16} className={style.deptChevron} />
                </button>
              ))}
            </div>
          </>
        )}
      </WSModal>

      <WSModal
        isOpen={isOpen && modalView === "add"}
        onClose={onClose}
        title="부서 등록"
        size="md"
      >
        <WSFormField label="부서명" required>
          <WSInput
            type="text"
            value={addDeptName}
            onChange={(e) => setAddDeptName(e.target.value)}
            placeholder="부서명을 입력하세요"
          />
        </WSFormField>

        <WSModalActions>
          <WSButton
            label="취소"
            variant="secondary"
            onClick={() => {
              setModalView("dept");
            }}
          />
          <WSButton
            label="추가"
            variant="primary"
            disabled={!addDeptName.trim()}
            onClick={handleAdd}
          />
        </WSModalActions>
      </WSModal>

      <WSModal
        isOpen={isOpen && modalView === "edit"}
        onClose={onClose}
        title="부서 수정"
        size="md"
      >
        <WSFormField label="부서명" required>
          <WSInput
            type="text"
            value={editDeptName}
            onChange={(e) => setEditDeptName(e.target.value)}
            placeholder="부서명을 입력하세요"
          />
        </WSFormField>

        <WSModalActions>
          <WSButton
            label="취소"
            variant="secondary"
            onClick={() => {
              setModalView("dept");
            }}
          />
          <WSButton
            label="삭제"
            variant="primary"
            disabled={!editDeptId}
            onClick={handleDelete}
          />
          <WSButton
            label="수정"
            variant="primary"
            disabled={!editDeptName.trim()}
            onClick={handleEdit}
          />
        </WSModalActions>
      </WSModal>
    </>
  );
}
