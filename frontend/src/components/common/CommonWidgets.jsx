import { useState } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Search,
  Filter,
  X,
  ChevronDown,
} from "lucide-react";
import s from "./Widgets.module.css";

export * from "./FormComponents";
export * from "./LayoutComponents";

/** WS Card */
export function WSCard({
  title,
  subtitle,
  action,
  children,
  className = "",
  noPad = false,
  style,
}) {
  return (
    <div className={`${s.card} ${className}`} style={style}>
      {(title || action) && (
        <div className={s.cardHeader}>
          <div>
            {title && <h3 className={s.cardTitle}>{title}</h3>}
            {subtitle && <p className={s.cardSubtitle}>{subtitle}</p>}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      <div className={noPad ? s.cardBodyFlat : s.cardBody}>{children}</div>
    </div>
  );
}

/** WS Stat Card */
export function WSStatCard({ label, value, sub, icon, color, trend }) {
  return (
    <div className={s.statCard}>
      <div
        className={s.statIcon}
        style={{ "--stat-bg": `${color}18`, "--stat-color": color }}
      >
        <span className={s.statIconGlyph}>{icon}</span>
      </div>
      <div className={s.statBody}>
        <p className={s.statLabel}>{label}</p>
        <p className={s.statValue}>{value}</p>
        {sub && <p className={s.statSub}>{sub}</p>}
        {trend && (
          <div className={s.trendRow}>
            <span
              className={`${s.trendBadge} ${trend.up ? s.trendUp : s.trendDown}`}
            >
              {trend.up ? "▲" : "▼"} {trend.value}
            </span>
            <span className={s.trendSub}>지난주 대비</span>
          </div>
        )}
      </div>
    </div>
  );
}

/** WS Badge */
const BADGE_STYLES = {
  pending: { bg: "#FEF3C7", color: "#D97706" },
  approved: { bg: "#D1FAE5", color: "#059669" },
  rejected: { bg: "#FEE2E2", color: "#DC2626" },
  high: { bg: "#FEE2E2", color: "#DC2626" },
  medium: { bg: "#FEF3C7", color: "#D97706" },
  low: { bg: "#DBEAFE", color: "#1D4ED8" },
  urgent: { bg: "#FCE7F3", color: "#BE185D" },
  online: { bg: "#D1FAE5", color: "#059669" },
  away: { bg: "#FEF3C7", color: "#D97706" },
  offline: { bg: "#F3F4F6", color: "#6B7280" },
  todo: { bg: "#F3F4F6", color: "#374151" },
  inProgress: { bg: "#DBEAFE", color: "#1D4ED8" },
  review: { bg: "#EDE9FE", color: "#7C3AED" },
  done: { bg: "#D1FAE5", color: "#059669" },
};

const STATUS_LABELS = {
  pending: "대기",
  approved: "승인",
  rejected: "반려",
  online: "온라인",
  away: "자리 비움",
  offline: "오프라인",
  todo: "준비중",
  inProgress: "진행중",
  done: "완료",
};

export function WSBadge({ status, label }) {
  const style = BADGE_STYLES[status] || { bg: "#F3F4F6", color: "#374151" };
  return (
    <span
      className={s.badge}
      style={{ "--badge-bg": style.bg, "--badge-color": style.color }}
    >
      {label || STATUS_LABELS[status] || status}
    </span>
  );
}

/** WS Avatar */
export function WSAvatar({ src, name, size = 32 }) {
  return (
    <img
      src={src}
      alt={name}
      className={s.avatar}
      style={{ "--avatar-size": `${size}px` }}
    />
  );
}

export function WSProfileMini({ avatar, name, sub }) {
  return (
    <div className={s.profileMini}>
      <WSAvatar src={avatar} name={name} size={30} />
      <div>
        <div className={s.profileMiniName}>{name}</div>
        {sub && <div className={s.profileMiniSub}>{sub}</div>}
      </div>
    </div>
  );
}

/** WS Pagination */
export function WSPagination({ total, page, perPage = 10, onPageChange }) {
  const totalPages = Math.ceil(total / perPage);
  const start = (page - 1) * perPage + 1;
  const end = Math.min(page * perPage, total);

  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
    if (totalPages <= 5) return i + 1;
    if (page <= 3) return i + 1;
    if (page >= totalPages - 2) return totalPages - 4 + i;
    return page - 2 + i;
  });

  return (
    <div className={s.pagination}>
      <div className={s.pagerControls}>
        <button
          onClick={() => onPageChange?.(page - 1)}
          disabled={page <= 1}
          className={s.pagerBtn}
          aria-label="이전 페이지"
        >
          <ChevronLeft size={14} />
        </button>
        {pages.map((p) => (
          <button
            key={p}
            onClick={() => onPageChange?.(p)}
            className={`${s.pagerBtn} ${p === page ? s.pagerBtnActive : ""}`}
            aria-current={p === page ? "page" : undefined}
          >
            {p}
          </button>
        ))}
        {totalPages > 5 && page < totalPages - 2 && (
          <>
            <span className={s.pagerEllipsis}>...</span>
            <button
              onClick={() => onPageChange?.(totalPages)}
              className={s.pagerBtn}
              aria-label={`마지막 페이지 ${totalPages}`}
            >
              {totalPages}
            </button>
          </>
        )}
        <button
          onClick={() => onPageChange?.(page + 1)}
          disabled={page >= totalPages}
          className={s.pagerBtn}
          aria-label="다음 페이지"
        >
          <ChevronRight size={14} />
        </button>
      </div>
    </div>
  );
}

/** WS Search Filters */
export function WSSearchFilters({
  placeholder = "검색...",
  filters = [],
  search,
  onSearchChange,
  activeFilters = {},
  onFilterChange,
}) {
  const [openFilter, setOpenFilter] = useState(null);
  const activeCount = Object.values(activeFilters).filter(Boolean).length;

  return (
    <div className={s.searchFilters}>
      <div className={s.searchInputBox}>
        <Search size={14} className={s.searchInputIcon} />
        <input
          type="text"
          placeholder={placeholder}
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className={s.searchInputField}
        />
        {search && (
          <button
            onClick={() => onSearchChange("")}
            className={s.searchInputClear}
            aria-label="검색어 지우기"
          >
            <X size={12} />
          </button>
        )}
      </div>

      {filters.map((filter) => {
        const active = !!activeFilters[filter.key];
        return (
          <div key={filter.key} className={s.filterWrap}>
            <button
              onClick={() =>
                setOpenFilter(openFilter === filter.key ? null : filter.key)
              }
              className={`${s.filterBtn} ${active ? s.filterBtnActive : ""}`}
              aria-haspopup="menu"
              aria-expanded={openFilter === filter.key}
            >
              <Filter size={12} />
              <span>{activeFilters[filter.key] || filter.label}</span>
              <ChevronDown size={12} />
            </button>
            {openFilter === filter.key && (
              <div className={s.filterMenu} role="menu">
                <button
                  className={`${s.filterMenuItem} ${s.filterMenuClear}`}
                  onClick={() => {
                    onFilterChange?.(filter.key, "");
                    setOpenFilter(null);
                  }}
                  role="menuitem"
                >
                  전체 {filter.label}
                </button>
                {filter.options.map((opt) => (
                  <button
                    key={opt}
                    className={`${s.filterMenuItem} ${activeFilters[filter.key] === opt ? s.filterMenuItemSel : ""}`}
                    onClick={() => {
                      onFilterChange?.(filter.key, opt);
                      setOpenFilter(null);
                    }}
                    role="menuitem"
                  >
                    {opt}
                  </button>
                ))}
              </div>
            )}
          </div>
        );
      })}

      {activeCount > 0 && (
        <button
          className={s.filterResetBtn}
          onClick={() => filters.forEach((f) => onFilterChange?.(f.key, ""))}
        >
          <X size={12} />
          초기화 ({activeCount})
        </button>
      )}
    </div>
  );
}

/** WS Progress */
export function WSProgress({
  value,
  color = "#1A73E8",
  label,
  showLabel = true,
}) {
  return (
    <div className={s.progressWrap}>
      {(label || showLabel) && (
        <div className={s.progressTop}>
          {label && <span className={s.progressLabel}>{label}</span>}
          {showLabel && (
            <span
              className={s.progressValue}
              style={{ "--progress-color": color }}
            >
              {value}%
            </span>
          )}
        </div>
      )}
      <div className={s.progressTrack}>
        <div
          className={s.progressFill}
          style={{ "--progress-width": `${value}%`, "--progress-color": color }}
        />
      </div>
    </div>
  );
}

/** WS Tag */
const TAG_COLORS = {
  Design: { bg: "#FCE7F3", color: "#BE185D" },
  UX: { bg: "#EDE9FE", color: "#7C3AED" },
  Backend: { bg: "#DBEAFE", color: "#1D4ED8" },
  Frontend: { bg: "#D1FAE5", color: "#059669" },
  API: { bg: "#FEF3C7", color: "#D97706" },
  Docs: { bg: "#F3F4F6", color: "#374151" },
  DevOps: { bg: "#FCE7F3", color: "#9D174D" },
  Bug: { bg: "#FEE2E2", color: "#B91C1C" },
  Security: { bg: "#FEF3C7", color: "#92400E" },
  Feature: { bg: "#D1FAE5", color: "#065F46" },
  Integration: { bg: "#DBEAFE", color: "#1E40AF" },
  Component: { bg: "#EDE9FE", color: "#5B21B6" },
};

export function WSTag({ label }) {
  const style = TAG_COLORS[label] || { bg: "#F3F4F6", color: "#374151" };
  return (
    <span
      className={s.tag}
      style={{ "--tag-bg": style.bg, "--tag-color": style.color }}
    >
      {label}
    </span>
  );
}

/** WS Button */
export function WSButton({
  label,
  icon,
  variant = "primary",
  size = "md",
  onClick,
  disabled,
  className = "",
}) {
  const variantCls =
    variant === "secondary"
      ? s.btnSecondary
      : variant === "ghost"
        ? s.btnGhost
        : variant === "danger"
          ? s.btnDanger
          : s.btnPrimary;
  const sizeCls = size === "sm" ? s.btnSizeSm : s.btnSizeMd;
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`${s.btn} ${variantCls} ${sizeCls} ${className}`}
      aria-label={label}
    >
      {icon && (
        <span
          style={{ display: "flex", alignItems: "center", marginTop: "2px" }}
        >
          {icon}
        </span>
      )}
      {label}
    </button>
  );
}

/** WS Section Header */
export function WSSectionHeader({ title, action }) {
  return (
    <div className={s.sectionHeader}>
      <h2 className={s.sectionTitle}>{title}</h2>
      {action}
    </div>
  );
}
