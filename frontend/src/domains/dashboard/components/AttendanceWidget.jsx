// 홈 화면의 근무체크 위젯 — 현재 시각과 오늘 출퇴근 상태, 출근·퇴근 버튼
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { LogIn, LogOut, ArrowRight } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import { toDateString } from "../../../utils/dateFormatter";
import {
  checkIn,
  checkOut,
  getMyAttendance,
} from "../../attendance/services/attendanceApi";
import s from "./HomeWidgets.module.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

function formatTime(dateTime) {
  return dateTime ? dateTime.slice(11, 16) : "--:--";
}

export default function AttendanceWidget() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [now, setNow] = useState(new Date());
  const [record, setRecord] = useState(null);
  const [message, setMessage] = useState("");

  // 현재 시각을 1초마다 갱신
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const loadToday = async () => {
    const date = new Date();
    const json = await getMyAttendance(
      accessToken,
      date.getFullYear(),
      date.getMonth() + 1,
    );
    const list = Array.isArray(json?.data) ? json.data : [];
    setRecord(list.find((r) => r.workDate === toDateString(date)) ?? null);
  };

  useEffect(() => {
    if (!accessToken) return;
    loadToday();
  }, [accessToken]);

  // 출근·퇴근 처리 — 실패하면 서버 메시지를 그대로 보여준다
  const handleCheck = async (apiCall) => {
    const json = await apiCall(accessToken);
    if (!json || json.status >= 400) {
      setMessage(json?.message ?? "처리에 실패했습니다.");
      return;
    }
    setMessage("");
    loadToday();
  };

  const status = !record ? "출근 전" : record.checkOutTime ? "퇴근" : "근무 중";
  const statusClass = !record
    ? s.statusBefore
    : record.checkOutTime
      ? s.statusDone
      : s.statusWorking;

  return (
    <WSCard
      title="근무체크"
      action={
        <WSButton
          label="근태"
          variant="secondary"
          size="sm"
          icon={<ArrowRight size={12} />}
          onClick={() => navigate("/attendance")}
        />
      }
    >
      <p className={s.clockDate}>
        {now.getMonth() + 1}월 {now.getDate()}일 ({WEEKDAYS[now.getDay()]})
      </p>
      <div className={s.clockRow}>
        <span className={s.clock}>{now.toLocaleTimeString("en-GB")}</span>
        <span className={`${s.statusBadge} ${statusClass}`}>{status}</span>
      </div>

      <div className={s.checkTimes}>
        <span>출근 {formatTime(record?.checkInTime)}</span>
        <span>퇴근 {formatTime(record?.checkOutTime)}</span>
      </div>

      <div className={s.checkActions}>
        <WSButton
          label="출근"
          size="sm"
          icon={<LogIn size={14} />}
          disabled={!!record}
          onClick={() => handleCheck(checkIn)}
        />
        <WSButton
          label="퇴근"
          size="sm"
          variant="secondary"
          icon={<LogOut size={14} />}
          disabled={!record || !!record.checkOutTime}
          onClick={() => handleCheck(checkOut)}
        />
      </div>
      {message && <p className={s.error}>{message}</p>}
    </WSCard>
  );
}
