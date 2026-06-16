import { useState, useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  Plus,
  MoreVertical,
  ChevronDown,
  Search,
  ClipboardList,
} from "lucide-react";
import { APPROVAL_DOCS } from "../../../constants/mockData";
import useAuthContext from "../../../store/AuthContext";
import s from "./ApprovalListPage.module.css";
import {
  getMyInfo,
  getMyApprovals,
  getApprovalById,
  deleteApproval,
  getPendingApproval,
  getReferenceApprovals,
  getApprovalInbox,
} from "../services/approvalApi";
import {
  WSAvatar,
  WSPagination,
  WSEmptyState,
} from "../../../components/common/CommonWidgets";

const STATUS_CONFIG = {
  IN_PROGRESS: { label: "대기", bg: "#FEF3C7", text: "#92400E" },
  APPROVED: { label: "승인", bg: "#D1FAE5", text: "#065F46" },
  REJECTED: { label: "반려", bg: "#FEE2E2", text: "#991B1B" },
};

const BOX_OPTIONS = [
  { key: "inbox", label: "결재함" },
  { key: "my", label: "기안함" },
  { key: "reference", label: "참조함" },
];

const STATUS_OPTIONS = [
  { key: "all", label: "전체" },
  { key: "IN_PROGRESS", label: "대기" },
  { key: "REJECTED", label: "반려" },
  { key: "APPROVED", label: "승인" },
];

export default function Approval() {
  const [search, setSearch] = useState("");
  // 뒤로가기 버튼 눌렀을 때 이전 화면으로 돌아갈 수 있도록 URL 저장
  const [searchParams, setSearchParams] = useSearchParams();
  const [cache, setCache] = useState({}); // 캐시 저장소
  const [page, setPage] = useState(1);
  const [openDropdown, setOpenDropdown] = useState(null);
  const [docs, setDocs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const boxType = searchParams.get("box") ?? "inbox"; // 기본값
  const status = searchParams.get("status") ?? "all";
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();

  // 탭/상태 전환 시 매번 api 재호출로 인한 로딩 지연 문제 해결
  // 동일한 탭/상태 재방문 시 기존 데이터 즉시 반환
  useEffect(() => {
    if (!accessToken) return;

    const cacheKey = `${boxType}-${status}`;

    // 캐시에 있으면 API 안 부르고 바로 사용
    if (cache[cacheKey]) {
      setDocs(cache[cacheKey]);
      return;
    }

    console.log("cache: ", cache);
    setIsLoading(true); // 캐시 없을 때만 로딩 시작

    let api;
    if (boxType === "inbox") api = getApprovalInbox(accessToken, status);
    else if (boxType === "my") api = getMyApprovals(accessToken, status);
    else api = getReferenceApprovals(accessToken, status);

    api
      .then((data) => {
        setDocs(data ?? []);
        setCache((prev) => ({ ...prev, [cacheKey]: data ?? [] }));
      })
      .finally(() => setIsLoading(false));
  }, [accessToken, boxType, status]);

  const filtered = (docs ?? []).filter((doc) => {
    const matchSearch =
      doc.title
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase()) ||
      String(doc.id)
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase());
    const matchStatus = status === "all" || doc.status === status;
    return matchSearch && matchStatus;
  });

  const perPage = 9;
  const paginatedDocs = filtered.slice((page - 1) * perPage, page * perPage);
  const boxLabel =
    BOX_OPTIONS.find((o) => o.key === boxType)?.label || "기안함";
  const statusLabel =
    STATUS_OPTIONS.find((o) => o.key === status)?.label || "전체";

  if (isLoading) {
    return null;
  }

  return (
    <div>
      <div className={s.filterBar}>
        <div className={s.searchSpace}>
          <div className={s.leftGroup}>
            <div className={s.dd}>
              <button
                onClick={() =>
                  setOpenDropdown(openDropdown === "box" ? null : "box")
                }
                className={s.ddBtn}
              >
                <span>{boxLabel}</span>
                <ChevronDown size={14} color="#9CA3AF" />
              </button>
              {openDropdown === "box" && (
                <div className={s.ddMenu}>
                  {BOX_OPTIONS.map((item) => (
                    <button
                      key={item.key}
                      onClick={() => {
                        setSearchParams({ box: item.key, status: "all" });
                        setPage(1);
                        setOpenDropdown(null);
                      }}
                      className={s.ddItem}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
            {/* 전체/대기/승인/반려 상태 드롭다운 (참조함에서는 숨김) */}
            {boxType !== "reference" && (
              <div className={s.dd}>
                <button
                  onClick={() =>
                    setOpenDropdown(openDropdown === "status" ? null : "status")
                  }
                  className={s.ddBtn}
                >
                  <span>{statusLabel}</span>
                  <ChevronDown size={14} color="#9CA3AF" />
                </button>
                {openDropdown === "status" && (
                  <div className={s.ddMenu}>
                    {STATUS_OPTIONS.map((item) => (
                      <button
                        key={item.key}
                        onClick={() => {
                          setSearchParams({ box: boxType, status: item.key });
                          setPage(1);
                          setOpenDropdown(null);
                        }}
                        className={s.ddItem}
                      >
                        {item.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            <div className={s.search}>
              <Search size={16} className={s.searchIcon} />
              <input
                type="text"
                placeholder="검색어를 입력하세요"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                className={s.searchInput}
              />
            </div>
          </div>
          <button
            onClick={() => navigate("/approval/new")}
            className={s.newBtn}
          >
            <Plus size={16} />
            <span>전체 문서 등록</span>
          </button>
        </div>
      </div>

      {paginatedDocs.length === 0 ? (
        <div className={s.empty}>
          <WSEmptyState
            icon={<ClipboardList size={32} />}
            title="등록된 결재가 없습니다"
            description="첫 결재를 등록하거나 검색 조건을 변경해 보세요."
          />
        </div>
      ) : (
        <div className={s.grid}>
          {paginatedDocs.map((doc) => {
            const config = STATUS_CONFIG[doc.status] ?? {
              label: doc.status,
              bg: "#E5E7EB",
              text: "#374151",
            };
            return (
              <div
                key={doc.id}
                className={s.card}
                onClick={() => navigate(`/approval/${doc.id}`)}
              >
                <>
                  {boxType === "my" && (
                    <div className={s.cardMore}>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOpenDropdown(
                            openDropdown === doc.id ? null : doc.id,
                          );
                        }}
                        className={s.cardMoreBtn}
                      >
                        <MoreVertical size={16} />
                      </button>
                      {openDropdown === doc.id && (
                        <div className={s.cardMoreMenu}>
                          <button
                            className={s.ddItem}
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(null);
                              if (doc.status !== "IN_PROGRESS") {
                                alert("대기 중인 문서만 수정할 수 있습니다.");
                                return;
                              }
                              navigate(`/approval/${doc.id}/edit`);
                            }}
                          >
                            수정
                          </button>
                          <button
                            className={`${s.ddItem} ${s.ddItemDanger}`}
                            onClick={async (e) => {
                              e.stopPropagation();
                              setOpenDropdown(null);
                              if (confirm("게시글을 삭제하시겠습니까?")) {
                                if (doc.status !== "IN_PROGRESS") {
                                  alert("대기 중인 문서만 삭제할 수 있습니다.");
                                  return;
                                }
                                try {
                                  await deleteApproval(accessToken, doc.id);
                                  setDocs((prev) =>
                                    prev.filter((d) => d.id !== doc.id),
                                  );
                                } catch (err) {
                                  alert("삭제 실패했습니다.");
                                }
                              }
                            }}
                          >
                            삭제
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </>

                <div
                  className={s.statusBadge}
                  style={{
                    "--status-bg": config.bg,
                    "--status-color": config.text,
                  }}
                >
                  {config.label}
                </div>

                <h3 className={s.cardTitle}>{doc.title}</h3>

                <div className={s.requesterRow}>
                  <WSAvatar src={null} name={doc.drafterName} size={28} />

                  <p className={s.requesterName}>{doc.drafterName}</p>
                </div>
                <hr className={s.divider} />
                <div
                  style={{ display: `flex`, justifyContent: `space-between` }}
                >
                  <p className={s.cardDate}>
                    {new Date(doc.createdAt).toLocaleDateString("ko-KR")}
                  </p>
                  <p className={s.requesterRole}>{doc.formName}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <WSPagination
        total={filtered.length}
        page={page}
        perPage={perPage}
        onPageChange={setPage}
      />
    </div>
  );
}
