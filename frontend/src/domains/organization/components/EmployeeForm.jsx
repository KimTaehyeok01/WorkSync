import { useNavigate } from "react-router-dom";
import { ArrowLeft, Upload, User, Send, Paperclip } from "lucide-react";
import {
  WSAvatar,
  WSButton,
  WSCard,
} from "../../../components/common/CommonWidgets";
import {
  WSInput,
  WSFormField,
  WSSelect,
  WSTextarea,
  WSFileUploadZone,
  WSDatepicker,
  WSFileList,
} from "../../../components/common/FormComponents";
import s from "../pages/EmployeeCreatePage.module.css";

// 직급
const JOB_GRADE_OPTIONS = [
  { key: "STAFF", label: "사원" },
  { key: "SENIOR", label: "주임" },
  { key: "ASSISTANT_MANAGER", label: "대리" },
  { key: "MANAGER", label: "과장" },
  { key: "GENERAL_MANAGER", label: "부장" },
  { key: "DIRECTOR", label: "이사" },
  { key: "CEO", label: "대표" },
];

// 권한
const ROLE_OPTIONS = [
  { key: "USER", label: "사용자" },
  { key: "ADMIN", label: "관리자" },
];

export default function EmployeeForm({
  form,
  setForm,
  pwDisabled,
  DEPT_OPTIONS = [],
  onSubmit,
  onCancel,
  isValid,
  submitLabel,
  textBtnLabel,
  pageTitle,
}) {
  const navigate = useNavigate();

  // 입력폼 형식 오류 메시지
  const errors = {
    email:
      form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)
        ? "이메일 형식으로 입력해주세요."
        : "",
    phone:
      form.phone && !/^010-\d{4}-\d{4}$/.test(form.phone)
        ? "010-0000-0000 형식으로 입력해주세요."
        : "",
  };

  return (
    <div>
      <div className={s.header}>
        <div className={s.headerLeft}>
          <button
            onClick={() => navigate("/organization")}
            className={s.backBtn}
          >
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>{pageTitle}</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={s.colMain}>
          <WSCard title="개인 정보" className={s.card}>
            <div className={s.formGrid}>
              <div className={s.row2}>
                <div>
                  <WSFormField label="사번" required>
                    <WSInput
                      type="text"
                      value={form.empNo}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, empNo: e.target.value }))
                      }
                      className={s.input}
                    />
                  </WSFormField>
                </div>
                <div>
                  <WSFormField label="이름" required>
                    <WSInput
                      type="text"
                      value={form.name}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, name: e.target.value }))
                      }
                      className={s.input}
                    />
                  </WSFormField>
                </div>
              </div>

              <div className={s.row2}>
                <div>
                  <WSFormField label="이메일" required>
                    <WSInput
                      type="text"
                      required
                      pattern="010-d{4}-d{4}"
                      title="010-0000-0000 형식으로 입력해주세요."
                      value={form.email}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, email: e.target.value }))
                      }
                      placeholder="example@gmail.com"
                      className={s.input}
                    />
                    {errors.email && (
                      <p
                        style={{
                          color: "var(--ws-danger)",
                          fontSize: "12px",
                          margin: "6px",
                        }}
                      >
                        {errors.email}
                      </p>
                    )}
                  </WSFormField>
                </div>
                <div>
                  <WSFormField label="비밀번호" required>
                    <WSInput
                      type="text"
                      value={form.password}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, password: e.target.value }))
                      }
                      className={s.input}
                      disabled={pwDisabled}
                    />
                  </WSFormField>
                </div>
              </div>

              <div className={s.row2}>
                <div>
                  <WSFormField label="연락처" required>
                    <WSInput
                      type="text"
                      value={form.phone}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, phone: e.target.value }))
                      }
                      placeholder="000-0000-0000"
                      className={s.input}
                    />
                    {errors.phone && (
                      <p
                        style={{
                          color: "var(--ws-danger)",
                          fontSize: "12px",
                          margin: "6px",
                        }}
                      >
                        {errors.phone}
                      </p>
                    )}
                  </WSFormField>
                </div>
                <div>
                  <WSFormField label="입사일" required>
                    <WSDatepicker
                      value={form.hireDate}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, hireDate: e.target.value }))
                      }
                    />
                  </WSFormField>
                </div>
              </div>
            </div>
          </WSCard>

          <WSCard title="조직 배정" className={s.card}>
            <div className={s.row3}>
              <div>
                <WSFormField label="소속 부서" required>
                  <WSSelect
                    placeholder="부서 선택"
                    value={form.departmentId}
                    onChange={(e) =>
                      setForm((p) => ({
                        ...p,
                        departmentId: Number(e.target.value),
                      }))
                    }
                    options={DEPT_OPTIONS.map((m) => ({
                      value: m.key,
                      label: m.label,
                    }))}
                  />
                </WSFormField>
              </div>
              <div>
                <WSFormField label="직급" required>
                  <WSSelect
                    placeholder="직급 선택"
                    value={form.jobGrade}
                    onChange={(e) =>
                      setForm((p) => ({ ...p, jobGrade: e.target.value }))
                    }
                    options={JOB_GRADE_OPTIONS.map((m) => ({
                      value: m.key,
                      label: m.label,
                    }))}
                  />
                </WSFormField>
              </div>
              <div>
                <WSFormField label="권한" required>
                  <WSSelect
                    placeholder="권한 선택"
                    value={form.role}
                    onChange={(e) =>
                      setForm((p) => ({ ...p, role: e.target.value }))
                    }
                    options={ROLE_OPTIONS.map((m) => ({
                      value: m.key,
                      label: m.label,
                    }))}
                  />
                </WSFormField>
              </div>
            </div>
          </WSCard>
        </div>

        <div>
          <WSCard title="프로필 이미지" className={`${s.card} ${s.colSide}`}>
            {/* <WSFileUploadZone
              onFilesAdded={addFiles}
              isDragging={isDragging}
              onDragStateChange={setIsDragging}
              icon={<Paperclip size={28} />}
              accept=".jpg, ,png"
              label="파일을 드래그하거나 클릭하여 업로드"
              helperText="JPG, PNG - 최대 50MB"
            />

            <WSFileList
              files={files.map(({ file }) => file)}
              onRemove={removeFiles}
            /> */}
          </WSCard>

          <div className={s.actionsCol}>
            <WSButton
              label={submitLabel}
              icon={<Send size={16} />}
              onClick={onSubmit}
              disabled={!isValid}
              className={s.submitBtn}
            />
            <button onClick={onCancel} className={s.cancelBtn}>
              {textBtnLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
