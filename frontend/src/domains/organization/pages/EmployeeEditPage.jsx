import { useState, useEffect, use } from "react";
import { useNavigate, useParams } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import {
  getDepartments,
  getEmployeeById,
  editEmployee,
  resignEmployee,
} from "../services/organizationListApi";
import EmployeeForm from "../components/EmployeeForm";
import { WSSuccessScreen } from "../../../components/common/LayoutComponents";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";

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
    profileImage: "",
    role: "",
    departmentId: 0,
    hireDate: "",
  });

  // 입력폼 유효성 체크
  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email);
  const isValidPhone = /^010-\d{4}-\d{4}$/.test(form.phone);
  const isValid = [
    form.empNo?.trim().length > 0,
    form.name?.trim().length > 0,
    form.email?.trim().length > 0,
    isValidEmail,
    isValidPhone,
    form.role?.trim().length > 0,
    form.jobGrade?.trim().length > 0,
    form.departmentId > 0,
  ].every(Boolean);

  // 파일 선언
  const {
    files,
    setFiles,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "EMPLOYEE", id);

  // 파일 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !id) return;
    getFile(accessToken, "EMPLOYEE", id).then((data) => {
      const fileList = Array.isArray(data.data) ? data.data : [];
      console.log(fileList);

      setFiles(
        fileList.map((f) => ({
          file: {
            name: f.originalName,
            size: f.fileSize,
          },
          url: f.filePath,
          refType: f.refType,
          refId: f.refId,
        })),
      );
    });
  }, [accessToken, id]);

  // 직원 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !id) return;
    getEmployeeById(accessToken, id).then((data) => {
      setForm({
        empNo: data.data.empNo ?? "",
        name: data.data.name ?? "",
        email: data.data.email ?? "",
        password: data.data.password ?? "",
        phone: data.data.phone ?? "",
        jobGrade: data.data.jobGrade ?? "",
        profileImage: data.data.profileImage ?? null,
        role: data.data.role ?? "",
        departmentId: data.data.departmentId ?? 0,
        hireDate: data.data.hireDate ?? "",
      });
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

  // 파일 삭제
  const handleRemoveFile = async (index) => {
    await removeFiles(index);
    setForm((prev) => ({ ...prev, profileImage: null }));
  };

  // 저장
  async function handleSubmit() {
    try {
      // 직원 저장
      const response = await editEmployee(accessToken, id, {
        ...form,
        profileImage: uploadedFile?.filePath ?? null,
      });
      const employeeId = response.data.id;

      // 파일 경로가 있으면 파일 저장
      for (const file of uploadedFile) {
        if (file?.filePath && file?.isNew) {
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
      }

      // 파일 삭제
      removeFiles();
    }

    // 파일 초기화
    clearFiles();
  }

  // 퇴사 처리 (소프트 삭제 — 상태를 INACTIVE로 변경, 감사 로그에 기록됨)
  async function handleDelete() {
    const confirmText = confirm("해당 직원을 퇴사 처리하시겠습니까?");

    if (!confirmText) {
      return;
    }

    try {
      await deleteEmployee(accessToken, id);
      removeFiles();
      clearFiles();
      navigate("/organization");
    } catch (error) {
      console.log("퇴사 처리 실패: " + error);
      alert("퇴사 처리에 실패했습니다.");
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
        isValid={isValid}
        form={form}
        setForm={setForm}
        pwDisabled={pwDisabled}
        DEPT_OPTIONS={DEPT_OPTIONS}
        onSubmit={handleSubmit}
        onCancel={handleDelete}
        submitLabel="직원 수정"
        textBtnLabel="삭제하기"
        pageTitle="직원 수정"
        files={files}
        isDragging={isDragging}
        setIsDragging={setIsDragging}
        addFiles={addFiles}
        removeFiles={handleRemoveFile}
      />
    </>
  );
}
