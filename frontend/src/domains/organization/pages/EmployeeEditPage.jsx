import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import {
  getDepartments,
  getEmployeeById,
  editEmployee,
  deleteEmployee,
} from "../services/organizationListApi";
import EmployeeForm from "../components/EmployeeForm";
import { WSSuccessScreen } from "../../../components/common/LayoutComponents";

export default function EmployeeEdit() {
  const { id } = useParams();
  const { accessToken } = useAuthContext();
  const navigate = useNavigate();
  const [pwDisabled, setPwDisabled] = useState(true);
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

  // 직원 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !id) return;
    console.log(id);
    getEmployeeById(accessToken, id).then((data) => {
      setForm(data.data);
    });
  }, [accessToken, id]);

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
    isValidPhone,
    form.role.trim().length > 0,
    form.jobGrade.trim().length > 0,
    form.departmentId > 0,
  ].every(Boolean);

  // 저장
  async function handleSubmit() {
    try {
      await editEmployee(accessToken, id, form);
    } catch (error) {
      console.log("저장 실패: " + error);
      alert("저장에 실패했습니다.");
    }
    setSubmitted(true);
    navigate("/organization");
  }

  // 삭제
  async function handleDelete() {
    const confirmText = confirm(
      "삭제 시 복구가 불가능합니다. 삭제하시겠습니까?",
    );

    if (!confirmText) {
      return;
    }

    try {
      await deleteEmployee(accessToken, id);
      navigate("/organization");
    } catch (error) {
      console.log("저장 실패: " + error);
      alert("저장에 실패했습니다.");
    }
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
        form={form}
        setForm={setForm}
        pwDisabled={pwDisabled}
        DEPT_OPTIONS={DEPT_OPTIONS}
        onSubmit={handleSubmit}
        onCancel={handleDelete}
        isValid={isValid}
        submitLabel="직원 수정"
        textBtnLabel="삭제하기"
        pageTitle="직원 수정"
      />
    </>
  );
}
