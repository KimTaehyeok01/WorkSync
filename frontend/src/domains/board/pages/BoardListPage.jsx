import { useState, useEffect, useMemo } from "react";
import useAuthContext from "../../../store/AuthContext";
import s from "./BoardListPage.module.css";
import {
  getBoards,
  getMyInfo,
  getPosts,
  getDepartmentBoard,
  getDepartments,
} from "../services/boardApi";
import { useNavigate } from "react-router-dom";
import { Plus, Search, ChevronDown, ClipboardList } from "lucide-react";
import {
  WSCard,
  WSAvatar,
  WSButton,
  WSPagination,
  WSEmptyState,
} from "../../../components/common/CommonWidgets";

export default function Board() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [posts, setPosts] = useState([]);
  const [category, setCategory] = useState("all"); // 현재 선택된 값
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [myDepartmentName, setMyDepartmentName] = useState("");
  const [role, setRole] = useState(null);
  const [deptBoardId, setDeptBoardId] = useState(null); // 내 부서게시판 id
  const [departments, setDepartments] = useState([]); // ADMIN 부서 필터 목록
  const [deptFilter, setDeptFilter] = useState("all"); // ADMIN 선택한 부서 필터
  const [isLoading, setIsLoading] = useState(true);
  const [categories, setCategories] = useState([
    // 드롭다운 목록 전체
    { value: "all", label: "전체" },
  ]); // 전체 -> 고정으로 유지

  // 카테고리 + 검색어 적용하여 정렬
  const filteredPosts = posts.filter((p) => {
    // "DEPARTMENT" 선택 시 이미 내 부서 게시판 posts만 가져왔으므로 전부 통과
    const matchCat =
      category === "all" || category === "DEPARTMENT" || p.boardId === category;
    const matchSearch =
      !search ||
      p.title
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase()) ||
      p.content.toLowerCase().includes(search.toLowerCase());
    return matchCat && matchSearch;
  });

  // 공지사항을 상단으로
  const sortedPosts = [...filteredPosts].sort((a, b) => {
    // boardName으로 수정
    if (a.boardName === "공지사항" && b.boardName !== "공지사항") return -1;
    if (a.boardName !== "공지사항" && b.boardName === "공지사항") return 1;
    return 0;
  });

  const perPage = 10;
  // 현재 페이지 게시글만
  const pagePosts = sortedPosts.slice((page - 1) * perPage, page * perPage);

  // API에서 받아온 게시판 목록(드롭다운)
  useEffect(() => {
    if (!accessToken) return;

    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setMyDepartmentName(data.departmentName);
      setRole(data.role);
      // ADMIN이면 부서 필터링 후 부서 목록 로드
      if (data.role === "ADMIN") {
        getDepartments(accessToken).then((depts) => {
          setDepartments(depts ?? []);
        });
      }
    });

    // 부서게시판 id 조회 (로그인 사용자 부서 기준)
    getDepartmentBoard(accessToken).then((deptBoard) => {
      if (deptBoard) setDeptBoardId(deptBoard.id);
    });

    getBoards(accessToken).then((data) => {
      if (!data) return;

      // Department 타입 제외 후 드롭다운 구성 (부서게시판은 하나로 고정)
      const apiCategories = data
        .filter((board) => board.boardType !== "DEPARTMENT")
        .sort((a, b) => a.id - b.id)
        .map((board) => ({
          value: board.id,
          label: board.name,
        }));

      // 전체 + API 데이터 + 부서게시판 고정 항목
      setCategories([
        { value: "all", label: "전체" },
        ...apiCategories,
        { value: "DEPARTMENT", label: "부서 게시판" },
      ]);
    });
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    setIsLoading(true);
    setPosts([]); // category 변경 시 stale 데이터 즉시 초기화

    if (category === "all") {
      // 전체 조회: DEPARTMENT 타입 중 내 부서 게시판만 포함
      getBoards(accessToken).then((data) => {
        if (!data) return;
        const nonDeptIds = data
          .filter((b) => b.boardType !== "DEPARTMENT")
          .map((b) => b.id);
        const ids = deptBoardId ? [...nonDeptIds, deptBoardId] : nonDeptIds;
        Promise.all(ids.map((id) => getPosts(id, accessToken))).then(
          (results) => {
            // 전체 조회 최신순 정렬
            const flat = results.flat().filter(Boolean);
            const sorted = flat.sort(
              (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
            );
            setPosts(sorted);
            setIsLoading(false);
          },
        );
      });
    } else if (category === "DEPARTMENT") {
      // 부서게시판: 내 부서 게시판 id로 조회
      // ADMIN이면 deptFilter(특정 부서 or 전체) 전 부서 글 조회
      if (!deptBoardId) return;
      const departmentId =
        role === "ADMIN" && deptFilter !== "all" ? deptFilter : undefined;
      getPosts(deptBoardId, accessToken, departmentId).then((data) => {
        // 부서 게시판 최신순 정렬
        const sorted = (data ?? []).sort(
          (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
        );
        setPosts(sorted);
        setIsLoading(false);
      });
    } else {
      getPosts(category, accessToken).then((data) => {
        // 자유 게시판 최신순 정렬
        if (!data) return;
        const sorted = data.sort(
          (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
        );
        setPosts(sorted);
        setIsLoading(false);
      });
    }
  }, [category, accessToken, deptBoardId, role, deptFilter]);

  if (isLoading) return null;

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.selectWrap}>
          <select
            value={category}
            onChange={(e) => {
              const raw = e.target.value;
              const val =
                raw === "all"
                  ? "all"
                  : raw === "DEPARTMENT"
                    ? "DEPARTMENT"
                    : Number(raw);
              setCategory(val);
              setPage(1);
            }}
            className={s.select}
          >
            {categories.map((cat) => (
              <option key={cat.value} value={cat.value}>
                {cat.label}
              </option>
            ))}
          </select>
          <ChevronDown size={14} className={s.selectChevron} />
        </div>

        {/* ADMIN + 부서게시판 선택 시 부서 필터 드롭다운 */}
        {role === "ADMIN" && category === "DEPARTMENT" && (
          <div className={s.selectWrap}>
            <select
              value={deptFilter}
              onChange={(e) => {
                const raw = e.target.value;
                setDeptFilter(raw === "all" ? "all" : Number(raw));
                setPage(1);
              }}
              className={s.select}
            >
              <option value="all">전체 부서</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
            <ChevronDown size={14} className={s.selectChevron} />
          </div>
        )}

        <div className={s.search}>
          <Search size={15} className={s.searchIcon} />
          <input
            type="text"
            placeholder="검색어를 입력하세요."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            className={s.searchInput}
          />
        </div>

        <WSButton
          label="글쓰기"
          variant="primary"
          size="md"
          icon={<Plus size={15} />}
          onClick={() => navigate("/board/new")}
        />
      </div>

      <WSCard>
        {pagePosts.length === 0 ? (
          <div className={s.empty}>
            <WSEmptyState
              icon={<ClipboardList size={32} />}
              title="등록된 게시글이 없습니다"
              description="첫 게시글을 등록하거나 검색 조건을 변경해 보세요."
            />
          </div>
        ) : (
          <div className={s.list}>
            {pagePosts.map((post) => {
              const isNotice = post.boardName === "공지사항";
              return (
                <div
                  key={post.id}
                  onClick={() => navigate(`/board/${post.boardId}/${post.id}`)}
                  className={s.row}
                >
                  <div className={s.rowBody}>
                    <div className={s.rowHeader}>
                      {isNotice && (
                        <span className={s.noticeBadge}>공지사항</span>
                      )}
                      {/* ADMIN이 부서게시판 볼 때 작성자 부서 뱃지 표시 */}
                      {role === "ADMIN" &&
                        category === "DEPARTMENT" &&
                        post.authorDepartmentName && (
                          <span className={s.deptBadge}>
                            {post.authorDepartmentName}
                          </span>
                        )}
                      <p className={s.rowTitle}>{post.title}</p>
                    </div>

                    <p className={s.rowContent}>{post.content.slice(0, 200)}</p>
                    <div className={s.rowMeta}>
                      <WSAvatar src={null} name={post.authorName} size={20} />
                      <span className={s.rowAuthor}>{post.authorName}</span>
                      <span className={s.rowDot}>·</span>
                      <span className={s.rowDate}>
                        {new Date(post.createdAt).toLocaleDateString("ko-KR")}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </WSCard>

      {sortedPosts.length > 0 && (
        <WSPagination
          total={sortedPosts.length}
          page={page}
          perPage={perPage}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
