// 결재 양식 관리 페이지 (관리자 전용) - 커스텀 결재 양식 등록/삭제
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Trash2, AlertTriangle } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSPageHeader,
  WSCard,
  WSButton,
  WSModal,
  WSModalActions,
  WSFormField,
  WSInput,
  WSEmptyState,
  WSTableHeader,
  WSTableRow,
} from "../../../components/common/CommonWidgets";
import { getMyInfo, getForms, createForm, deleteForm } from "../services/approvalApi";
import s from "./ApprovalFormManagePage.module.css";

const TH_COL = ["양식명", "유형", "필드 수", "생성일", "관리"];
const GRID_TEMPLATE = "2fr 1fr 1fr 1fr 1fr";
const FIELD_TYPES = ["TEXT", "TEXTAREA", "DATE", "NUMBER", "SELECT"];

// 라벨을 영문 소문자 + 언더스코어 key로 단순 변환 (한글 등 비영문은 제거됨 - 수동 수정 필요)
function slugify(label) {
  return label
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

// formSchema(JSON 문자열)에서 필드 개수 계산 (파싱 실패/빈 값은 0)
function fieldCount(formSchema) {
  try {
    return JSON.parse(formSchema || "{}").fields?.length ?? 0;
  } catch {
    return 0;
  }
}

function emptyField() {
  return {
    id: Date.now() + Math.random(),
    label: "",
    key: "",
    keyTouched: false,
    type: "TEXT",
    required: false,
    options: "",
  };
}

export default function ApprovalFormManagePage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [myInfo, setMyInfo] = useState(null);
  const [isLoadingRole, setIsLoadingRole] = useState(true);
  const [forms, setForms] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [formName, setFormName] = useState("");
  const [fields, setFields] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 내 정보(role) 확인
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      setMyInfo(data);
      setIsLoadingRole(false);
    });
  }, [accessToken]);

  // 양식 목록 조회
  const loadForms = () => {
    if (!accessToken) return;
    getForms(accessToken).then((data) => setForms(Array.isArray(data) ? data : []));
  };

  useEffect(() => {
    loadForms();
  }, [accessToken]);

  const resetModal = () => {
    setFormName("");
    setFields([]);
  };

  const openModal = () => {
    resetModal();
    setModalOpen(true);
  };

  const addField = () => setFields((prev) => [...prev, emptyField()]);

  const delField = (id) =>
    setFields((prev) => prev.filter((f) => f.id !== id));

  const updateField = (id, prop, value) =>
    setFields((prev) =>
      prev.map((f) => (f.id === id ? { ...f, [prop]: value } : f)),
    );

  // 라벨 입력 시 key를 자동 생성한다 (key를 직접 수정한 적이 있으면 자동 생성하지 않음)
  const handleLabelChange = (id, value) =>
    setFields((prev) =>
      prev.map((f) =>
        f.id === id
          ? { ...f, label: value, key: f.keyTouched ? f.key : slugify(value) }
          : f,
      ),
    );

  const handleKeyChange = (id, value) =>
    setFields((prev) =>
      prev.map((f) => (f.id === id ? { ...f, key: value, keyTouched: true } : f)),
    );

  const handleCreate = async () => {
    if (!formName.trim()) {
      alert("양식명을 입력하세요.");
      return;
    }
    if (fields.length === 0) {
      alert("필드를 1개 이상 추가하세요.");
      return;
    }
    const keys = fields.map((f) => f.key.trim());
    if (new Set(keys).size !== keys.length) {
      alert("필드 key가 중복되었습니다.");
      return;
    }

    const body = {
      formName: formName.trim(),
      fields: fields.map((f) => ({
        key: f.key.trim(),
        label: f.label.trim(),
        type: f.type,
        required: f.required,
        ...(f.type === "SELECT"
          ? {
              options: f.options
                .split(",")
                .map((o) => o.trim())
                .filter(Boolean),
            }
          : {}),
      })),
    };

    setIsSubmitting(true);
    try {
      await createForm(accessToken, body);
      setModalOpen(false);
      resetModal();
      loadForms();
    } catch (error) {
      alert(error.message || "양식 등록에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (form) => {
    if (!confirm(`"${form.formName}" 양식을 삭제하시겠습니까?`)) return;
    try {
      await deleteForm(accessToken, form.id);
      setForms((prev) => prev.filter((f) => f.id !== form.id));
    } catch (error) {
      alert(error.message || "양식 삭제에 실패했습니다.");
    }
  };

  if (isLoadingRole) {
    return <div>로딩 중...</div>;
  }

  if (myInfo?.role !== "ADMIN") {
    return (
      <div className={s.deniedWrap}>
        <WSEmptyState
          icon={<AlertTriangle size={32} />}
          title="접근 권한이 없습니다"
          description="결재 양식 관리는 관리자만 접근할 수 있습니다."
        />
      </div>
    );
  }

  return (
    <div>
      <WSPageHeader
        title="결재 양식 관리"
        backButton={{ onClick: () => navigate(-1) }}
        action={
          <WSButton
            label="새 양식 추가"
            icon={<Plus size={16} />}
            variant="primary"
            onClick={openModal}
          />
        }
      />

      <div className={s.table}>
        <WSTableHeader columns={TH_COL} gridTemplate={GRID_TEMPLATE} />

        {forms.length === 0 ? (
          <WSEmptyState title="등록된 양식이 없습니다" />
        ) : (
          forms.map((form) => (
            <WSTableRow key={form.id} gridTemplate={GRID_TEMPLATE}>
              <p className={s.cell}>{form.formName}</p>
              <span className={s.typeBadge}>{form.formType}</span>
              <p className={s.cell}>{fieldCount(form.formSchema)}개</p>
              <p className={s.cell}>
                {form.createdAt
                  ? new Date(form.createdAt).toLocaleDateString("ko-KR")
                  : "-"}
              </p>
              <div>
                {form.formType === "CUSTOM" && (
                  <button
                    onClick={() => handleDelete(form)}
                    className={s.deleteBtn}
                    type="button"
                    aria-label={`${form.formName} 삭제`}
                  >
                    <Trash2 size={16} />
                  </button>
                )}
              </div>
            </WSTableRow>
          ))
        )}
      </div>

      <WSModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="새 양식 추가"
        size="lg"
      >
        <div className={s.formNameField}>
          <WSFormField label="양식명" required>
            <WSInput
              type="text"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="양식명을 입력하세요"
            />
          </WSFormField>
        </div>

        <div className={s.fieldsSection}>
          <div className={s.fieldsHeader}>
            <span className={s.fieldsTitle}>필드 목록</span>
            <button
              type="button"
              onClick={addField}
              className={s.addFieldBtn}
            >
              <Plus size={14} />
              필드 추가
            </button>
          </div>

          {fields.length === 0 && (
            <p className={s.fieldsEmpty}>필드를 추가하세요.</p>
          )}

          {fields.map((field) => (
            <div key={field.id} className={s.fieldRow}>
              <div className={s.fieldRowMain}>
                <input
                  type="text"
                  placeholder="라벨 (예: 사유)"
                  value={field.label}
                  onChange={(e) => handleLabelChange(field.id, e.target.value)}
                  className={s.fieldInput}
                />
                <input
                  type="text"
                  placeholder="key (예: reason)"
                  value={field.key}
                  onChange={(e) => handleKeyChange(field.id, e.target.value)}
                  className={s.fieldInput}
                />
                <select
                  value={field.type}
                  onChange={(e) => updateField(field.id, "type", e.target.value)}
                  className={s.fieldSelect}
                >
                  {FIELD_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
                <label className={s.fieldCheckboxLabel}>
                  <input
                    type="checkbox"
                    checked={field.required}
                    onChange={(e) =>
                      updateField(field.id, "required", e.target.checked)
                    }
                  />
                  필수
                </label>
                <button
                  type="button"
                  onClick={() => delField(field.id)}
                  className={s.delFieldBtn}
                  aria-label="필드 삭제"
                >
                  <Trash2 size={14} />
                </button>
              </div>
              {field.type === "SELECT" && (
                <input
                  type="text"
                  placeholder="선택지 (콤마로 구분, 예: 옵션1,옵션2)"
                  value={field.options}
                  onChange={(e) =>
                    updateField(field.id, "options", e.target.value)
                  }
                  className={s.fieldOptionsInput}
                />
              )}
            </div>
          ))}
        </div>

        <WSModalActions>
          <WSButton
            label="취소"
            variant="secondary"
            onClick={() => setModalOpen(false)}
          />
          <WSButton
            label={isSubmitting ? "등록 중..." : "등록"}
            variant="primary"
            disabled={isSubmitting}
            onClick={handleCreate}
          />
        </WSModalActions>
      </WSModal>
    </div>
  );
}
