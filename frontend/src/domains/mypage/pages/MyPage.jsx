// 본인 프로필 조회·수정과 비밀번호 변경을 제공하는 마이페이지
import { useState, useEffect, useRef } from "react";
import { Camera, Trash2 } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSCard,
  WSAvatar,
  WSButton,
  WSFormField,
  WSInput,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import { uploadFile } from "../../file/services/fileApi";
import { isOk } from "../../../api/apiRequest";
import {
  JOB_GRADE_LABEL,
  NOTIFICATION_TYPE_LABEL,
} from "../../../constants/statusLabels";
import {
  getMyInfo,
  updateMyProfile,
  changeMyPassword,
  getNotificationSettings,
  updateNotificationSetting,
} from "../services/mypageApi";
import s from "./MyPage.module.css";

const EMPTY_PASSWORDS = { currentPassword: "", newPassword: "", confirm: "" };

export default function MyPage() {
  const { accessToken, refreshProfile } = useAuthContext();
  const fileInputRef = useRef(null);
  const [settings, setSettings] = useState([]);
  const [me, setMe] = useState(null);
  const [phone, setPhone] = useState("");
  const [profileImage, setProfileImage] = useState(null);
  const [profileAlert, setProfileAlert] = useState(null);
  const [passwords, setPasswords] = useState(EMPTY_PASSWORDS);
  const [passwordAlert, setPasswordAlert] = useState(null);

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => {
      const data = json?.data;
      if (!data) return;
      setMe(data);
      setPhone(data.phone ?? "");
      setProfileImage(data.profileImage ?? null);
    });
    getNotificationSettings(accessToken).then((json) =>
      setSettings(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken]);

  const isValidPhone = phone === "" || /^010-\d{4}-\d{4}$/.test(phone);
  const isValidPassword =
    passwords.currentPassword.length > 0 &&
    passwords.newPassword.length >= 8 &&
    passwords.newPassword === passwords.confirm;

  // 프로필 사진 선택 — 스토리지에 올린 뒤 경로만 보관하고, 저장 버튼을 눌러야 반영된다
  const handleImageChange = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    const fileData = new FormData();
    fileData.append("file", file);
    const json = await uploadFile(accessToken, fileData);
    if (!json?.data?.filePath) {
      setProfileAlert({ type: "error", message: "사진 업로드에 실패했습니다." });
      return;
    }
    setProfileImage(json.data.filePath);
    setProfileAlert(null);
  };

  const handleSaveProfile = async () => {
    const json = await updateMyProfile(accessToken, {
      phone: phone || null,
      profileImage,
    });
    if (!json || json.status >= 400) {
      setProfileAlert({
        type: "error",
        message: json?.message ?? "저장에 실패했습니다.",
      });
      return;
    }
    setMe(json.data);
    setProfileAlert({ type: "success", message: "프로필이 저장되었습니다." });
    // 상단 바의 이름·사진도 바로 바뀌도록 알린다
    refreshProfile();
  };

  // 알림 수신 설정 — 누르면 바로 저장하고, 실패하면 원래 값으로 되돌린다
  const handleToggleSetting = async (type, isEnabled) => {
    setSettings((prev) =>
      prev.map((setting) => (setting.type === type ? { ...setting, isEnabled } : setting)),
    );
    const json = await updateNotificationSetting(accessToken, type, isEnabled);
    if (!isOk(json)) {
      setSettings((prev) =>
        prev.map((setting) =>
          setting.type === type ? { ...setting, isEnabled: !isEnabled } : setting,
        ),
      );
    }
  };

  const handleChangePassword = async () => {
    const json = await changeMyPassword(accessToken, {
      currentPassword: passwords.currentPassword,
      newPassword: passwords.newPassword,
    });
    if (!json || json.status >= 400) {
      setPasswordAlert({
        type: "error",
        message: json?.message ?? "비밀번호 변경에 실패했습니다.",
      });
      return;
    }
    setPasswords(EMPTY_PASSWORDS);
    setPasswordAlert({ type: "success", message: "비밀번호가 변경되었습니다." });
  };

  if (!me) return <p className={s.loading}>불러오는 중...</p>;

  const infoRows = [
    ["사번", me.empNo],
    ["이메일", me.email],
    ["부서", me.departmentName ?? "미배정"],
    ["직급", JOB_GRADE_LABEL[me.jobGrade] ?? me.jobGrade],
    ["입사일", me.hireDate ?? "-"],
  ];

  return (
    <div className={s.root}>
      <WSCard title="내 정보">
        <div className={s.profile}>
          <div className={s.avatarCol}>
            <WSAvatar src={profileImage} name={me.name} size={88} />
            <div className={s.avatarActions}>
              <WSButton
                label="사진 변경"
                size="sm"
                variant="secondary"
                icon={<Camera size={14} />}
                onClick={() => fileInputRef.current?.click()}
              />
              {profileImage && (
                <WSButton
                  label="삭제"
                  size="sm"
                  variant="ghost"
                  icon={<Trash2 size={14} />}
                  onClick={() => setProfileImage(null)}
                />
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className={s.fileInput}
              onChange={handleImageChange}
            />
          </div>

          <div className={s.infoCol}>
            <p className={s.name}>{me.name}</p>
            <dl className={s.infoList}>
              {infoRows.map(([label, value]) => (
                <div key={label} className={s.infoRow}>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          </div>
        </div>

        <div className={s.form}>
          <WSFormField
            label="연락처"
            error={isValidPhone ? null : "010-0000-0000 형식으로 입력해 주세요."}
          >
            <WSInput
              placeholder="010-0000-0000"
              value={phone}
              maxLength={13}
              onChange={(e) => setPhone(e.target.value)}
            />
          </WSFormField>
          {profileAlert && (
            <WSAlert
              type={profileAlert.type}
              message={profileAlert.message}
              onClose={() => setProfileAlert(null)}
            />
          )}
          <div className={s.actions}>
            <WSButton
              label="프로필 저장"
              disabled={!isValidPhone}
              onClick={handleSaveProfile}
            />
          </div>
        </div>
      </WSCard>

      <WSCard title="알림 수신 설정" subtitle="끄면 해당 종류의 알림을 받지 않습니다.">
        <div className={s.settings}>
          {settings.map((setting) => (
            <label key={setting.type} className={s.settingRow}>
              <span>{NOTIFICATION_TYPE_LABEL[setting.type] ?? setting.type}</span>
              <input
                type="checkbox"
                className={s.switch}
                checked={setting.isEnabled}
                onChange={(e) => handleToggleSetting(setting.type, e.target.checked)}
              />
            </label>
          ))}
        </div>
      </WSCard>

      <WSCard title="비밀번호 변경">
        <div className={s.form}>
          <WSFormField label="현재 비밀번호" required>
            <WSInput
              type="password"
              value={passwords.currentPassword}
              onChange={(e) =>
                setPasswords({ ...passwords, currentPassword: e.target.value })
              }
            />
          </WSFormField>
          <WSFormField
            label="새 비밀번호"
            required
            helperText="8자 이상 입력해 주세요."
          >
            <WSInput
              type="password"
              value={passwords.newPassword}
              maxLength={72}
              onChange={(e) =>
                setPasswords({ ...passwords, newPassword: e.target.value })
              }
            />
          </WSFormField>
          <WSFormField
            label="새 비밀번호 확인"
            required
            error={
              passwords.confirm && passwords.newPassword !== passwords.confirm
                ? "새 비밀번호가 일치하지 않습니다."
                : null
            }
          >
            <WSInput
              type="password"
              value={passwords.confirm}
              maxLength={72}
              onChange={(e) =>
                setPasswords({ ...passwords, confirm: e.target.value })
              }
            />
          </WSFormField>
          {passwordAlert && (
            <WSAlert
              type={passwordAlert.type}
              message={passwordAlert.message}
              onClose={() => setPasswordAlert(null)}
            />
          )}
          <div className={s.actions}>
            <WSButton
              label="비밀번호 변경"
              disabled={!isValidPassword}
              onClick={handleChangePassword}
            />
          </div>
        </div>
      </WSCard>
    </div>
  );
}
