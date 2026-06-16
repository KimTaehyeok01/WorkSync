import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import {
  getDepartments,
  createEmployee,
} from "../services/organizationListApi";
import { WSSuccessScreen } from "../../../components/common/LayoutComponents";
import useFileUpload from "../../../hooks/useFileUpload";
import EmployeeForm from "../components/EmployeeForm";
import { saveFile, deleteFile } from "../../file/services/fileApi";

export default function EmployeeAdd() {
  const { accessToken } = useAuthContext();
  const navigate = useNavigate();
  const [pwDisabled, setPwDisabled] = useState(false);
  const [departments, setDepartments] = useState([]);
  const [submitted, setSubmitted] = useState(false);
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

  // 부서 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getDepartments(accessToken).then((data) => {
      setDepartments(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  const DEPT_OPTIONS = departments.map((dept) => ({
    key: dept.id,
    label: dept.name,
  }));

  // 입력폼 유효성 체크
  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email);
  const isValidPhone = /^010-\d{4}-\d{4}$/.test(form.phone);
  const isValid = [
    form.empNo.trim().length > 0,
    form.name.trim().length > 0,
    form.email.trim().length > 0,
    isValidEmail,
    form.password.trim().length > 0,
    isValidPhone,
    form.role.trim().length > 0,
    form.jobGrade.trim().length > 0,
    form.departmentId > 0,
  ].every(Boolean);

  // 파일 선언
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "EMPLOYEE");

  // 폼 저장
  async function handleSubmit() {
    try {
      // 직원 저장
      const response = await createEmployee(accessToken, {
        ...form,
        profileImage: uploadedFile?.filePath ?? null,
      });
      const employeeId = response.data.id;

      for (const file of uploadedFile) {
        if (file?.filePath) {
          // 파일 저장
          await saveFile(accessToken, {
            ...file,
            refType: "EMPLOYEE",
            refId: employeeId,
          });
        }
      }

      setSubmitted(true);
      navigate("/organization");
    } catch (error) {
      if (error.response?.status === 409) {
        alert("이미 존재하는 이메일 또는 사번입니다.");
        return;
      } else {
        console.log("저장실패: " + error);
        return;
      }
    }

    // 파일 초기화
    clearFiles();
  }

  // 취소
  function handleRollBack() {
    navigate("/organization");
  }

  if (submitted) {
    return (
      <WSSuccessScreen
        title="작업이 등록되었습니다."
        description="업무 보드로 이동합니다."
        redirectLabel="업무 보드로 이동 중..."
        isVisible={isVisible}
      />
    );
  }

  return (
    <>
      <EmployeeForm
        isValid={isValid}
        form={form}
        setForm={setForm}
        pwDisabled={pwDisabled}
        DEPT_OPTIONS={DEPT_OPTIONS}
        onSubmit={handleSubmit}
        onCancel={handleRollBack}
        submitLabel="직원 등록"
        textBtnLabel="취소하고 돌아가기"
        pageTitle="직원 등록"
        files={files}
        isDragging={isDragging}
        setIsDragging={setIsDragging}
        addFiles={addFiles}
        removeFiles={removeFiles}
      />
    </>
  );
}
