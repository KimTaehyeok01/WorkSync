// 개인·부서·전사 일정과 휴가·업무 마감을 월간 달력으로 보여주는 캘린더 페이지
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSButton } from "../../../components/common/CommonWidgets";
import { toDateString, buildMonthGrid } from "../../../utils/dateFormatter";
import {
  getMyInfo,
  getSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  getDepartmentLeaves,
  getMyTasks,
} from "../services/calendarApi";
import ScheduleModal, { SCOPE_LABEL } from "../components/ScheduleModal";
import s from "./CalendarPage.module.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

const LEAVE_TYPE = {
  ANNUAL: "연차",
  HALF: "반차",
  SICK: "병가",
  FAMILY: "경조사",
  OTHER: "휴가",
};

const SCOPE_CHIP = {
  PERSONAL: s.chipPersonal,
  DEPARTMENT: s.chipDepartment,
  COMPANY: s.chipCompany,
};

const LEGEND = [
  ...Object.entries(SCOPE_LABEL).map(([scope, label]) => ({
    label: `${label} 일정`,
    className: SCOPE_CHIP[scope],
  })),
  { label: "부서원 휴가", className: s.chipLeave },
  { label: "업무 마감", className: s.chipTask },
];

export default function CalendarPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const now = new Date();
  const today = toDateString(now);

  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [me, setMe] = useState(null);
  const [schedules, setSchedules] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [tasks, setTasks] = useState([]);
  // modal: null(닫힘) | { schedule, date }
  const [modal, setModal] = useState(null);

  const days = buildMonthGrid(year, month);
  const from = days[0];
  const to = days[days.length - 1];

  const loadSchedules = async () => {
    const json = await getSchedules(accessToken, from, to);
    setSchedules(Array.isArray(json?.data) ? json.data : []);
  };

  // 내 정보와 내 담당 업무는 한 번만 조회
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => {
      const data = json?.data;
      if (!data) return;
      setMe(data);
      getMyTasks(accessToken, data.id).then((taskJson) => {
        const content = taskJson?.data?.content;
        setTasks(Array.isArray(content) ? content : []);
      });
    });
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    loadSchedules();
    getDepartmentLeaves(accessToken, from, to).then((json) =>
      setLeaves(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken, from, to]);

  const moveMonth = (diff) => {
    const next = new Date(year, month - 1 + diff, 1);
    setYear(next.getFullYear());
    setMonth(next.getMonth() + 1);
  };

  const goToday = () => {
    setYear(now.getFullYear());
    setMonth(now.getMonth() + 1);
  };

  const handleSave = async (schedule) => {
    const json = schedule.id
      ? await updateSchedule(accessToken, schedule)
      : await createSchedule(accessToken, schedule);
    if (json && json.status < 400) {
      setModal(null);
      loadSchedules();
    }
    return json;
  };

  const handleDelete = async (id) => {
    const json = await deleteSchedule(accessToken, id);
    if (json && json.status < 400) {
      setModal(null);
      loadSchedules();
    }
    return json;
  };

  // 날짜 문자열(yyyy-MM-dd) 비교로 해당 일에 걸친 항목을 고른다
  const getDayItems = (day) => ({
    schedules: schedules.filter(
      (sc) => sc.startAt.slice(0, 10) <= day && day <= sc.endAt.slice(0, 10),
    ),
    leaves: leaves.filter((l) => l.startDate <= day && day <= l.endDate),
    tasks: tasks.filter((t) => t.dueDate === day && t.status !== "DONE"),
  });

  const monthPrefix = `${year}-${String(month).padStart(2, "0")}`;

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.nav}>
          <button
            type="button"
            className={s.navBtn}
            onClick={() => moveMonth(-1)}
            aria-label="이전 달"
          >
            <ChevronLeft size={16} />
          </button>
          <span className={s.monthLabel}>
            {year}년 {month}월
          </span>
          <button
            type="button"
            className={s.navBtn}
            onClick={() => moveMonth(1)}
            aria-label="다음 달"
          >
            <ChevronRight size={16} />
          </button>
          <WSButton label="오늘" size="sm" variant="secondary" onClick={goToday} />
        </div>
        <WSButton
          label="일정 등록"
          icon={<Plus size={15} />}
          onClick={() => setModal({ schedule: null, date: today })}
        />
      </div>

      <div className={s.legend}>
        {LEGEND.map((item) => (
          <span key={item.label} className={s.legendItem}>
            <span className={`${s.legendDot} ${item.className}`} />
            {item.label}
          </span>
        ))}
      </div>

      <div className={s.calendar}>
        {WEEKDAYS.map((weekday, idx) => (
          <div
            key={weekday}
            className={`${s.weekday} ${idx === 0 ? s.sunday : ""} ${idx === 6 ? s.saturday : ""}`}
          >
            {weekday}
          </div>
        ))}

        {days.map((day, idx) => {
          const items = getDayItems(day);
          const dayOfWeek = idx % 7;
          return (
            <div
              key={day}
              className={`${s.cell} ${day.startsWith(monthPrefix) ? "" : s.cellOutside}`}
              onClick={() => setModal({ schedule: null, date: day })}
            >
              <span
                className={`${s.dayNumber} ${day === today ? s.today : ""} ${dayOfWeek === 0 ? s.sunday : ""} ${dayOfWeek === 6 ? s.saturday : ""}`}
              >
                {Number(day.slice(8))}
              </span>

              {items.schedules.map((sc) => (
                <button
                  key={`s-${sc.id}`}
                  type="button"
                  className={`${s.chip} ${SCOPE_CHIP[sc.scope]}`}
                  title={sc.title}
                  onClick={(e) => {
                    e.stopPropagation();
                    setModal({ schedule: sc, date: day });
                  }}
                >
                  {!sc.isAllDay && sc.startAt.slice(0, 10) === day && (
                    <span className={s.chipTime}>{sc.startAt.slice(11, 16)}</span>
                  )}
                  {sc.title}
                </button>
              ))}

              {items.leaves.map((leave) => (
                <span
                  key={`l-${leave.id}`}
                  className={`${s.chip} ${s.chipLeave}`}
                  title={`${leave.employeeName} ${LEAVE_TYPE[leave.leaveType]}`}
                  onClick={(e) => e.stopPropagation()}
                >
                  {leave.employeeName} {LEAVE_TYPE[leave.leaveType]}
                </span>
              ))}

              {items.tasks.map((task) => (
                <button
                  key={`t-${task.id}`}
                  type="button"
                  className={`${s.chip} ${s.chipTask}`}
                  title={task.title}
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/tasks/${task.id}`);
                  }}
                >
                  마감 · {task.title}
                </button>
              ))}
            </div>
          );
        })}
      </div>

      <ScheduleModal
        isOpen={!!modal}
        onClose={() => setModal(null)}
        schedule={modal?.schedule ?? null}
        date={modal?.date ?? today}
        isAdmin={me?.role === "ADMIN"}
        onSave={handleSave}
        onDelete={handleDelete}
      />
    </div>
  );
}
