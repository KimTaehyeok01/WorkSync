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
  // 켜면 이메일로도 찾고, 추천 목록·칩에 이메일을 함께 보여준다 (메일 받는 사람·참조)
  byEmail = false,
  placeholder = byEmail ? "이메일 입력" : "이름으로 검색",
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

  // byEmail: 입력과 이메일이 완전히 같은(대소문자 무시) 직원인지
  const isExactEmail = (e) =>
    byEmail &&
    keyword.trim() !== "" &&
    (e.email ?? "").toLowerCase() === keyword.trim().toLowerCase();

  const suggestions = employees
    .filter((e) => !selectedIds.includes(e.id) && !excludeIds.includes(e.id))
    .filter((e) => e.status !== "INACTIVE")
    .filter((e) =>
      `${e.name} ${e.departmentName ?? ""}${byEmail ? ` ${e.email ?? ""}` : ""}`
        .toLowerCase()
        .includes(keyword.trim().toLowerCase()),
    )
    // 완전 일치를 맨 위로 — 8명으로 자르기 전에 올려야 잘려 나가지 않는다.
    // sort 는 안정 정렬이라 그 외 순서(와 byEmail 이 꺼졌을 때의 순서)는 그대로다
    .sort((a, b) => isExactEmail(b) - isExactEmail(a))
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
            <span className={s.chipLabel}>
              {byEmail && employee.email
                ? `${employee.name} <${employee.email}>`
                : employee.name}
            </span>
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
              // 이메일을 치고 Enter 하면 열려 있는 추천 목록의 맨 위를 고른다.
              // 한글 조합 중에는 고르지 않되, 폼 제출은 그때도 막는다
              if (byEmail && e.key === "Enter") {
                e.preventDefault();
                if (
                  !e.nativeEvent.isComposing &&
                  open &&
                  keyword.trim() !== "" &&
                  suggestions.length > 0
                ) {
                  add(suggestions[0].id);
                }
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
              {byEmail && <span className={s.optionEmail}>{employee.email}</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
