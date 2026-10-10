// 개인·부서·전사 일정과 휴가·업무 마감·내 예약을 월간·주간·일간으로 보여주는 캘린더 페이지
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight, Plus, Repeat } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSButton } from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { WEEKDAYS, LEAVE_TYPE_LABEL } from "../../../constants/statusLabels";
import { toDateString, buildMonthGrid } from "../../../utils/dateFormatter";
import {
  getMyInfo,
  getEmployees,
  getSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  getDepartmentLeaves,
  getMyTasks,
  getMyReservations,
} from "../services/calendarApi";
import ScheduleModal, { SCOPE_LABEL } from "../components/ScheduleModal";
import TimeGridView from "../components/TimeGridView";
import s from "./CalendarPage.module.css";

const VIEWS = [
  { key: "month", label: "월" },
  { key: "week", label: "주" },
  { key: "day", label: "일" },
];

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
  { label: "내 예약", className: s.chipReservation },
];

function addDays(date, days) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

// 보기 방식에 따라 화면에 표시할 날짜 문자열 목록
function getDays(view, cursor) {
  if (view === "month") {
    return buildMonthGrid(cursor.getFullYear(), cursor.getMonth() + 1);
  }
  if (view === "week") {
    const sunday = addDays(cursor, -cursor.getDay());
    return Array.from({ length: 7 }, (_, i) => toDateString(addDays(sunday, i)));
  }
  return [toDateString(cursor)];
}

function getTitle(view, cursor, days) {
  if (view === "month") {
    return `${cursor.getFullYear()}년 ${cursor.getMonth() + 1}월`;
  }
  if (view === "week") {
    return `${days[0].slice(5).replace("-", ".")} ~ ${days[6].slice(5).replace("-", ".")}`;
  }
  return `${cursor.getMonth() + 1}월 ${cursor.getDate()}일 (${WEEKDAYS[cursor.getDay()]})`;
}

export default function CalendarPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const today = toDateString(new Date());

  const [view, setView] = useState("month");
  // 보고 있는 기간의 기준 날짜
  const [cursor, setCursor] = useState(new Date());
  const [me, setMe] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [reservations, setReservations] = useState([]);
  // modal: null(닫힘) | { schedule, date, hour }
  const [modal, setModal] = useState(null);

  const days = getDays(view, cursor);
  const from = days[0];
  const to = days[days.length - 1];

  const loadSchedules = async () => {
    const json = await getSchedules(accessToken, from, to);
    setSchedules(Array.isArray(json?.data) ? json.data : []);
  };

  // 내 정보·직원 목록·내 담당 업무·내 예약은 한 번만 조회
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
    getEmployees(accessToken).then((json) =>
      setEmployees(Array.isArray(json?.data) ? json.data : []),
    );
    getMyReservations(accessToken).then((json) =>
      setReservations(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    loadSchedules();
    getDepartmentLeaves(accessToken, from, to).then((json) =>
      setLeaves(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken, from, to]);

  // 이전·다음 — 보기 방식만큼 이동
  const move = (direction) => {
    if (view === "month") {
      setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + direction, 1));
    } else {
      setCursor(addDays(cursor, direction * (view === "week" ? 7 : 1)));
    }
  };

  const handleSave = async (schedule) => {
    const json = schedule.id
      ? await updateSchedule(accessToken, schedule)
      : await createSchedule(accessToken, schedule);
    if (isOk(json)) {
      setModal(null);
      loadSchedules();
    }
    return json;
  };

  const handleDelete = async (id) => {
    const json = await deleteSchedule(accessToken, id);
    if (isOk(json)) {
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
    reservations: reservations.filter((r) => r.startAt.slice(0, 10) === day),
  });

  // 휴가·업무 마감·내 예약 칩 — 월간 칸과 주간·일간의 종일 줄에서 같이 쓴다
  const renderExtras = (items, chipClass) => (
    <>
      {items.leaves.map((leave) => (
        <span
          key={`l-${leave.id}`}
          className={`${chipClass} ${s.chipLeave}`}
          title={`${leave.employeeName} ${LEAVE_TYPE_LABEL[leave.leaveType]}`}
          onClick={(e) => e.stopPropagation()}
        >
          {leave.employeeName} {LEAVE_TYPE_LABEL[leave.leaveType]}
        </span>
      ))}
      {items.tasks.map((task) => (
        <button
          key={`t-${task.id}`}
          type="button"
          className={`${chipClass} ${s.chipTask}`}
          title={task.title}
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/tasks/${task.id}`);
          }}
        >
          마감 · {task.title}
        </button>
      ))}
      {items.reservations.map((reservation) => (
        <button
          key={`r-${reservation.id}`}
          type="button"
          className={`${chipClass} ${s.chipReservation}`}
          title={`${reservation.resourceName} · ${reservation.title}`}
          onClick={(e) => {
            e.stopPropagation();
            navigate("/reservation");
          }}
        >
          {reservation.startAt.slice(11, 16)} {reservation.resourceName}
        </button>
      ))}
    </>
  );

  const monthPrefix = `${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, "0")}`;

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.nav}>
          <button
            type="button"
            className={s.navBtn}
            onClick={() => move(-1)}
            aria-label="이전"
          >
            <ChevronLeft size={16} />
          </button>
          <span className={s.monthLabel}>{getTitle(view, cursor, days)}</span>
          <button
            type="button"
            className={s.navBtn}
            onClick={() => move(1)}
            aria-label="다음"
          >
            <ChevronRight size={16} />
          </button>
          <WSButton
            label="오늘"
            size="sm"
            variant="secondary"
            onClick={() => setCursor(new Date())}
          />
        </div>
        <div className={s.toolbarRight}>
          <div className={s.viewToggle}>
            {VIEWS.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`${s.viewBtn} ${view === item.key ? s.viewBtnOn : ""}`}
                onClick={() => setView(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>
          <WSButton
            label="일정 등록"
            icon={<Plus size={15} />}
            onClick={() => setModal({ schedule: null, date: today, hour: 9 })}
          />
        </div>
      </div>

      <div className={s.legend}>
        {LEGEND.map((item) => (
          <span key={item.label} className={s.legendItem}>
            <span className={`${s.legendDot} ${item.className}`} />
            {item.label}
          </span>
        ))}
      </div>

      {view === "month" ? (
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
                onClick={() => setModal({ schedule: null, date: day, hour: 9 })}
              >
                <span
                  className={`${s.dayNumber} ${day === today ? s.today : ""} ${dayOfWeek === 0 ? s.sunday : ""} ${dayOfWeek === 6 ? s.saturday : ""}`}
                >
                  {Number(day.slice(8))}
                </span>

                {items.schedules.map((sc) => (
                  <button
                    key={`s-${sc.id}-${sc.startAt}`}
                    type="button"
                    className={`${s.chip} ${SCOPE_CHIP[sc.scope]}`}
                    title={sc.title}
                    onClick={(e) => {
                      e.stopPropagation();
                      setModal({ schedule: sc, date: day, hour: 9 });
                    }}
                  >
                    {sc.repeatType !== "NONE" && (
                      <Repeat size={10} className={s.chipIcon} />
                    )}
                    {!sc.isAllDay && sc.startAt.slice(0, 10) === day && (
                      <span className={s.chipTime}>{sc.startAt.slice(11, 16)}</span>
                    )}
                    {sc.title}
                  </button>
                ))}
                {renderExtras(items, s.chip)}
              </div>
            );
          })}
        </div>
      ) : (
        <TimeGridView
          days={days}
          today={today}
          getDayItems={getDayItems}
          scopeClass={SCOPE_CHIP}
          renderExtras={renderExtras}
          onCreate={(date, hour) => setModal({ schedule: null, date, hour })}
          onSelect={(schedule, date) => setModal({ schedule, date, hour: 9 })}
        />
      )}

      <ScheduleModal
        isOpen={!!modal}
        onClose={() => setModal(null)}
        schedule={modal?.schedule ?? null}
        date={modal?.date ?? today}
        hour={modal?.hour ?? 9}
        isAdmin={me?.role === "ADMIN"}
        employees={employees}
        myId={me?.id}
        onSave={handleSave}
        onDelete={handleDelete}
      />
    </div>
  );
}
