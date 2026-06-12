import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Zap, Eye, EyeOff, Mail, Lock, AlertCircle } from "lucide-react";
import styles from "./LoginPage.module.css";
import useAuthContext from "../../../store/AuthContext";

export default function Login() {
  const navigate = useNavigate();

  const [empNo, setEmpNo] = useState("");
  const [password, setPassword] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [focusEmp, setFocusEmp] = useState(false);
  const [focusPw, setFocusPw] = useState(false);
  const { login } = useAuthContext();

  async function handleLogin(e) {
    if (e) e.preventDefault();
    setError("");
    if (!empNo.trim()) { setError("사번을 입력해 주세요."); return; }
    if (!password.trim()) { setError("비밀번호를 입력해 주세요."); return; }
    setLoading(true);
    try {
      await login(empNo, password);
      navigate("/");
    } catch (err) {
      setError("사번 또는 비밀번호가 올바르지 않습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.bgImage} />
      <div className={styles.bgOverlay} />

      <div className={styles.topbar}>
        <div className={styles.topLogo}>
          <Zap size={20} color="#fff" fill="#fff" />
        </div>
        <div>
          <div className={styles.brandName}>WorkSync</div>
          <div className={styles.brandSub}>ENTERPRISE v1.0</div>
        </div>
      </div>

      <div className={styles.centerWrap}>
        <div className={styles.loginPanel}>
          <div className={styles.card}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}>로그인</h2>
              <p className={styles.cardSubtitle}>계정에 로그인하여 그룹웨어를 시작하세요.</p>
            </div>

            {error && (
              <div className={styles.error}>
                <AlertCircle size={14} className={styles.errorIcon} />
                <span className={styles.errorText}>{error}</span>
              </div>
            )}

            <form onSubmit={handleLogin} className={styles.form}>
              <div className={styles.field}>
                <label className={styles.label}>사번</label>
                <div className={`${styles.inputBox} ${focusEmp ? styles.inputBoxFocus : ""}`}>
                  <Mail size={15} className={`${styles.icon} ${focusEmp ? styles.iconFocus : ""}`} />
                  <input
                    type="text"
                    placeholder="사번을 입력하세요"
                    value={empNo}
                    onChange={(e) => { setEmpNo(e.target.value); setError(""); }}
                    onFocus={() => setFocusEmp(true)}
                    onBlur={() => setFocusEmp(false)}
                    autoComplete="username"
                  />
                </div>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>비밀번호</label>
                <div className={`${styles.inputBox} ${focusPw ? styles.inputBoxFocus : ""}`}>
                  <Lock size={15} className={`${styles.icon} ${focusPw ? styles.iconFocus : ""}`} />
                  <input
                    type={showPw ? "text" : "password"}
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    onChange={(e) => { setPassword(e.target.value); setError(""); }}
                    onFocus={() => setFocusPw(true)}
                    onBlur={() => setFocusPw(false)}
                    autoComplete="current-password"
                  />
                  <button type="button" onClick={() => setShowPw(!showPw)} className={styles.eyeBtn}>
                    {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>

              <button type="submit" disabled={loading} className={styles.submit}>
                {loading ? (
                  <div className={styles.spinnerRow}>
                    <div className={styles.spinner} />
                    로그인 중...
                  </div>
                ) : (
                  "로그인"
                )}
              </button>
            </form>
          </div>

          <div className={styles.footer}>
            <p>
              로그인에 문제가 발생하셨나요?{" "}
              <span className={styles.footerLink}>0120 내선번호로 문의주세요</span>
            </p>
            <p className={styles.copyright}>© 2026 WorkSync Enterprise · v1.0</p>
          </div>
        </div>
      </div>
    </div>
  );
}
