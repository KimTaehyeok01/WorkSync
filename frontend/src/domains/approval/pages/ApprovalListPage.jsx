import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, MoreVertical, ChevronDown, Search } from "lucide-react";
import { APPROVAL_DOCS } from "../../../constants/mockData";
import { WSAvatar, WSPagination } from "../../../components/common/CommonWidgets";
import s from "./ApprovalListPage.module.css";

const STATUS_CONFIG = {
  pending:  { label: "대기", bg: "#FEF3C7", text: "#92400E" },
  approved: { label: "승인", bg: "#D1FAE5", text: "#065F46" },
  rejected: { label: "반려", bg: "#FEE2E2", text: "#991B1B" },
};

const STATUS_OPTIONS = [
  { key: "all", label: "전체" },
  { key: "pending", label: "대기" },
  { key: "rejected", label: "반려" },
  { key: "approved", label: "승인" },
];

export default function Approval() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [page, setPage] = useState(1);
  const [openDropdown, setOpenDropdown] = useState(null);
  const navigate = useNavigate();

  const filtered = APPROVAL_DOCS.filter((doc) => {
    const matchSearch =
      doc.title.toLowerCase().includes(search.toLowerCase()) ||
      doc.id.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === "all" || doc.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const perPage = 10;
  const paginatedDocs = filtered.slice((page - 1) * perPage, page * perPage);
  const statusLabel = STATUS_OPTIONS.find((o) => o.key === statusFilter)?.label || "전체";

  return (
    <div>
      <div className={s.filterBar}>
        <div className={s.dd}>
          <button
            onClick={() => setOpenDropdown(openDropdown === "status" ? null : "status")}
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
                  onClick={() => { setStatusFilter(item.key); setOpenDropdown(null); setPage(1); }}
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

        <button onClick={() => navigate("/approval/new")} className={s.newBtn}>
          <Plus size={16} />
          <span>전체 문서 등록</span>
        </button>
      </div>

      <div className={s.grid}>
        {paginatedDocs.map((doc) => {
          const config = STATUS_CONFIG[doc.status];
          return (
            <div key={doc.id} className={s.card} onClick={() => navigate(`/approval/${doc.id}`)}>
              <div className={s.cardMore}>
                <button
                  onClick={(e) => { e.stopPropagation(); setOpenDropdown(openDropdown === doc.id ? null : doc.id); }}
                  className={s.cardMoreBtn}
                >
                  <MoreVertical size={16} />
                </button>
                {openDropdown === doc.id && (
                  <div className={s.cardMoreMenu}>
                    <button
                      className={s.ddItem}
                      onClick={(e) => { e.stopPropagation(); setOpenDropdown(null); }}
                    >수정</button>
                    <button
                      className={`${s.ddItem} ${s.ddItemDanger}`}
                      onClick={(e) => { e.stopPropagation(); setOpenDropdown(null); }}
                    >삭제</button>
                  </div>
                )}
              </div>

              <div className={s.statusBadge} style={{ "--status-bg": config.bg, "--status-color": config.text }}>
                {config.label}
              </div>

              <h3 className={s.cardTitle}>{doc.title}</h3>

              <div className={s.requesterRow}>
                <WSAvatar src={doc.requester.avatar} name={doc.requester.name} size={28} />
                <div>
                  <p className={s.requesterName}>{doc.requester.name}</p>
                  <p className={s.requesterRole}>{doc.requester.role}</p>
                </div>
              </div>

              <p className={s.cardDate}>{doc.date}</p>
            </div>
          );
        })}
      </div>

      <WSPagination total={filtered.length} page={page} perPage={perPage} onPageChange={setPage} />
    </div>
  );
}
