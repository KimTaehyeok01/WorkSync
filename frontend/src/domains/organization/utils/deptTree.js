// parentId 로 연결된 평면 부서 목록을 트리·들여쓰기 목록으로 바꾸는 유틸
export function buildDeptTree(departments) {
  const nodes = new Map(
    departments.map((dept) => [dept.id, { ...dept, children: [] }]),
  );
  const roots = [];
  nodes.forEach((node) => {
    const parent = node.parentId != null ? nodes.get(node.parentId) : null;
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return roots;
}

// 트리를 깊이(depth) 정보가 붙은 1차원 목록으로 펼친다 (화면 들여쓰기용)
export function flattenDeptTree(departments) {
  const result = [];
  const walk = (list, depth) => {
    list.forEach((node) => {
      result.push({ ...node, depth });
      walk(node.children, depth + 1);
    });
  };
  walk(buildDeptTree(departments), 0);
  return result;
}

// 자기 자신 포함, 모든 하위 부서 id
export function getDeptSubtreeIds(departments, id) {
  const ids = new Set([id]);
  let added = true;
  while (added) {
    added = false;
    departments.forEach((dept) => {
      if (dept.parentId != null && ids.has(dept.parentId) && !ids.has(dept.id)) {
        ids.add(dept.id);
        added = true;
      }
    });
  }
  return ids;
}
