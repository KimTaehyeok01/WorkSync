import React, { useState } from "react";
import { ChevronDown, X, Search, Info, CheckCircle, AlertCircle, Calendar } from "lucide-react";
import s from "./Widgets.module.css";

/** 아이콘 버튼 */
export function WSIconButton({ icon, onClick, variant = "default", className = "", ariaLabel }) {
  const cls =
    variant === "danger" ? `${s.iconBtn} ${s.iconBtnDanger}` :
    variant === "primary" ? `${s.iconBtn} ${s.iconBtnPrimary}` :
    s.iconBtn;
  return (
    <button onClick={onClick} className={`${cls} ${className}`} type="button" aria-label={ariaLabel}>
      {icon}
    </button>
  );
}

/** 구분선 */
export function WSDivider({ orientation = "horizontal" }) {
  return <div className={orientation === "vertical" ? s.dividerV : s.dividerH} />;
}

/** 로딩 스피너 */
export function WSSpinner({ size = 20, color = "#1A73E8" }) {
  return (
    <div
      className={s.spinner}
      style={{ "--spinner-size": `${size}px`, "--spinner-color": color }}
      role="status"
      aria-label="로딩 중"
    />
  );
}

/** 아이콘 원형 */
export function WSIconCircle({ icon, color, size = 48 }) {
  return (
    <div
      className={s.iconCircle}
      style={{ "--icon-circle-size": `${size}px`, "--icon-circle-bg": `${color}18`, "--icon-circle-color": color }}
    >
      {icon}
    </div>
  );
}

/** 폼 필드 */
export function WSFormField({ label, required, error, children, helperText, charCount, maxChars }) {
  return (
    <div className={s.field}>
      <label className={s.fieldLabel}>
        {label}
        {required && <span className={s.required}>*</span>}
      </label>
      {children}
      <div className={s.fieldFooter}>
        {helperText && <span className={s.fieldHelper}>{helperText}</span>}
        {charCount !== undefined && maxChars && (
          <span className={`${s.fieldCount} ${charCount > maxChars * 0.9 ? s.fieldCountWarn : ""}`}>
            {charCount}/{maxChars}
          </span>
        )}
      </div>
      {error && (
        <div className={s.fieldError}>
          <AlertCircle size={12} />
          {error}
        </div>
      )}
    </div>
  );
}

/** 텍스트 입력 */
export function WSInput({ type = "text", placeholder, value, onChange, disabled, maxLength, className = "" }) {
  return (
    <input
      type={type}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      disabled={disabled}
      maxLength={maxLength}
      className={`${s.inputBase} ${className}`}
    />
  );
}

/** 텍스트 영역 */
export function WSTextarea({ placeholder, value, onChange, disabled, maxLength, rows = 5, className = "" }) {
  return (
    <textarea
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      disabled={disabled}
      maxLength={maxLength}
      rows={rows}
      className={`${s.textareaBase} ${className}`}
    />
  );
}

/** 셀렉트 */
export function WSSelect({ value, onChange, options, placeholder = "선택...", disabled, className = "" }) {
  return (
    <div className={s.selectWrap}>
      <select
        value={value}
        onChange={onChange}
        disabled={disabled}
        className={`${s.selectBase} ${className}`}
      >
        <option value="">{placeholder}</option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      <ChevronDown size={14} className={s.selectChevron} />
    </div>
  );
}

/** 날짜 선택박스 */
export function WSDatepicker({
  value,
  onChange,
  placeholder = "날짜 선택",
  disabled,
  min,
  max,
  className = "",
}) {
  return (
    <div className={s.datePickerWrap}>
      <input
        type="date"
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        min={min}
        max={max}
        className={`${s.dateInputBase} ${className}`}
        aria-label={placeholder}
      />
      <Calendar size={14} className={s.datePickerIcon} />
    </div>
  );
}

/** 기간 선택박스 */
export function WSCalendarpicker({
  startValue,
  endValue,
  onStartChange,
  onEndChange,
  startPlaceholder = "시작일",
  endPlaceholder = "종료일",
  disabled,
  min,
  max,
  className = "",
}) {
  return (
    <div className={`${s.calendarPickerWrap} ${className}`}>
      <div className={s.calendarPickerField}>
        <input
          type="date"
          value={startValue}
          onChange={onStartChange}
          placeholder={startPlaceholder}
          disabled={disabled}
          min={min}
          max={endValue || max}
          className={s.dateInputBase}
          aria-label={startPlaceholder}
        />
        <Calendar size={14} className={s.datePickerIcon} />
      </div>
      <span className={s.calendarPickerDivider}>~</span>
      <div className={s.calendarPickerField}>
        <input
          type="date"
          value={endValue}
          onChange={onEndChange}
          placeholder={endPlaceholder}
          disabled={disabled}
          min={startValue || min}
          max={max}
          className={s.dateInputBase}
          aria-label={endPlaceholder}
        />
        <Calendar size={14} className={s.datePickerIcon} />
      </div>
    </div>
  );
}

/** 검색 입력 */
export function WSSearchInput({ value, onChange, placeholder = "검색...", className = "" }) {
  return (
    <div className={`${s.searchInputCompact} ${className}`}>
      <Search size={16} className={s.searchInputCompactIcon} />
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={s.searchInputCompactField}
      />
      {value && (
        <button onClick={() => onChange("")} className={s.searchInputCompactClear} type="button" aria-label="검색어 지우기">
          <X size={13} />
        </button>
      )}
    </div>
  );
}

/** 필터 드롭다운 */
export function WSFilterDropdown({ label, value, options, onChange, className = "" }) {
  const [open, setOpen] = useState(false);
  const selected = options.find((o) => o.key === value);

  return (
    <div className={`${s.filterDD} ${className}`}>
      <button
        onClick={() => setOpen(!open)}
        className={s.filterDDBtn}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span>{selected?.label || label}</span>
        <ChevronDown size={14} className={s.filterDDChevron} />
      </button>
      {open && (
        <div className={s.filterDDMenu} role="listbox" aria-label={`${label} 필터`}>
          {options.map((item) => (
            <button
              key={item.key}
              onClick={() => { onChange(item.key); setOpen(false); }}
              className={s.filterDDItem}
              type="button"
              role="option"
              aria-selected={item.key === value}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/** 알림 박스 */
export function WSAlert({ type = "info", message, onClose }) {
  const variantClass =
    type === "success" ? s.alertSuccess :
    type === "warning" ? s.alertWarning :
    type === "error" ? s.alertError :
    s.alertInfo;

  const icon =
    type === "success" ? <CheckCircle size={15} className={s.alertIconSuccess} /> :
    type === "warning" ? <AlertCircle size={15} className={s.alertIconWarning} /> :
    type === "error" ? <AlertCircle size={15} className={s.alertIconError} /> :
    <Info size={15} className={s.alertIconInfo} />;

  return (
    <div className={`${s.alert} ${variantClass}`}>
      {icon}
      <span className={s.alertText}>{message}</span>
      {onClose && (
        <button onClick={onClose} className={s.alertClose} type="button" aria-label="알림 닫기">
          <X size={14} />
        </button>
      )}
    </div>
  );
}

/** 파일 업로드 존 */
export function WSFileUploadZone({
  onFilesAdded, accept = "*", isDragging,
  onDragStateChange, icon, label, helperText,
}) {
  const inputRef = React.useRef(null);

  const handleDrop = (e) => {
    e.preventDefault();
    onDragStateChange(false);
    const files = Array.from(e.dataTransfer.files);
    onFilesAdded(files);
  };

  return (
    <>
      <div
        onDragOver={(e) => { e.preventDefault(); onDragStateChange(true); }}
        onDragLeave={() => onDragStateChange(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        className={`${s.uploadZone} ${isDragging ? s.uploadZoneActive : ""}`}
        role="button"
        tabIndex={0}
        aria-label={label}
      >
        <div className={`${s.uploadZoneIcon} ${isDragging ? s.uploadZoneIconActive : ""}`}>{icon}</div>
        <p className={s.uploadZoneLabel}>{label}</p>
        {helperText && <p className={s.uploadZoneHelper}>{helperText}</p>}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        multiple
        className={s.uploadInput}
        onChange={(e) => {
          if (e.target.files) onFilesAdded(Array.from(e.target.files));
        }}
      />
    </>
  );
}
