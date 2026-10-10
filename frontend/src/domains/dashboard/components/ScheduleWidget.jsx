// 홈 화면의 미니 달력 위젯 — 일정이 있는 날을 표시하고 선택한 날의 일정을 보여준다
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight, ArrowRight } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import { toDateString, buildMonthGrid } from "../../../utils/dateFormatter";
import { getSchedules } from "../../calendar/services/calendarApi";
import s from "./HomeWidgets.module.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

export default function ScheduleWidget() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const now = new Date();
  const today = toDateString(now);

  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selected, setSelected] = useState(today);
  const [schedules, setSchedules] = useState([]);

  const days = buildMonthGrid(year, month);
  const from = days[0];
  const to = days[days.length - 1];
  const monthPrefix = `${year}-${String(month).padStart(2, "0")}`;

  useEffect(() => {
    if (!accessToken) return;
    getSchedules(accessToken, from, to).then((json) =>
      setSchedules(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken, from, to]);

  const moveMonth = (diff) => {
    const next = new Date(year, month - 1 + diff, 1);
    setYear(next.getFullYear());
    setMonth(next.getMonth() + 1);
  };

  // 날짜 문자열(yyyy-MM-dd) 비교로 해당 일에 걸친 일정을 고른다
  const getDaySchedules = (day) =>
    schedules.filter(
      (sc) => sc.startAt.slice(0, 10) <= day && day <= sc.endAt.slice(0, 10),
    );

  const selectedSchedules = getDaySchedules(selected);

  return (
    <WSCard
      title={`${year}년 ${month}월`}
      action={
        <div className={s.miniNav}>
          <button
            type="button"
            className={s.miniNavBtn}
            onClick={() => moveMonth(-1)}
            aria-label="이전 달"
          >
            <ChevronLeft size={14} />
          </button>
          <button
            type="button"
            className={s.miniNavBtn}
            onClick={() => moveMonth(1)}
            aria-label="다음 달"
          >
            <ChevronRight size={14} />
          </button>
          <WSButton
            label="캘린더"
            variant="secondary"
            size="sm"
            icon={<ArrowRight size={12} />}
            onClick={() => navigate("/calendar")}
          />
        </div>
      }
    >
      <div className={s.miniGrid}>
        {WEEKDAYS.map((weekday) => (
          <span key={weekday} className={s.miniWeekday}>
            {weekday}
          </span>
        ))}
        {days.map((day) => (
          <button
            key={day}
            type="button"
            className={`${s.miniDay} ${day.startsWith(monthPrefix) ? "" : s.miniDayOutside} ${day === today ? s.miniDayToday : ""} ${day === selected ? s.miniDaySelected : ""}`}
            onClick={() => setSelected(day)}
          >
            {Number(day.slice(8))}
            {getDaySchedules(day).length > 0 && <span className={s.miniDot} />}
          </button>
        ))}
      </div>

      <div className={s.dayList}>
        <p className={s.dayListTitle}>
          {Number(selected.slice(5, 7))}월 {Number(selected.slice(8))}일 일정
        </p>
        {selectedSchedules.length === 0 ? (
          <p className={s.empty}>등록된 일정이 없습니다.</p>
        ) : (
          selectedSchedules.map((sc) => (
            <div key={sc.id} className={s.dayItem}>
              <span className={s.dayItemTime}>
                {sc.isAllDay ? "종일" : sc.startAt.slice(11, 16)}
              </span>
              <span className={s.dayItemTitle}>{sc.title}</span>
            </div>
          ))
        )}
      </div>
    </WSCard>
  );
}
