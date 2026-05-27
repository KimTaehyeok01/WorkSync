import { useState, useEffect } from "react";
import { getBoards } from "../services/boardApi";
import { useNavigate } from "react-router-dom";
import { Plus, Search, ChevronDown } from "lucide-react";
import { BOARD_POSTS } from "../../../constants/mockData";
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
  const [category, setCategory] = useState("all"); // 현재 선택된 값
  const [categories, setCategories] = useState([
    // 드롭다운 목록 전체
    { value: "all", label: "전체" },
  ]); // 전체 -> 고정으로 유지
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);

  const filteredPosts = BOARD_POSTS.filter((p) => {
    const matchCat = category === "all" || p.category === category;
    const matchSearch =
      !search ||
      p.title.toLowerCase().includes(search.toLowerCase()) ||
      p.content.toLowerCase().includes(search.toLowerCase());
    return matchCat && matchSearch;
  });

  const sortedPosts = [...filteredPosts].sort((a, b) => {
    if (a.category === "notice" && b.category !== "notice") return -1;
    if (a.category !== "notice" && b.category === "notice") return 1;
    return 0;
  });

  const perPage = 10;
  const pagePosts = sortedPosts.slice((page - 1) * perPage, page * perPage);

  // API에서 받아온 게시판 목록
  useEffect(() => {
    const token = localStorage.getItem("refreshToken"); // 저장된 토큰 가져오기
    console.log("토큰 : ", token);

    getBoards(token).then((data) => {
      console.log("게시판 데이터 : ", data);
      if (!data) return;

      // API 데이터를 드롭다운 형식으로 변환
      const apiCategories = data.map((board) => ({
        value: board.boardType, //"FREE"
        label: board.name, //"자유게시판"
      }));

      // 전체 + API 데이터 합치기
      setCategories([{ value: "all", label: "전체" }, ...apiCategories]);
    });
  }, []);

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.selectWrap}>
          <select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value);
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
                  onClick={() => navigate(`/board/${post.id}`)}
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
                      <WSAvatar
                        src={post.author.avatar}
                        name={post.author.name}
                        size={20}
                      />
                      <span className={s.rowAuthor}>{post.author.name}</span>
                      <span className={s.rowDot}>·</span>
                      <span className={s.rowDate}>{post.createdAt}</span>
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
