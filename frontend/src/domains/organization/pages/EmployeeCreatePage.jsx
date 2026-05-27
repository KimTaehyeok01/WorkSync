import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
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
import s from "./EmployeeCreatePage.module.css";
import {
  getDepartments,
  getEmployee,
  createEmpoyee,
  editDepartments,
} from "../services/organizationListApi";

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

export default function EmployeeAdd() {
  const { accessToken } = useAuthContext();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    empNo: "",
    name: "",
    email: "",
    password: "",
    phone: "",
    jobGrade: "",
    role: "",
    departmentId: 0,
    hireDate: "",
  });
  const [departments, setDepartments] = useState([]);
  const [submitted, setSubmitted] = useState(false);

  // 부서
  useEffect(() => {
    if (!accessToken) return;
    getDepartments(accessToken).then((data) => {
      // console.log(data);
      setDepartments(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  const DEPT_OPTIONS = departments.map((dept) => ({
    key: dept.id,
    label: dept.name,
  }));

  const isValid = [
    form.empNo.trim().length > 0,
    form.name.trim().length > 0,
    form.email.trim().length > 0,
    form.password.trim().length > 0,
    form.role.trim().length > 0,
    form.jobGrade.trim().length > 0,
    form.departmentId > 0,
  ];

  async function handleSubmit() {
    // console.log("전송 데이터:", JSON.stringify(form));
    try {
      if (!isValid) {
        alert("필수 기재란을 채워주세요.");
        return;
      }
      await createEmpoyee(accessToken, form);
      setSubmitted(true);
      navigate("/organization");
    } catch (error) {
      console.log("저장 실패: " + error);
      alert("저장에 실패했습니다.");
    }
  }

  if (submitted) {
    return (
      <div className={s.successScreen}>
        <div className={s.successCard}>
          <div className={s.successIcon}>
            <CheckCircle size={40} className={s.successIconGlyph} />
          </div>
          <div>
            <p className={s.successTitle}>작업이 등록되었습니다</p>
            <p className={s.successDesc}>업무 보드로 이동합니다...</p>
          </div>
          <div className={s.successBadge}>업무 보드로 이동 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className={s.header}>
        <button onClick={() => navigate("/organization")} className={s.backBtn}>
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className={s.pageTitle}>직원 추가</h1>
          <p className={s.pageSub}>새로운 직원 정보를 입력하세요</p>
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
                      placeholder="사번 입력"
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
                      placeholder="이름 입력"
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
                      value={form.email}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, email: e.target.value }))
                      }
                      placeholder="example@gmail.com"
                      className={s.input}
                    />
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
                      placeholder="비밀번호 입력"
                      className={s.input}
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
              label="직원 등록"
              icon={<Send size={16} />}
              onClick={handleSubmit}
              disabled={!isValid}
              className={s.submitBtn}
            />
            <button
              onClick={() => navigate("/organization")}
              className={s.cancelBtn}
            >
              취소하고 돌아가기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
