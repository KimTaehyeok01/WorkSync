// 직원을 이름으로 찾아 여러 명 고르는 입력 — 메일 받는 사람·참조, 일정 참석자에서 쓴다
import { useState, useRef, useEffect } from "react";
import { X } from "lucide-react";
import { WSAvatar } from "./CommonWidgets";
import s from "./EmployeePicker.module.css";

const MAX_SUGGESTIONS = 8;

export default function EmployeePicker({
  employees,
  selectedIds,
  onChange,
  placeholder = "이름으로 검색",
  excludeIds = [],
  disabled = false,
}) {
  const [keyword, setKeyword] = useState("");
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  // 바깥을 누르면 목록을 닫는다
  useEffect(() => {
    const handleOutside = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", handleOutside);
    return () => document.removeEventListener("mousedown", handleOutside);
  }, []);

  const selected = selectedIds
    .map((id) => employees.find((e) => e.id === id))
    .filter(Boolean);

  const suggestions = employees
    .filter((e) => !selectedIds.includes(e.id) && !excludeIds.includes(e.id))
    .filter((e) => e.status !== "INACTIVE")
    .filter((e) =>
      `${e.name} ${e.departmentName ?? ""}`
        .toLowerCase()
        .includes(keyword.trim().toLowerCase()),
    )
    .slice(0, MAX_SUGGESTIONS);

  const add = (id) => {
    onChange([...selectedIds, id]);
    setKeyword("");
  };

  const remove = (id) => onChange(selectedIds.filter((v) => v !== id));

  return (
    <div className={s.root} ref={rootRef}>
      <div className={`${s.box} ${disabled ? s.boxDisabled : ""}`}>
        {selected.map((employee) => (
          <span key={employee.id} className={s.chip}>
            {employee.name}
            {!disabled && (
              <button
                type="button"
                className={s.chipRemove}
                onClick={() => remove(employee.id)}
                aria-label={`${employee.name} 제외`}
              >
                <X size={12} />
              </button>
            )}
          </span>
        ))}
        {!disabled && (
          <input
            className={s.input}
            value={keyword}
            placeholder={selected.length === 0 ? placeholder : ""}
            onChange={(e) => {
              setKeyword(e.target.value);
              setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={(e) => {
              // 입력이 비어 있을 때 Backspace 로 마지막 사람 제외
              if (e.key === "Backspace" && keyword === "" && selectedIds.length > 0) {
                remove(selectedIds[selectedIds.length - 1]);
              }
            }}
          />
        )}
      </div>

      {open && !disabled && suggestions.length > 0 && (
        <div className={s.menu} role="listbox">
          {suggestions.map((employee) => (
            <button
              key={employee.id}
              type="button"
              className={s.option}
              role="option"
              aria-selected="false"
              onClick={() => add(employee.id)}
            >
              <WSAvatar src={employee.profileImage} name={employee.name} size={26} />
              <span className={s.optionName}>{employee.name}</span>
              <span className={s.optionSub}>{employee.departmentName ?? "미배정"}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
