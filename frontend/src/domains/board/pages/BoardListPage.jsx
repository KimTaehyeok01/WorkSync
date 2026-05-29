import { useState, useEffect } from "react";
import useAuthContext from "../../../store/AuthContext";
import { getBoards, getMyInfo, getPosts, getDepartmentBoard } from "../services/boardApi";
import { useNavigate } from "react-router-dom";
import { Plus, Search, ChevronDown } from "lucide-react";
import {
  WSCard,
  WSAvatar,
  WSButton,
  WSPagination,
} from "../../../components/common/CommonWidgets";
import s from "./BoardListPage.module.css";

// const CATEGORIES = [
//   { value: "all", label: "전체" },
//   { value: "notice", label: "공지사항" },
//   { value: "dept", label: "부서 게시판" },
//   { value: "free", label: "자유 게시판" },
// ];

export default function Board() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [posts, setPosts] = useState([]);
  const [category, setCategory] = useState("all"); // 현재 선택된 값
  const [categories, setCategories] = useState([
    // 드롭다운 목록 전체
    { value: "all", label: "전체" },
  ]); // 전체 -> 고정으로 유지
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [myDepartmentName, setMyDepartmentName] = useState("");
  const [role, setRole] = useState(null);
  const [deptBoardId, setDeptBoardId] = useState(null); // 내 부서게시판 id

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
    });

    // 부서게시판 id 조회 (로그인 사용자 부서 기준)
    getDepartmentBoard(accessToken).then((deptBoard) => {
      if (deptBoard) setDeptBoardId(deptBoard.id);
    });

    getBoards(accessToken).then((data) => {
      if (!data) return;

      // DEPARTMENT 타입 제외 후 드롭다운 구성 (부서게시판은 하나로 고정)
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
            setPosts(results.flat().filter(Boolean));
          }
        );
      });
    } else if (category === "DEPARTMENT") {
      // 부서게시판: 내 부서 게시판 id로만 조회
      if (!deptBoardId) return;
      getPosts(deptBoardId, accessToken).then((data) => {
        setPosts(data ?? []);
      });
    } else {
      getPosts(category, accessToken).then((data) => {
        if (!data) return;
        setPosts(data);
      });
    }
  }, [category, accessToken, deptBoardId]);

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.selectWrap}>
          <select
            value={category}
            onChange={(e) => {
              const raw = e.target.value;
              const val =
                raw === "all" ? "all"
                : raw === "DEPARTMENT" ? "DEPARTMENT"
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
            <p className={s.emptyTitle}>게시글이 없습니다</p>
            <p className={s.emptyDesc}>첫 번째 게시글을 작성해 보세요.</p>
          </div>
        ) : (
          <div className={s.list}>
            {pagePosts.map((post) => {
              console.log(post);
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
