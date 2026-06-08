import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Plus, MoreVertical, ChevronDown, Search } from "lucide-react";
import { APPROVAL_DOCS } from "../../../constants/mockData";
import {
  getMyInfo,
  getMyApprovals,
  getApprovalById,
  deleteApproval,
} from "../services/approvalApi";
import {
  WSAvatar,
  WSPagination,
} from "../../../components/common/CommonWidgets";
import useAuthContext from "../../../store/AuthContext";
import s from "./ApprovalListPage.module.css";

const STATUS_CONFIG = {
  IN_PROGRESS: { label: "대기", bg: "#FEF3C7", text: "#92400E" },
  APPROVED: { label: "승인", bg: "#D1FAE5", text: "#065F46" },
  REJECTED: { label: "반려", bg: "#FEE2E2", text: "#991B1B" },
};

const STATUS_OPTIONS = [
  { key: "all", label: "전체" },
  { key: "IN_PROGRESS", label: "대기" },
  { key: "REJECTED", label: "반려" },
  { key: "APPROVED", label: "승인" },
];

export default function Approval() {
  const { id } = useParams();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [openDropdown, setOpenDropdown] = useState(null);
  // const [statusFilter, setStatusFilter] = useState("all");
  const [status, setStatus] = useState("all");
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [myApprovals, setMyApprovals] = useState([]);

  useEffect(() => {
    if (!accessToken) return;

    getMyApprovals(accessToken, status).then((data) => {
      if (!data) return;

      setMyApprovals(data);
    });
  }, [accessToken, status]);

  const filtered = (myApprovals ?? []).filter((doc) => {
    const matchSearch =
      doc.title
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase()) ||
      doc.id
        .replace(/\s/g, "")
        .toLowerCase()
        .includes(search.replace(/\s/g, "").toLowerCase());
    const matchStatus = status === "all" || doc.status === status;
    return matchSearch && matchStatus;
  });

  const perPage = 10;
  const paginatedDocs = filtered.slice((page - 1) * perPage, page * perPage);
  const statusLabel =
    STATUS_OPTIONS.find((o) => o.key === status)?.label || "전체";

  return (
    <div>
      <div className={s.filterBar}>
        <div className={s.searchSpace}>
          <div className={s.leftGroup}>
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
                        setStatus(item.key);
                        setOpenDropdown(null);
                        setPage(1);
                      }}
                      className={s.ddItem}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className={s.search}>
              <Search size={16} className={s.searchIcon} />
              <input
                type="text"
                placeholder="검색어를 입력하세요"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
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
              <div className={s.cardMore}>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setOpenDropdown(openDropdown === doc.id ? null : doc.id);
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
                        navigate(`/approval/${doc.id}/edit`);
                        e.stopPropagation();
                        setOpenDropdown(null);
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
                          try {
                            await deleteApproval(accessToken, doc.id);
                            getMyApprovals(accessToken, status).then((data) => {
                              if (!data) return;
                              setMyApprovals(data);
                            });
                          } catch (err) {
                            console.error("삭제 에러 : " + err);
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
              <div style={{ display: `flex`, justifyContent: `space-between` }}>
                <p className={s.cardDate}>
                  {new Date(doc.createdAt).toLocaleDateString("ko-KR")}
                </p>
                <p className={s.requesterRole}>{doc.formName}</p>
              </div>
            </div>
          );
        })}
      </div>

      <WSPagination
        total={filtered.length}
        page={page}
        perPage={perPage}
        onPageChange={setPage}
      />
    </div>
  );
}
