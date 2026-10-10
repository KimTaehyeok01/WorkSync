// 예약 대상 자원을 등록·수정하는 관리자용 모달
import { useState, useEffect } from "react";
import {
  WSModal,
  WSModalActions,
  WSButton,
  WSFormField,
  WSInput,
  WSSelect,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import s from "./ResourceManageModal.module.css";

export const CATEGORY_LABEL = {
  MEETING_ROOM: "회의실",
  VEHICLE: "차량",
  EQUIPMENT: "비품",
};

const CATEGORY_OPTIONS = Object.entries(CATEGORY_LABEL).map(
  ([value, label]) => ({ value, label }),
);

const EMPTY_FORM = {
  id: null,
  name: "",
  category: "MEETING_ROOM",
  location: "",
  capacity: "",
  description: "",
  isActive: true,
};

export default function ResourceManageModal({
  isOpen,
  onClose,
  resources,
  onSave,
}) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setForm(EMPTY_FORM);
    setError("");
  }, [isOpen]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const selectResource = (resource) => {
    setError("");
    setForm({
      id: resource.id,
      name: resource.name,
      category: resource.category,
      location: resource.location ?? "",
      capacity: resource.capacity ?? "",
      description: resource.description ?? "",
      isActive: resource.isActive,
    });
  };

  const isValid = form.name.trim().length > 0 && form.category !== "";

  const handleSave = async () => {
    const json = await onSave({
      ...form,
      name: form.name.trim(),
      location: form.location || null,
      capacity: form.capacity === "" ? null : Number(form.capacity),
      description: form.description || null,
    });
    if (!json || json.status >= 400) {
      setError(json?.message ?? "저장에 실패했습니다.");
      return;
    }
    setForm(EMPTY_FORM);
    setError("");
  };

  return (
    <WSModal
      isOpen={isOpen}
      onClose={onClose}
      title="자원 관리"
      subtitle="회의실·차량·비품을 등록하거나 수정합니다."
      size="lg"
    >
      <div className={s.layout}>
        <div className={s.list}>
          <button
            type="button"
            className={`${s.listItem} ${form.id === null ? s.listItemActive : ""}`}
            onClick={() => {
              setForm(EMPTY_FORM);
              setError("");
            }}
          >
            + 새 자원
          </button>
          {resources.map((resource) => (
            <button
              key={resource.id}
              type="button"
              className={`${s.listItem} ${form.id === resource.id ? s.listItemActive : ""}`}
              onClick={() => selectResource(resource)}
            >
              <span>{resource.name}</span>
              <span className={s.listSub}>
                {CATEGORY_LABEL[resource.category]}
                {!resource.isActive && " · 사용 중지"}
              </span>
            </button>
          ))}
        </div>

        <div className={s.form}>
          <WSFormField label="자원명" required>
            <WSInput value={form.name} maxLength={50} onChange={set("name")} />
          </WSFormField>
          <WSFormField label="분류" required>
            <WSSelect
              value={form.category}
              options={CATEGORY_OPTIONS}
              onChange={set("category")}
            />
          </WSFormField>
          <WSFormField label="위치">
            <WSInput
              value={form.location}
              maxLength={100}
              onChange={set("location")}
            />
          </WSFormField>
          <WSFormField label="수용 인원">
            <WSInput
              type="number"
              value={form.capacity}
              onChange={set("capacity")}
            />
          </WSFormField>
          <WSFormField label="설명">
            <WSInput
              value={form.description}
              maxLength={255}
              onChange={set("description")}
            />
          </WSFormField>
          {form.id !== null && (
            <label className={s.checkRow}>
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(e) =>
                  setForm({ ...form, isActive: e.target.checked })
                }
              />
              예약 가능 (해제하면 신규 예약을 받지 않습니다)
            </label>
          )}
          {error && (
            <WSAlert
              type="error"
              message={error}
              onClose={() => setError("")}
            />
          )}
        </div>
      </div>

      <WSModalActions>
        <WSButton label="닫기" variant="secondary" onClick={onClose} />
        <WSButton
          label={form.id === null ? "등록" : "수정"}
          disabled={!isValid}
          onClick={handleSave}
        />
      </WSModalActions>
    </WSModal>
  );
}
