// 일정 등록·수정·상세 조회 모달
import { useState, useEffect } from "react";
import {
  WSModal,
  WSModalActions,
  WSButton,
  WSFormField,
  WSInput,
  WSSelect,
  WSTextarea,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import s from "./ScheduleModal.module.css";

export const SCOPE_LABEL = {
  PERSONAL: "개인",
  DEPARTMENT: "부서",
  COMPANY: "전사",
};

// schedule이 있으면 그 값으로, 없으면 선택한 날짜 기준 기본값으로 폼을 채운다
function toForm(schedule, date) {
  if (!schedule) {
    return {
      title: "",
      scope: "PERSONAL",
      isAllDay: false,
      startDate: date,
      startTime: "09:00",
      endDate: date,
      endTime: "10:00",
      location: "",
      description: "",
    };
  }
  return {
    title: schedule.title,
    scope: schedule.scope,
    isAllDay: schedule.isAllDay,
    startDate: schedule.startAt.slice(0, 10),
    startTime: schedule.startAt.slice(11, 16),
    endDate: schedule.endAt.slice(0, 10),
    endTime: schedule.endAt.slice(11, 16),
    location: schedule.location ?? "",
    description: schedule.description ?? "",
  };
}

export default function ScheduleModal({
  isOpen,
  onClose,
  schedule,
  date,
  isAdmin,
  onSave,
  onDelete,
}) {
  const [form, setForm] = useState(toForm(schedule, date));
  const [error, setError] = useState("");
  const readOnly = !!schedule && !schedule.isEditable;

  useEffect(() => {
    if (!isOpen) return;
    setForm(toForm(schedule, date));
    setError("");
  }, [isOpen, schedule, date]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const startAt = `${form.startDate}T${form.isAllDay ? "00:00" : form.startTime}:00`;
  const endAt = `${form.endDate}T${form.isAllDay ? "23:59" : form.endTime}:00`;
  const isValid =
    form.title.trim().length > 0 && form.scope !== "" && startAt <= endAt;

  const scopeOptions = Object.entries(SCOPE_LABEL)
    .filter(([value]) => value !== "COMPANY" || isAdmin)
    .map(([value, label]) => ({ value, label }));

  const handleSave = async () => {
    const json = await onSave({
      id: schedule?.id,
      title: form.title.trim(),
      scope: form.scope,
      isAllDay: form.isAllDay,
      startAt,
      endAt,
      location: form.location || null,
      description: form.description || null,
    });
    if (!json || json.status >= 400) {
      setError(json?.message ?? "저장에 실패했습니다.");
    }
  };

  const handleDelete = async () => {
    if (!confirm("이 일정을 삭제하시겠습니까?")) return;
    const json = await onDelete(schedule.id);
    if (!json || json.status >= 400) {
      setError(json?.message ?? "삭제에 실패했습니다.");
    }
  };

  return (
    <WSModal
      isOpen={isOpen}
      onClose={onClose}
      title={schedule ? (readOnly ? "일정 상세" : "일정 수정") : "일정 등록"}
      subtitle={schedule ? `작성자 ${schedule.ownerName}` : undefined}
    >
      <div className={s.form}>
        <WSFormField label="제목" required>
          <WSInput
            value={form.title}
            maxLength={100}
            disabled={readOnly}
            onChange={set("title")}
          />
        </WSFormField>

        <WSFormField label="공개 범위" required>
          <WSSelect
            value={form.scope}
            options={scopeOptions}
            disabled={readOnly}
            onChange={set("scope")}
          />
        </WSFormField>

        <label className={s.checkRow}>
          <input
            type="checkbox"
            checked={form.isAllDay}
            disabled={readOnly}
            onChange={(e) => setForm({ ...form, isAllDay: e.target.checked })}
          />
          종일
        </label>

        <WSFormField
          label="기간"
          required
          error={startAt <= endAt ? null : "종료가 시작보다 빠를 수 없습니다."}
        >
          <div className={s.period}>
            <span className={s.periodLabel}>시작</span>
            <div className={s.periodInputs}>
              <WSInput
                type="date"
                value={form.startDate}
                disabled={readOnly}
                onChange={set("startDate")}
              />
              {!form.isAllDay && (
                <WSInput
                  type="time"
                  value={form.startTime}
                  disabled={readOnly}
                  onChange={set("startTime")}
                />
              )}
            </div>
            <span className={s.periodLabel}>종료</span>
            <div className={s.periodInputs}>
              <WSInput
                type="date"
                value={form.endDate}
                disabled={readOnly}
                onChange={set("endDate")}
              />
              {!form.isAllDay && (
                <WSInput
                  type="time"
                  value={form.endTime}
                  disabled={readOnly}
                  onChange={set("endTime")}
                />
              )}
            </div>
          </div>
        </WSFormField>

        <WSFormField label="장소">
          <WSInput
            value={form.location}
            maxLength={100}
            disabled={readOnly}
            onChange={set("location")}
          />
        </WSFormField>

        <WSFormField label="설명">
          <WSTextarea
            value={form.description}
            rows={3}
            disabled={readOnly}
            onChange={set("description")}
          />
        </WSFormField>

        {error && (
          <WSAlert type="error" message={error} onClose={() => setError("")} />
        )}
      </div>

      <WSModalActions>
        {schedule && !readOnly && (
          <WSButton label="삭제" variant="danger" onClick={handleDelete} />
        )}
        <WSButton
          label={readOnly ? "닫기" : "취소"}
          variant="secondary"
          onClick={onClose}
        />
        {!readOnly && (
          <WSButton label="저장" disabled={!isValid} onClick={handleSave} />
        )}
      </WSModalActions>
    </WSModal>
  );
}
