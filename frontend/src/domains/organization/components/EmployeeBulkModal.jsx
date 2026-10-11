// 직원 CSV 일괄 등록 모달 — 양식 내려받기, 파일 선택, 행별 결과 표시
import { useState, useEffect, useRef } from "react";
import { Download, Upload } from "lucide-react";
import {
  WSModal,
  WSModalActions,
  WSButton,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { downloadCsv } from "../../../utils/dateFormatter";
import { bulkCreateEmployees } from "../services/employeeAdminApi";
import s from "./EmployeeBulkModal.module.css";

// CSV 열 순서 — 양식 머리글과 서버 필드를 한 곳에서 맞춘다
const COLUMNS = [
  { key: "empNo", label: "사번" },
  { key: "name", label: "이름" },
  { key: "email", label: "이메일" },
  { key: "password", label: "초기비밀번호" },
  { key: "phone", label: "연락처" },
  { key: "jobGrade", label: "직급" },
  { key: "departmentName", label: "부서명" },
  { key: "hireDate", label: "입사일" },
];

const SAMPLE_ROW = [
  "EMP101",
  "홍길동",
  "hong@worksync.com",
  "초기비번1234",
  "010-1234-5678",
  "STAFF",
  "개발팀",
  "2026-01-02",
];

const MAX_ROWS = 500;

// 따옴표로 감싼 값(쉼표·줄바꿈 포함)까지 처리하는 CSV 파서
function parseCsv(text) {
  const rows = [];
  let row = [];
  let value = "";
  let quoted = false;

  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    if (quoted) {
      if (char === '"' && text[i + 1] === '"') {
        value += '"';
        i++;
      } else if (char === '"') {
        quoted = false;
      } else {
        value += char;
      }
    } else if (char === '"') {
      quoted = true;
    } else if (char === ",") {
      row.push(value);
      value = "";
    } else if (char === "\n" || char === "\r") {
      if (char === "\r" && text[i + 1] === "\n") i++;
      row.push(value);
      rows.push(row);
      row = [];
      value = "";
    } else {
      value += char;
    }
  }
  if (value !== "" || row.length > 0) {
    row.push(value);
    rows.push(row);
  }
  return rows.filter((cells) => cells.some((cell) => cell.trim() !== ""));
}

export default function EmployeeBulkModal({ isOpen, onClose, accessToken, onDone }) {
  const inputRef = useRef(null);
  const [rows, setRows] = useState([]);
  const [fileName, setFileName] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setRows([]);
    setFileName("");
    setResult(null);
    setError("");
    setSubmitting(false);
  }, [isOpen]);

  const handleFile = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    // 엑셀이 붙이는 BOM 제거 후 첫 줄(머리글)은 건너뛴다
    const text = (await file.text()).replace(/^﻿/, "");
    const parsed = parseCsv(text).slice(1);
    setResult(null);
    setFileName(file.name);

    if (parsed.length === 0) {
      setRows([]);
      setError("등록할 행이 없습니다. 머리글 아래에 직원 정보를 입력해 주세요.");
      return;
    }
    if (parsed.length > MAX_ROWS) {
      setRows([]);
      setError(`한 번에 ${MAX_ROWS}행까지만 등록할 수 있습니다.`);
      return;
    }
    setError("");
    setRows(
      parsed.map((cells) =>
        Object.fromEntries(COLUMNS.map((column, idx) => [column.key, (cells[idx] ?? "").trim()])),
      ),
    );
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    const json = await bulkCreateEmployees(accessToken, rows);
    setSubmitting(false);
    if (!isOk(json)) {
      setError(json?.message ?? "일괄 등록에 실패했습니다.");
      return;
    }
    setResult(json.data);
    setRows([]);
    if (json.data.successCount > 0) onDone();
  };

  const failed = result?.results.filter((r) => !r.isSuccess) ?? [];

  return (
    <WSModal
      isOpen={isOpen}
      onClose={onClose}
      title="직원 일괄 등록"
      subtitle="CSV 파일로 여러 명을 한 번에 등록합니다."
      size="lg"
    >
      <div className={s.steps}>
        <div className={s.step}>
          <span className={s.stepNo}>1</span>
          <div className={s.stepBody}>
            <p className={s.stepTitle}>양식 내려받기</p>
            <p className={s.stepDesc}>
              직급은 STAFF, SENIOR, ASSISTANT_MANAGER, MANAGER, GENERAL_MANAGER, DIRECTOR, VICE_PRESIDENT, PRESIDENT, CEO 중
              하나, 입사일은 2026-01-02 형식으로 적습니다. 부서명은 등록된 부서와 같아야 합니다.
            </p>
          </div>
          <WSButton
            label="양식"
            size="sm"
            variant="secondary"
            icon={<Download size={13} />}
            onClick={() =>
              downloadCsv("직원_일괄등록_양식.csv", [COLUMNS.map((c) => c.label), SAMPLE_ROW])
            }
          />
        </div>

        <div className={s.step}>
          <span className={s.stepNo}>2</span>
          <div className={s.stepBody}>
            <p className={s.stepTitle}>작성한 파일 선택</p>
            <p className={s.stepDesc}>
              {fileName ? `${fileName} · ${rows.length}행` : "CSV(쉼표로 구분) 형식으로 저장한 파일을 선택합니다."}
            </p>
          </div>
          <WSButton
            label="파일 선택"
            size="sm"
            variant="secondary"
            icon={<Upload size={13} />}
            onClick={() => inputRef.current?.click()}
          />
          <input
            ref={inputRef}
            type="file"
            accept=".csv,text/csv"
            className={s.fileInput}
            onChange={handleFile}
          />
        </div>
      </div>

      {error && <WSAlert type="error" message={error} onClose={() => setError("")} />}

      {result && (
        <div className={s.result}>
          <WSAlert
            type={result.failCount === 0 ? "success" : "warning"}
            message={`${result.successCount}명 등록, ${result.failCount}명 실패`}
          />
          {failed.length > 0 && (
            <div className={s.failList}>
              {failed.map((r) => (
                <div key={r.rowNumber} className={s.failRow}>
                  <span className={s.failNo}>{r.rowNumber}행</span>
                  <span className={s.failEmp}>{r.empNo || "-"}</span>
                  <span className={s.failMsg}>{r.message}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <WSModalActions>
        <WSButton label="닫기" variant="secondary" onClick={onClose} />
        <WSButton
          label={`${rows.length}명 등록`}
          disabled={rows.length === 0 || submitting}
          onClick={handleSubmit}
        />
      </WSModalActions>
    </WSModal>
  );
}
