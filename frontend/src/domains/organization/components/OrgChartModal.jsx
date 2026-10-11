// 부서 트리(왼쪽)와 선택한 부서의 사원 카드(오른쪽)로 보여주는 전체 조직도 모달
import { useEffect, useMemo, useState } from "react";
import { ChevronDown, ChevronRight, Network } from "lucide-react";
import {
  WSModal,
  WSEmptyState,
  WSAvatar,
} from "../../../components/common/CommonWidgets";
import { getDepartments, getEmployee } from "../services/organizationListApi";
import { buildDeptTree, getDeptSubtreeIds } from "../utils/deptTree";
import style from "./OrgChartModal.module.css";

const JOB_GRADE = {
  STAFF: "사원",
  SENIOR: "주임",
  ASSISTANT_MANAGER: "대리",
  MANAGER: "과장",
  GENERAL_MANAGER: "부장",
  DIRECTOR: "이사",
  VICE_PRESIDENT: "부사장",
  PRESIDENT: "사장",
  CEO: "대표이사",
};

const ALL = "all";

export default function OrgChartModal({ isOpen, onClose, accessToken }) {
  const [departments, setDepartments] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [search, setSearch] = useState("");
  const [selectedRawId, setSelectedId] = useState(ALL);
  const [collapsed, setCollapsed] = useState(() => new Set());
  const [includeSub, setIncludeSub] = useState(true);

  useEffect(() => {
    if (!isOpen || !accessToken) return;
    getDepartments(accessToken).then((data) => {
      setDepartments(Array.isArray(data?.data) ? data.data : []);
    });
    getEmployee(accessToken).then((data) => {
      setEmployees(Array.isArray(data?.data) ? data.data : []);
    });
  }, [isOpen, accessToken]);

  const tree = useMemo(() => buildDeptTree(departments), [departments]);

  // 부서별 소속 인원수 (하위 부서 포함)
  const countOf = useMemo(() => {
    const direct = new Map();
    employees.forEach((emp) => {
      direct.set(emp.departmentId, (direct.get(emp.departmentId) ?? 0) + 1);
    });
    const total = new Map();
    const sum = (node) => {
      const value =
        (direct.get(node.id) ?? 0) +
        node.children.reduce((acc, child) => acc + sum(child), 0);
      total.set(node.id, value);
      return value;
    };
    tree.forEach(sum);
    return total;
  }, [tree, employees]);

  const deptNameById = useMemo(
    () => new Map(departments.map((dept) => [dept.id, dept.name])),
    [departments],
  );

  // 선택했던 부서가 삭제돼 사라졌으면 전체로 되돌린다
  const selectedId =
    selectedRawId !== ALL &&
    departments.length > 0 &&
    !deptNameById.has(selectedRawId)
      ? ALL
      : selectedRawId;

  // 오른쪽 목록 — 선택 부서(옵션에 따라 하위 포함) + 이름 검색
  const sections = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    const matched = employees.filter((emp) =>
      emp.name.toLowerCase().includes(keyword),
    );

    if (selectedId === ALL) {
      const byDept = departments
        .map((dept) => ({
          id: dept.id,
          name: dept.name,
          members: matched.filter((emp) => emp.departmentId === dept.id),
        }))
        .filter((group) => group.members.length > 0);
      const unassigned = matched.filter((emp) => !emp.departmentId);
      if (unassigned.length > 0) {
        byDept.push({ id: "unassigned", name: "미배정", members: unassigned });
      }
      return byDept;
    }

    const targetIds = includeSub
      ? getDeptSubtreeIds(departments, selectedId)
      : new Set([selectedId]);
    return departments
      .filter((dept) => targetIds.has(dept.id))
      .map((dept) => ({
        id: dept.id,
        name: dept.name,
        members: matched.filter((emp) => emp.departmentId === dept.id),
      }))
      .filter((group) => group.members.length > 0);
  }, [departments, employees, search, selectedId, includeSub]);

  const toggleCollapse = (id) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const renderNode = (node, depth) => {
    const hasChildren = node.children.length > 0;
    const isCollapsed = collapsed.has(node.id);
    return (
      <div key={node.id}>
        <div
          className={`${style.treeRow} ${selectedId === node.id ? style.treeRowActive : ""}`}
          style={{ paddingLeft: 8 + depth * 16 }}
        >
          {hasChildren ? (
            <button
              type="button"
              className={style.treeToggle}
              onClick={() => toggleCollapse(node.id)}
              aria-label={isCollapsed ? "펼치기" : "접기"}
            >
              {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
            </button>
          ) : (
            <span className={style.treeToggleSpacer} />
          )}
          <button
            type="button"
            className={style.treeLabel}
            onClick={() => setSelectedId(node.id)}
          >
            <span className={style.treeName}>{node.name}</span>
            <span className={style.treeCount}>{countOf.get(node.id) ?? 0}</span>
          </button>
        </div>
        {hasChildren && !isCollapsed && (
          <div>{node.children.map((child) => renderNode(child, depth + 1))}</div>
        )}
      </div>
    );
  };

  const selectedName =
    selectedId === ALL ? "전체" : (deptNameById.get(selectedId) ?? "전체");

  return (
    <WSModal isOpen={isOpen} onClose={onClose} title="조직도" size="xl">
      <div className={style.layout}>
        <aside className={style.sidebar}>
          <div className={style.treeRow + " " + (selectedId === ALL ? style.treeRowActive : "")}>
            <span className={style.treeToggleSpacer} />
            <button
              type="button"
              className={style.treeLabel}
              onClick={() => setSelectedId(ALL)}
            >
              <span className={style.treeName}>전체</span>
              <span className={style.treeCount}>{employees.length}</span>
            </button>
          </div>
          {tree.map((node) => renderNode(node, 0))}
        </aside>

        <section className={style.content}>
          <div className={style.toolbar}>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="이름으로 검색하세요."
              className={style.search}
              aria-label="조직도 사원 검색"
            />
            <label className={style.subToggle}>
              <input
                type="checkbox"
                checked={includeSub}
                onChange={(e) => setIncludeSub(e.target.checked)}
                disabled={selectedId === ALL}
              />
              하위조직 포함
            </label>
          </div>
          <div className={style.contentTitle}>{selectedName}</div>

          <div className={style.groupList}>
            {sections.length === 0 ? (
              <div className={style.emptyWrap}>
                <WSEmptyState
                  icon={<Network size={32} />}
                  title="표시할 사원이 없습니다"
                />
              </div>
            ) : (
              sections.map((group) => (
                <div key={group.id} className={style.group}>
                  <div className={style.groupHeader}>
                    <span className={style.groupName}>{group.name}</span>
                    <span className={style.groupCount}>{group.members.length}</span>
                  </div>
                  <div className={style.cardGrid}>
                    {group.members.map((member) => (
                      <div key={member.id} className={style.card}>
                        <WSAvatar
                          src={member.profileImage}
                          name={member.name}
                          size={40}
                        />
                        <div className={style.cardMeta}>
                          <span className={style.cardName}>{member.name}</span>
                          <span className={style.cardRank}>
                            {JOB_GRADE[member.jobGrade] || "-"}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </WSModal>
  );
}
