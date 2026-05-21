import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Upload, User } from "lucide-react";
import { WSAvatar, WSButton, WSCard } from "../../../components/common/CommonWidgets";
import s from "./EmployeeCreatePage.module.css";

export default function EmployeeAdd() {
  const navigate = useNavigate();
  const [employeeId, setEmployeeId] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [joinDate, setJoinDate] = useState("");
  const [department, setDepartment] = useState("");
  const [rank, setRank] = useState("");
  const [permission, setPermission] = useState("");
  const [profileImage, setProfileImage] = useState(null);

  function handleImageUpload(e) {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => setProfileImage(reader.result);
      reader.readAsDataURL(file);
    }
  }

  function handleSubmit() {
    navigate("/organization");
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
                  <label className={s.label}>사번</label>
                  <input
                    type="text"
                    value={employeeId}
                    onChange={(e) => setEmployeeId(e.target.value)}
                    placeholder="사번 입력"
                    className={s.input}
                  />
                </div>
                <div>
                  <label className={s.label}>이름</label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="이름 입력"
                    className={s.input}
                  />
                </div>
              </div>

              <div>
                <label className={s.label}>이메일</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="이메일 입력"
                  className={s.input}
                />
              </div>

              <div className={s.row2}>
                <div>
                  <label className={s.label}>연락처</label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="010-0000-0000"
                    className={s.input}
                  />
                </div>
                <div>
                  <label className={s.label}>입사일</label>
                  <input
                    type="date"
                    value={joinDate}
                    onChange={(e) => setJoinDate(e.target.value)}
                    className={s.input}
                  />
                </div>
              </div>
            </div>
          </WSCard>

          <WSCard title="조직 배정" className={s.card}>
            <div className={s.row3}>
              <div>
                <label className={s.label}>소속 부서</label>
                <select value={department} onChange={(e) => setDepartment(e.target.value)} className={s.select}>
                  <option value="">선택</option>
                  <option value="경영진">경영진</option>
                  <option value="제품팀">제품팀</option>
                  <option value="개발팀">개발팀</option>
                  <option value="디자인팀">디자인팀</option>
                </select>
              </div>
              <div>
                <label className={s.label}>직급</label>
                <select value={rank} onChange={(e) => setRank(e.target.value)} className={s.select}>
                  <option value="">선택</option>
                  <option value="사원">사원</option>
                  <option value="주임">주임</option>
                  <option value="대리">대리</option>
                  <option value="과장">과장</option>
                  <option value="차장">차장</option>
                  <option value="부장">부장</option>
                  <option value="이사">이사</option>
                </select>
              </div>
              <div>
                <label className={s.label}>권한</label>
                <select value={permission} onChange={(e) => setPermission(e.target.value)} className={s.select}>
                  <option value="">선택</option>
                  <option value="관리자">관리자</option>
                  <option value="매니저">매니저</option>
                  <option value="일반">일반</option>
                </select>
              </div>
            </div>
          </WSCard>
        </div>

        <div className={s.colSide}>
          <WSCard title="프로필 이미지" className={s.sideCard}>
            <div className={s.uploadWrap}>
              <div className={`${s.imagePreview} ${profileImage ? s.imagePreviewHasImage : ""}`}>
                {profileImage ? (
                  <WSAvatar src={profileImage} name={name || "Profile"} size={128} />
                ) : (
                  <User size={48} color="#D1D5DB" />
                )}
              </div>

              <label htmlFor="profile-upload" className={s.uploadBtn}>
                <Upload size={16} />
                이미지 업로드
              </label>
              <input
                id="profile-upload"
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                className={s.hiddenInput}
              />

              <p className={s.uploadHint}>JPG, PNG 파일 (최대 5MB)</p>
            </div>
          </WSCard>
        </div>
      </div>

      <div className={s.actions}>
        <WSButton
          label="취소하고 돌아가기"
          variant="secondary"
          onClick={() => navigate("/organization")}
          className={s.cancelBtn}
        />
        <WSButton
          label="직원 등록"
          onClick={handleSubmit}
          className={s.submitBtn}
        />
      </div>
    </div>
  );
}
