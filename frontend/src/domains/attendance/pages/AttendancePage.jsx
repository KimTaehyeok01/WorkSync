// 내 출퇴근 기록과 연차 현황을 보여주는 근태·연차 페이지
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  LogIn,
  LogOut,
  CalendarDays,
  Clock,
  AlertTriangle,
  Plus,
  ClipboardList,
  Download,
  Pencil,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSCard,
  WSStatCard,
  WSBadge,
  WSButton,
  WSEmptyState,
  WSTableHeader,
  WSDatepicker,
  WSModal,
  WSModalActions,
  WSFormField,
  WSInput,
  WSSelect,
  WSAlert,
} from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import {
  WEEKDAYS,
  ATTENDANCE_STATUS,
  LEAVE_TYPE_LABEL,
  LEAVE_STATUS,
} from "../../../constants/statusLabels";
import {
  toDateString,
  formatTime,
  downloadCsv,
} from "../../../utils/dateFormatter";
import {
  getMyInfo,
  checkIn,
  checkOut,
  getMyAttendance,
  getAdminAttendance,
  updateAttendance,
  getLeaveBalance,
  getMyLeaves,
} from "../services/attendanceApi";
import s from "./AttendancePage.module.css";

const MY_COLUMNS = ["근무일", "상태", "출근", "퇴근", "근무시간"];
const ADMIN_COLUMNS = ["이름", "부서", "상태", "출근", "퇴근", "근무시간", ""];
const MY_GRID = "140px 100px 120px 120px 1fr";
const ADMIN_GRID = "140px 140px 90px 90px 90px 1fr 48px";
const LEAVE_GRID = "90px 1fr 80px 100px";

const STATUS_OPTIONS = Object.entries(ATTENDANCE_STATUS).map(
  ([value, { label }]) => ({ value, label }),
);

// 출근~퇴근 사이 근무 분 (퇴근 전이면 0)
function getWorkMinutes(record) {
  if (!record.checkInTime || !record.checkOutTime) return 0;
  const diff = new Date(record.checkOutTime) - new Date(record.checkInTime);
  return Math.max(0, Math.floor(diff / 60000));
}

function formatMinutes(minutes) {
  if (!minutes) return "-";
  return `${Math.floor(minutes / 60)}시간 ${minutes % 60}분`;
}

function formatWorkDate(dateString) {
  const day = new Date(`${dateString}T00:00:00`).getDay();
  return `${dateString.slice(5)} (${WEEKDAYS[day]})`;
}

function StatusBadge({ status }) {
  return (
    <WSBadge
      status={ATTENDANCE_STATUS[status]?.badge}
      label={ATTENDANCE_STATUS[status]?.label}
    />
  );
}

export default function AttendancePage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const now = new Date();
  const today = toDateString(now);

  const [tab, setTab] = useState("my");
  const [role, setRole] = useState(null);
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [records, setRecords] = useState([]);
  const [todayRecord, setTodayRecord] = useState(null);
  const [balance, setBalance] = useState(null);
  const [leaves, setLeaves] = useState([]);
  const [adminDate, setAdminDate] = useState(today);
  const [adminRecords, setAdminRecords] = useState([]);
  const [message, setMessage] = useState("");
  // editing: null(닫힘) | 관리자 수정 폼 값
  const [editing, setEditing] = useState(null);
  const [editError, setEditError] = useState("");

  // 오늘 출퇴근 기록 조회
  const loadToday = async () => {
    const json = await getMyAttendance(
      accessToken,
      now.getFullYear(),
      now.getMonth() + 1,
    );
    const list = Array.isArray(json?.data) ? json.data : [];
    setTodayRecord(list.find((r) => r.workDate === today) ?? null);
  };

  // 선택한 월의 근태 기록 조회
  const loadMonth = async () => {
    const json = await getMyAttendance(accessToken, year, month);
    const list = Array.isArray(json?.data) ? json.data : [];
    setRecords([...list].sort((a, b) => b.workDate.localeCompare(a.workDate)));
  };

  const loadAdmin = async () => {
    const json = await getAdminAttendance(accessToken, adminDate);
    setAdminRecords(Array.isArray(json?.data) ? json.data : []);
  };

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => setRole(json?.data?.role ?? null));
    getLeaveBalance(accessToken).then((json) => setBalance(json?.data ?? null));
    getMyLeaves(accessToken, now.getFullYear()).then((json) =>
      setLeaves(Array.isArray(json?.data) ? json.data : []),
    );
    loadToday();
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    loadMonth();
  }, [accessToken, year, month]);

  useEffect(() => {
    if (!accessToken || tab !== "admin" || !adminDate) return;
    loadAdmin();
  }, [accessToken, tab, adminDate]);

  // 출근·퇴근 처리 — 실패하면 서버 메시지를 그대로 보여준다
  const handleCheck = async (apiCall) => {
    const json = await apiCall(accessToken);
    if (!isOk(json)) {
      setMessage(json?.message ?? "처리에 실패했습니다.");
      return;
    }
    setMessage("");
    loadToday();
    loadMonth();
  };

  const moveMonth = (diff) => {
    const next = new Date(year, month - 1 + diff, 1);
    setYear(next.getFullYear());
    setMonth(next.getMonth() + 1);
  };

  const downloadMine = () =>
    downloadCsv(`근태_${year}-${String(month).padStart(2, "0")}.csv`, [
      MY_COLUMNS,
      ...records.map((r) => [
        r.workDate,
        ATTENDANCE_STATUS[r.status]?.label,
        formatTime(r.checkInTime, ""),
        formatTime(r.checkOutTime, ""),
        formatMinutes(getWorkMinutes(r)),
      ]),
    ]);

  const downloadAdmin = () =>
    downloadCsv(`전체근태_${adminDate}.csv`, [
      ADMIN_COLUMNS.slice(0, 6),
      ...adminRecords.map((r) => [
        r.employeeName,
        r.departmentName ?? "",
        ATTENDANCE_STATUS[r.status]?.label,
        formatTime(r.checkInTime, ""),
        formatTime(r.checkOutTime, ""),
        formatMinutes(getWorkMinutes(r)),
      ]),
    ]);

  const openEdit = (record) => {
    setEditError("");
    setEditing({
      employeeId: record.employeeId,
      employeeName: record.employeeName,
      checkInTime: formatTime(record.checkInTime, ""),
      checkOutTime: formatTime(record.checkOutTime, ""),
      status: record.status,
    });
  };

  const handleSaveEdit = async () => {
    const json = await updateAttendance(accessToken, {
      employeeId: editing.employeeId,
      workDate: adminDate,
      checkInTime: editing.checkInTime || null,
      checkOutTime: editing.checkOutTime || null,
      status: editing.status,
    });
    if (!isOk(json)) {
      setEditError(json?.message ?? "저장에 실패했습니다.");
      return;
    }
    setEditing(null);
    loadAdmin();
  };

  const lateCount = records.filter((r) => r.status === "LATE").length;
  const totalMinutes = records.reduce((sum, r) => sum + getWorkMinutes(r), 0);
  const absentCount = adminRecords.filter((r) => r.status === "ABSENT").length;

  return (
    <div className={s.root}>
      {role === "ADMIN" && (
        <div className={s.tabs}>
          <button
            type="button"
            className={`${s.tab} ${tab === "my" ? s.tabActive : ""}`}
            onClick={() => setTab("my")}
          >
            내 근태
          </button>
          <button
            type="button"
            className={`${s.tab} ${tab === "admin" ? s.tabActive : ""}`}
            onClick={() => setTab("admin")}
          >
            전체 근태
          </button>
        </div>
      )}

      {tab === "my" ? (
        <>
          <div className={s.topGrid}>
            <WSCard title="오늘 근무" subtitle={today}>
              <div className={s.todayTimes}>
                <div>
                  <p className={s.timeLabel}>출근</p>
                  <p className={s.timeValue}>
                    {formatTime(todayRecord?.checkInTime)}
                  </p>
                </div>
                <div>
                  <p className={s.timeLabel}>퇴근</p>
                  <p className={s.timeValue}>
                    {formatTime(todayRecord?.checkOutTime)}
                  </p>
                </div>
                {todayRecord && <StatusBadge status={todayRecord.status} />}
              </div>
              <div className={s.todayActions}>
                <WSButton
                  label="출근"
                  icon={<LogIn size={15} />}
                  disabled={!!todayRecord}
                  onClick={() => handleCheck(checkIn)}
                />
                <WSButton
                  label="퇴근"
                  variant="secondary"
                  icon={<LogOut size={15} />}
                  disabled={!todayRecord || !!todayRecord.checkOutTime}
                  onClick={() => handleCheck(checkOut)}
                />
              </div>
              {message && <p className={s.message}>{message}</p>}
            </WSCard>

            <WSCard
              title="연차 현황"
              subtitle={balance ? `${balance.year}년` : ""}
              action={
                <WSButton
                  label="휴가 신청"
                  size="sm"
                  icon={<Plus size={14} />}
                  onClick={() => navigate("/approval/new")}
                />
              }
            >
              <div className={s.balanceRow}>
                <div className={s.balanceItem}>
                  <p className={s.timeLabel}>총 연차</p>
                  <p className={s.timeValue}>{balance?.totalDays ?? "-"}</p>
                </div>
                <div className={s.balanceItem}>
                  <p className={s.timeLabel}>사용</p>
                  <p className={s.timeValue}>{balance?.usedDays ?? "-"}</p>
                </div>
                <div className={s.balanceItem}>
                  <p className={s.timeLabel}>잔여</p>
                  <p className={`${s.timeValue} ${s.remaining}`}>
                    {balance?.remainingDays ?? "-"}
                  </p>
                </div>
              </div>
            </WSCard>
          </div>

          <div className={s.statsGrid}>
            <WSStatCard
              label="출근 일수"
              value={`${records.length}일`}
              icon={<CalendarDays size={20} />}
              color="#1A73E8"
            />
            <WSStatCard
              label="지각"
              value={`${lateCount}회`}
              icon={<AlertTriangle size={20} />}
              color="#F59E0B"
            />
            <WSStatCard
              label="총 근무시간"
              value={formatMinutes(totalMinutes)}
              icon={<Clock size={20} />}
              color="#10B981"
            />
          </div>

          <WSCard
            title="월별 근태 기록"
            noPad
            action={
              <div className={s.monthNav}>
                <button
                  type="button"
                  className={s.monthBtn}
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
                  className={s.monthBtn}
                  onClick={() => moveMonth(1)}
                  aria-label="다음 달"
                >
                  <ChevronRight size={16} />
                </button>
                <WSButton
                  label="CSV"
                  size="sm"
                  variant="secondary"
                  icon={<Download size={13} />}
                  disabled={records.length === 0}
                  onClick={downloadMine}
                />
              </div>
            }
          >
            <WSTableHeader columns={MY_COLUMNS} gridTemplate={MY_GRID} />
            {records.length === 0 ? (
              <WSEmptyState
                icon={<ClipboardList size={32} />}
                title="근태 기록이 없습니다"
                description="해당 월에 출근한 기록이 없습니다."
              />
            ) : (
              records.map((r) => (
                <div key={r.id} className={`${s.row} ${s.rowMy}`}>
                  <span className={s.cellStrong}>
                    {formatWorkDate(r.workDate)}
                  </span>
                  <span>
                    <StatusBadge status={r.status} />
                  </span>
                  <span>{formatTime(r.checkInTime)}</span>
                  <span>{formatTime(r.checkOutTime)}</span>
                  <span>{formatMinutes(getWorkMinutes(r))}</span>
                </div>
              ))
            )}
          </WSCard>

          <WSCard title="휴가 신청 내역" subtitle={`${now.getFullYear()}년`} noPad>
            <WSTableHeader
              columns={["유형", "기간", "일수", "상태"]}
              gridTemplate={LEAVE_GRID}
            />
            {leaves.length === 0 ? (
              <WSEmptyState
                icon={<CalendarDays size={32} />}
                title="휴가 신청 내역이 없습니다"
              />
            ) : (
              leaves.map((leave) => (
                <div
                  key={leave.id}
                  className={`${s.row} ${s.rowLeave} ${leave.approvalDocId ? s.rowClickable : ""}`}
                  onClick={() =>
                    leave.approvalDocId &&
                    navigate(`/approval/${leave.approvalDocId}`)
                  }
                >
                  <span className={s.cellStrong}>
                    {LEAVE_TYPE_LABEL[leave.leaveType] ?? leave.leaveType}
                  </span>
                  <span>
                    {leave.startDate} ~ {leave.endDate}
                  </span>
                  <span>{leave.daysCount}일</span>
                  <span>
                    <WSBadge
                      status={LEAVE_STATUS[leave.status]?.badge}
                      label={LEAVE_STATUS[leave.status]?.label}
                    />
                  </span>
                </div>
              ))
            )}
          </WSCard>
        </>
      ) : (
        <WSCard
          title="일자별 전체 근태"
          subtitle={`재직 ${adminRecords.length}명 · 미출근 ${absentCount}명`}
          noPad
          action={
            <div className={s.monthNav}>
              <WSDatepicker
                value={adminDate}
                max={today}
                onChange={(e) => setAdminDate(e.target.value)}
              />
              <WSButton
                label="CSV"
                size="sm"
                variant="secondary"
                icon={<Download size={13} />}
                disabled={adminRecords.length === 0}
                onClick={downloadAdmin}
              />
            </div>
          }
        >
          <WSTableHeader columns={ADMIN_COLUMNS} gridTemplate={ADMIN_GRID} />
          {adminRecords.length === 0 ? (
            <WSEmptyState
              icon={<ClipboardList size={32} />}
              title="직원이 없습니다"
            />
          ) : (
            adminRecords.map((r) => (
              <div key={r.employeeId} className={`${s.row} ${s.rowAdmin}`}>
                <span className={s.cellStrong}>{r.employeeName}</span>
                <span>{r.departmentName ?? "미배정"}</span>
                <span>
                  <StatusBadge status={r.status} />
                </span>
                <span>{formatTime(r.checkInTime)}</span>
                <span>{formatTime(r.checkOutTime)}</span>
                <span>{formatMinutes(getWorkMinutes(r))}</span>
                <button
                  type="button"
                  className={s.editBtn}
                  onClick={() => openEdit(r)}
                  aria-label={`${r.employeeName} 근태 수정`}
                >
                  <Pencil size={14} />
                </button>
              </div>
            ))
          )}
        </WSCard>
      )}

      <WSModal
        isOpen={!!editing}
        onClose={() => setEditing(null)}
        title="근태 수정"
        subtitle={editing ? `${editing.employeeName} · ${adminDate}` : ""}
        size="sm"
      >
        {editing && (
          <div className={s.editForm}>
            <WSFormField label="출근 시각" helperText="비우면 출근 기록 없음으로 저장됩니다.">
              <WSInput
                type="time"
                value={editing.checkInTime}
                onChange={(e) =>
                  setEditing({ ...editing, checkInTime: e.target.value })
                }
              />
            </WSFormField>
            <WSFormField label="퇴근 시각">
              <WSInput
                type="time"
                value={editing.checkOutTime}
                onChange={(e) =>
                  setEditing({ ...editing, checkOutTime: e.target.value })
                }
              />
            </WSFormField>
            <WSFormField label="상태" required>
              <WSSelect
                value={editing.status}
                options={STATUS_OPTIONS}
                onChange={(e) => setEditing({ ...editing, status: e.target.value })}
              />
            </WSFormField>
            {editError && (
              <WSAlert type="error" message={editError} onClose={() => setEditError("")} />
            )}
          </div>
        )}
        <WSModalActions>
          <WSButton label="취소" variant="secondary" onClick={() => setEditing(null)} />
          <WSButton label="저장" disabled={!editing?.status} onClick={handleSaveEdit} />
        </WSModalActions>
      </WSModal>
    </div>
  );
}
