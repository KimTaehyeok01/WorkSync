import { useState, useEffect } from "react";
import useAuthContext from "../../../store/AuthContext";
import { getBoards, getPosts } from "../services/boardApi";
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

  // 카테고리 + 검색어 적용하여 정렬
  const filteredPosts = posts.filter((p) => {
    console.log("현재 category : ", category);
    console.log(
      "posts[0] boardId : ",
      posts[0]?.boardId,
      typeof posts[0]?.boardId,
    );

    const matchCat = category === "all" || p.boardId === category;
    const matchSearch =
      !search ||
      p.title.toLowerCase().includes(search.toLowerCase()) || // 제목에 검색어 포함되면 true
      p.content.toLowerCase().includes(search.toLowerCase()); // 내용에 검색어 포함되면 true
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

    getBoards(accessToken).then((data) => {
      if (!data) return;

      // API 데이터를 드롭다운 형식으로 변환
      const apiCategories = data
        .sort((a, b) => a.id - b.id) // boardId 순으로 드롭다운 정렬
        .map((board) => ({
          value: board.id, // 1, 2, 3
          label: board.name, //"공지사항", "부서게시판", "자유게시판"
        }));

      // 전체 + API 데이터 합치기
      setCategories([{ value: "all", label: "전체" }, ...apiCategories]);
    });
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    const boardId = category === "all" ? null : category;

    if (boardId === null) {
      Promise.all([
        getPosts(1, accessToken),
        getPosts(2, accessToken),
        getPosts(3, accessToken),
      ]).then(([data1, data2, data3]) => {
        setPosts([...(data1 ?? []), ...(data2 ?? []), ...(data3 ?? [])]);
      });
    } else {
      getPosts(boardId, accessToken).then((data) => {
        console.log("게시글 데이터 확인 : ", data[0]);
        if (!data) return;
        setPosts(data);
      });
    }
  }, [category, accessToken]);

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.selectWrap}>
          <select
            value={category}
            onChange={(e) => {
              const val =
                e.target.value === "all" ? "all" : Number(e.target.value);
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
              const isNotice = post.category === "notice";
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

                    <p className={s.rowContent}>
                      {post.content.slice(0, 200)}...
                    </p>
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
