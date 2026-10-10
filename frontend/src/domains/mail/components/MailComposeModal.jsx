// 메일 작성 모달 — 새 메일·답장·전달 공용
import { useState, useEffect } from "react";
import { Send, Paperclip } from "lucide-react";
import {
  WSModal,
  WSModalActions,
  WSButton,
  WSInput,
  WSTextarea,
  WSAlert,
  WSFileUploadZone,
  WSFileList,
} from "../../../components/common/CommonWidgets";
import EmployeePicker from "../../../components/common/EmployeePicker";
import useAuthContext from "../../../store/AuthContext";
import useFileUpload from "../../../hooks/useFileUpload";
import { isOk } from "../../../api/apiRequest";
import s from "./MailComposeModal.module.css";

const EMPTY_DRAFT = { toIds: [], ccIds: [], subject: "", content: "" };

// 화면의 파일 목록과 업로드 결과를 이름·크기로 하나씩 짝짓는다 (같은 이름·크기가 둘이면 하나씩 소비).
// 짝이 없는 항목은 null — 아직 올리는 중이거나 올리지 못한 파일이다
function pairUploads(files, uploads) {
  const used = new Set();
  return files.map(({ file }) => {
    const index = uploads.findIndex(
      (upload, i) =>
        !used.has(i) &&
        upload.originalName === file.name &&
        upload.fileSize === file.size,
    );
    if (index === -1) return null;
    used.add(index);
    return uploads[index];
  });
}

export default function MailComposeModal({
  isOpen,
  onClose,
  employees,
  draft,
  onSend,
}) {
  const { accessToken } = useAuthContext();
  const [form, setForm] = useState(EMPTY_DRAFT);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);

  // 첨부파일 — 올리면 바로 스토리지에 올라가고, 보낼 때 메일 id 로 저장된다
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFileRef,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "MAIL");

  // 열릴 때마다 초안(답장·전달이면 미리 채운 값)과 첨부 상태를 초기화
  useEffect(() => {
    if (!isOpen) return;
    setForm({ ...EMPTY_DRAFT, ...draft });
    setError("");
    setSending(false);
    setIsDragging(false);
    clearFiles();
  }, [isOpen, draft]);

  // 보낼 첨부는 화면의 files 기준으로 정한다. 훅의 uploadedFile state 는 removeFiles 가 엉뚱한 index 를
  // 지울 수 있어 쓰지 않고, 쌓이기만 하는 uploadedFileRef 에서 짝지어진 업로드 결과만 쓴다.
  // 훅은 ref 를 갱신한 직후 state 도 갱신해 리렌더를 일으키므로 렌더 중에 ref 를 읽어도 최신이다
  const paired = pairUploads(files, uploadedFileRef.current);
  const uploading = paired.some((upload) => upload === null);

  const isValid =
    form.toIds.length > 0 &&
    form.subject.trim().length > 0 &&
    form.content.trim().length > 0;

  const handleSend = async () => {
    // 보내는 순간의 ref 로 한 번 더 짝지어 확정한다 (버튼이 막혀 있어 짝 없는 항목은 거의 없다)
    const attachments = pairUploads(files, uploadedFileRef.current);
    if (attachments.some((upload) => upload === null)) return;

    setSending(true);
    const json = await onSend({
      ...form,
      subject: form.subject.trim(),
      files: attachments,
    });
    if (!isOk(json)) {
      setError(json?.message ?? "메일을 보내지 못했습니다.");
      setSending(false);
    }
  };

  return (
    <WSModal
      isOpen={isOpen}
      onClose={() => {
        // 보내는 중에 닫으면 발송 결과와 첨부 저장을 확인할 수 없으므로 막는다
        if (!sending) onClose();
      }}
      title="메일 쓰기"
      size="lg"
    >
      <div className={s.form}>
        <div className={s.row}>
          <span className={s.label}>
            받는 사람<span className={s.required}>*</span>
          </span>
          <EmployeePicker
            employees={employees}
            selectedIds={form.toIds}
            excludeIds={form.ccIds}
            byEmail
            onChange={(toIds) => setForm({ ...form, toIds })}
          />
        </div>
        <div className={s.row}>
          <span className={s.label}>참조</span>
          <EmployeePicker
            employees={employees}
            selectedIds={form.ccIds}
            excludeIds={form.toIds}
            byEmail
            onChange={(ccIds) => setForm({ ...form, ccIds })}
          />
        </div>
        <label className={s.row}>
          <span className={s.label}>
            제목<span className={s.required}>*</span>
          </span>
          <WSInput
            value={form.subject}
            maxLength={200}
            onChange={(e) => setForm({ ...form, subject: e.target.value })}
          />
        </label>
        <div className={s.row}>
          <span className={s.label}>첨부파일</span>
          <div className={s.attach}>
            <WSFileUploadZone
              onFilesAdded={addFiles}
              isDragging={isDragging}
              onDragStateChange={setIsDragging}
              icon={<Paperclip size={20} />}
              accept=".pdf,.docx,.xlsx,.pptx"
              label="파일을 드래그하거나 클릭하여 업로드"
              helperText="PDF, DOCX, XLSX, PPTX - 최대 50MB"
            />
            <WSFileList
              files={files.map(({ file }) => file)}
              onRemove={removeFiles}
            />
            {uploading && (
              <p className={s.uploading}>
                업로드 중이거나 실패한 파일이 있습니다. 실패한 파일은 X 로 지운 뒤
                다시 올려 주세요.
              </p>
            )}
          </div>
        </div>
        <WSTextarea
          value={form.content}
          rows={9}
          onChange={(e) => setForm({ ...form, content: e.target.value })}
        />
      </div>

      {error && <WSAlert type="error" message={error} onClose={() => setError("")} />}

      <WSModalActions>
        <WSButton
          label="취소"
          variant="secondary"
          disabled={sending}
          onClick={onClose}
        />
        <WSButton
          label="보내기"
          icon={<Send size={14} />}
          disabled={!isValid || sending || uploading}
          onClick={handleSend}
        />
      </WSModalActions>
    </WSModal>
  );
}
