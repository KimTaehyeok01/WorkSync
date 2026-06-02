import { X, CheckCircle, Paperclip } from "lucide-react";
import { useEffect, useId } from "react";
import { WSSearchInput, WSFilterDropdown } from "./FormComponents";
import s from "./Widgets.module.css";

/** 모달 */
export function WSModal({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
  size = "md",
}) {
  const titleId = useId();
  useEffect(() => {
    if (!isOpen) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;
  const sizeCls =
    size === "sm" ? s.modalSm : size === "lg" ? s.modalLg : s.modalMd;
  return (
    <div
      className={s.modalOverlay}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      role="presentation"
    >
      <div
        className={`${s.modal} ${sizeCls}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
      >
        <div className={s.modalHeader}>
          <div>
            <h2 id={titleId} className={s.modalTitle}>
              {title}
            </h2>
            {subtitle && <p className={s.modalSubtitle}>{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className={s.modalClose}
            type="button"
            aria-label="모달 닫기"
          >
            <X size={20} />
          </button>
        </div>
        <div className={s.modalBody}>{children}</div>
      </div>
    </div>
  );
}

/** 모달 액션 */
export function WSModalActions({ children, align = "right" }) {
  const alignCls =
    align === "left"
      ? s.modalActionsLeft
      : align === "center"
        ? s.modalActionsCenter
        : s.modalActionsRight;
  return <div className={`${s.modalActions} ${alignCls}`}>{children}</div>;
}

/** 빈 상태 */
export function WSEmptyState({
  icon,
  title,
  description,
  action,
  color = "#1A73E8",
}) {
  return (
    <div className={s.empty}>
      <div
        className={s.emptyIconWrap}
        style={{ "--empty-color-bg": `${color}18` }}
      >
        <div className={s.emptyIcon} style={{ "--empty-color": color }}>
          {icon}
        </div>
      </div>
      <p className={s.emptyTitle}>{title}</p>
      {description && <p className={s.emptyDesc}>{description}</p>}
      {action && (
        <button
          onClick={action.onClick}
          className={s.emptyAction}
          style={{ "--empty-action-bg": color }}
          type="button"
        >
          {action.icon}
          {action.label}
        </button>
      )}
    </div>
  );
}

/** 성공 화면 */
export function WSSuccessScreen({
  title,
  description,
  redirectLabel,
  isVisible,
}) {
  if (!isVisible) return null;
  return (
    <div className={s.successScreen}>
      <div className={s.successCard}>
        <div className={s.successIcon}>
          <CheckCircle size={40} className={s.successIconGlyph} />
        </div>
        <div>
          <p className={s.successTitle}>{title}</p>
          {description && <p className={s.successDesc}>{description}</p>}
        </div>
        {redirectLabel && (
          <div className={s.successRedirect}>{redirectLabel}</div>
        )}
      </div>
    </div>
  );
}

/** 필터 바 */
export function WSFilterBar({
  filters,
  filterValues,
  onFilterChange,
  searchValue,
  onSearchChange,
  searchPlaceholder = "검색어를 입력하세요",
  actions,
}) {
  return (
    <div className={s.filterBar}>
      {filters?.map((filter) => (
        <WSFilterDropdown
          key={filter.key}
          label={filter.label}
          value={filterValues?.[filter.key] || ""}
          options={filter.options}
          onChange={(value) => onFilterChange?.(filter.key, value)}
        />
      ))}
      <WSSearchInput
        value={searchValue}
        onChange={onSearchChange}
        placeholder={searchPlaceholder}
        className={s.filterBarSearch}
      />
      {actions?.map((action, idx) => (
        <button
          key={idx}
          onClick={action.onClick}
          className={`${s.filterBarAction} ${action.variant === "secondary" ? s.filterBarActionSecondary : s.filterBarActionPrimary}`}
          type="button"
          aria-label={action.label}
        >
          {action.icon}
          <span>{action.label}</span>
        </button>
      ))}
    </div>
  );
}

/** 테이블 헤더 */
export function WSTableHeader({ columns, gridTemplate }) {
  return (
    <div className={s.tableHeader} style={{ "--table-grid": gridTemplate }}>
      {columns.map((col, idx) => (
        <span key={idx}>{col}</span>
      ))}
    </div>
  );
}

/** 테이블 행 */
export function WSTableRow({ children, gridTemplate, onClick }) {
  return (
    <div
      onClick={onClick}
      className={`${s.tableRow} ${onClick ? s.tableRowClickable : ""}`}
      style={{ "--table-grid": gridTemplate }}
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={(e) => {
        if (!onClick) return;
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick(e);
        }
      }}
    >
      {children}
    </div>
  );
}

/** 페이지 헤더 */
export function WSPageHeader({ title, breadcrumb, action, backButton }) {
  return (
    <div className={s.pageHeader}>
      <div className={s.pageHeaderLeft}>
        {backButton && (
          <button
            onClick={backButton.onClick}
            className={s.backBtn}
            type="button"
            aria-label="이전 화면으로 이동"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
          </button>
        )}
        <div>
          <h1 className={s.pageTitle}>{title}</h1>
          {breadcrumb && (
            <p className={s.pageBreadcrumb}>
              {breadcrumb.map((crumb, idx) => (
                <span key={idx}>
                  {crumb}
                  {idx < breadcrumb.length - 1 && " / "}
                </span>
              ))}
            </p>
          )}
        </div>
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}
