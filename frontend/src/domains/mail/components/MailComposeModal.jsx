// 메일 작성 모달 — 새 메일·답장·전달 공용
import { useState, useEffect } from "react";
import { Send } from "lucide-react";
import {
  WSModal,
  WSModalActions,
  WSButton,
  WSFormField,
  WSInput,
  WSTextarea,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import EmployeePicker from "../../../components/common/EmployeePicker";
import { isOk } from "../../../api/apiRequest";

const EMPTY_DRAFT = { toIds: [], ccIds: [], subject: "", content: "" };

export default function MailComposeModal({
  isOpen,
  onClose,
  employees,
  draft,
  onSend,
}) {
  const [form, setForm] = useState(EMPTY_DRAFT);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);

  // 열릴 때마다 초안(답장·전달이면 미리 채운 값)으로 초기화
  useEffect(() => {
    if (!isOpen) return;
    setForm({ ...EMPTY_DRAFT, ...draft });
    setError("");
    setSending(false);
  }, [isOpen, draft]);

  const isValid =
    form.toIds.length > 0 &&
    form.subject.trim().length > 0 &&
    form.content.trim().length > 0;

  const handleSend = async () => {
    setSending(true);
    const json = await onSend({ ...form, subject: form.subject.trim() });
    if (!isOk(json)) {
      setError(json?.message ?? "메일을 보내지 못했습니다.");
      setSending(false);
    }
  };

  return (
    <WSModal isOpen={isOpen} onClose={onClose} title="메일 쓰기" size="lg">
      <WSFormField label="받는 사람" required>
        <EmployeePicker
          employees={employees}
          selectedIds={form.toIds}
          excludeIds={form.ccIds}
          onChange={(toIds) => setForm({ ...form, toIds })}
        />
      </WSFormField>
      <WSFormField label="참조">
        <EmployeePicker
          employees={employees}
          selectedIds={form.ccIds}
          excludeIds={form.toIds}
          onChange={(ccIds) => setForm({ ...form, ccIds })}
        />
      </WSFormField>
      <WSFormField label="제목" required>
        <WSInput
          value={form.subject}
          maxLength={200}
          onChange={(e) => setForm({ ...form, subject: e.target.value })}
        />
      </WSFormField>
      <WSFormField label="내용" required>
        <WSTextarea
          value={form.content}
          rows={10}
          onChange={(e) => setForm({ ...form, content: e.target.value })}
        />
      </WSFormField>

      {error && <WSAlert type="error" message={error} onClose={() => setError("")} />}

      <WSModalActions>
        <WSButton label="취소" variant="secondary" onClick={onClose} />
        <WSButton
          label="보내기"
          icon={<Send size={14} />}
          disabled={!isValid || sending}
          onClick={handleSend}
        />
      </WSModalActions>
    </WSModal>
  );
}
