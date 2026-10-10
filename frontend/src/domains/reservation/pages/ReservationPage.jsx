// 회의실·차량·비품의 일자별 예약 현황을 보고 예약·취소하는 자원 예약 페이지
import { useState, useEffect } from "react";
import { Plus, Settings, CalendarClock } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSCard,
  WSButton,
  WSModal,
  WSModalActions,
  WSFormField,
  WSInput,
  WSSelect,
  WSDatepicker,
  WSAlert,
  WSEmptyState,
} from "../../../components/common/CommonWidgets";
import { toDateString } from "../../../utils/dateFormatter";
import {
  getMyInfo,
  getResources,
  createResource,
  updateResource,
  getReservations,
  getMyReservations,
  reserve,
  cancelReservation,
} from "../services/reservationApi";
import ResourceManageModal, {
  CATEGORY_LABEL,
} from "../components/ResourceManageModal";
import s from "./ReservationPage.module.css";

// 시간표에 표시할 시간대 (08시 ~ 20시)
const HOURS = Array.from({ length: 12 }, (_, i) => i + 8);

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatPeriod(reservation) {
  return `${reservation.startAt.slice(0, 10)} ${reservation.startAt.slice(11, 16)} ~ ${reservation.endAt.slice(11, 16)}`;
}

export default function ReservationPage() {
  const { accessToken } = useAuthContext();
  const today = toDateString(new Date());

  const [me, setMe] = useState(null);
  const [date, setDate] = useState(today);
  const [resources, setResources] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [myReservations, setMyReservations] = useState([]);
  // form: null(닫힘) | { resourceId, title, startTime, endTime }
  const [form, setForm] = useState(null);
  const [formError, setFormError] = useState("");
  const [showManage, setShowManage] = useState(false);

  const activeResources = resources.filter((r) => r.isActive);

  const loadResources = async () => {
    const json = await getResources(accessToken);
    setResources(Array.isArray(json?.data) ? json.data : []);
  };

  const loadReservations = async () => {
    const json = await getReservations(accessToken, date);
    setReservations(Array.isArray(json?.data) ? json.data : []);
  };

  const loadMyReservations = async () => {
    const json = await getMyReservations(accessToken);
    setMyReservations(Array.isArray(json?.data) ? json.data : []);
  };

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => setMe(json?.data ?? null));
    loadResources();
    loadMyReservations();
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken || !date) return;
    loadReservations();
  }, [accessToken, date]);

  // 해당 자원의 해당 시간대(1시간)와 겹치는 예약
  const findReservation = (resourceId, hour) => {
    const slotStart = `${date}T${pad(hour)}:00:00`;
    const slotEnd = `${date}T${pad(hour + 1)}:00:00`;
    return reservations.find(
      (r) =>
        r.resourceId === resourceId && r.startAt < slotEnd && r.endAt > slotStart,
    );
  };

  const openForm = (resourceId, hour) => {
    setFormError("");
    setForm({
      resourceId: String(resourceId ?? ""),
      title: "",
      startTime: `${pad(hour)}:00`,
      endTime: `${pad(hour + 1)}:00`,
    });
  };

  const isFormValid =
    form &&
    form.resourceId !== "" &&
    form.title.trim().length > 0 &&
    form.startTime < form.endTime;

  const handleReserve = async () => {
    const json = await reserve(accessToken, {
      resourceId: Number(form.resourceId),
      title: form.title.trim(),
      startAt: `${date}T${form.startTime}:00`,
      endAt: `${date}T${form.endTime}:00`,
    });
    if (!json || json.status >= 400) {
      setFormError(json?.message ?? "예약에 실패했습니다.");
      return;
    }
    setForm(null);
    loadReservations();
    loadMyReservations();
  };

  const handleCancel = async (reservation) => {
    if (!confirm(`'${reservation.title}' 예약을 취소하시겠습니까?`)) return;
    const json = await cancelReservation(accessToken, reservation.id);
    if (!json || json.status >= 400) {
      alert(json?.message ?? "예약 취소에 실패했습니다.");
      return;
    }
    loadReservations();
    loadMyReservations();
  };

  const handleSaveResource = async (resource) => {
    const json = resource.id
      ? await updateResource(accessToken, resource)
      : await createResource(accessToken, resource);
    if (json && json.status < 400) loadResources();
    return json;
  };

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <WSDatepicker value={date} onChange={(e) => setDate(e.target.value)} />
        <div className={s.toolbarActions}>
          {me?.role === "ADMIN" && (
            <WSButton
              label="자원 관리"
              variant="secondary"
              icon={<Settings size={15} />}
              onClick={() => setShowManage(true)}
            />
          )}
          <WSButton
            label="예약하기"
            icon={<Plus size={15} />}
            disabled={activeResources.length === 0}
            onClick={() => openForm(activeResources[0]?.id, 9)}
          />
        </div>
      </div>

      <WSCard title="예약 현황" subtitle={date} noPad>
        {activeResources.length === 0 ? (
          <WSEmptyState
            icon={<CalendarClock size={32} />}
            title="예약할 수 있는 자원이 없습니다"
            description="관리자가 자원을 등록하면 예약할 수 있습니다."
          />
        ) : (
          <div className={s.tableScroll}>
            <div className={s.table}>
              <div className={s.headCell} />
              {HOURS.map((hour) => (
                <div key={hour} className={s.headCell}>
                  {pad(hour)}시
                </div>
              ))}

              {activeResources.map((resource) => (
                <div key={resource.id} className={s.resourceRow}>
                  <div className={s.resourceCell}>
                    <span className={s.resourceName}>{resource.name}</span>
                    <span className={s.resourceSub}>
                      {CATEGORY_LABEL[resource.category]}
                      {resource.capacity ? ` · ${resource.capacity}명` : ""}
                    </span>
                  </div>
                  {HOURS.map((hour) => {
                    const reservation = findReservation(resource.id, hour);
                    if (!reservation) {
                      return (
                        <button
                          key={hour}
                          type="button"
                          className={s.slot}
                          aria-label={`${resource.name} ${pad(hour)}시 예약`}
                          onClick={() => openForm(resource.id, hour)}
                        />
                      );
                    }
                    return (
                      <div
                        key={hour}
                        className={`${s.slot} ${reservation.isCancelable ? s.slotMine : s.slotTaken}`}
                        title={`${reservation.title} (${reservation.employeeName}) ${reservation.startAt.slice(11, 16)}~${reservation.endAt.slice(11, 16)}`}
                        onClick={
                          reservation.isCancelable
                            ? () => handleCancel(reservation)
                            : undefined
                        }
                      >
                        <span className={s.slotTitle}>{reservation.title}</span>
                        <span className={s.slotSub}>
                          {reservation.employeeName}
                        </span>
                      </div>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        )}
      </WSCard>

      <WSCard title="내 예약" subtitle="앞으로 예정된 예약" noPad>
        {myReservations.length === 0 ? (
          <WSEmptyState
            icon={<CalendarClock size={32} />}
            title="예정된 예약이 없습니다"
          />
        ) : (
          myReservations.map((reservation) => (
            <div key={reservation.id} className={s.myRow}>
              <div>
                <p className={s.myTitle}>
                  {reservation.resourceName} · {reservation.title}
                </p>
                <p className={s.mySub}>{formatPeriod(reservation)}</p>
              </div>
              <WSButton
                label="취소"
                size="sm"
                variant="secondary"
                onClick={() => handleCancel(reservation)}
              />
            </div>
          ))
        )}
      </WSCard>

      <WSModal
        isOpen={!!form}
        onClose={() => setForm(null)}
        title="자원 예약"
        subtitle={date}
      >
        {form && (
          <div className={s.form}>
            <WSFormField label="자원" required>
              <WSSelect
                value={form.resourceId}
                options={activeResources.map((r) => ({
                  value: String(r.id),
                  label: r.name,
                }))}
                onChange={(e) =>
                  setForm({ ...form, resourceId: e.target.value })
                }
              />
            </WSFormField>
            <WSFormField label="예약 목적" required>
              <WSInput
                value={form.title}
                maxLength={100}
                placeholder="예: 주간 회의"
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </WSFormField>
            <WSFormField
              label="시간"
              required
              error={
                form.startTime < form.endTime
                  ? null
                  : "종료 시각은 시작 시각보다 늦어야 합니다."
              }
            >
              <div className={s.timeRow}>
                <WSInput
                  type="time"
                  value={form.startTime}
                  onChange={(e) =>
                    setForm({ ...form, startTime: e.target.value })
                  }
                />
                <span>~</span>
                <WSInput
                  type="time"
                  value={form.endTime}
                  onChange={(e) =>
                    setForm({ ...form, endTime: e.target.value })
                  }
                />
              </div>
            </WSFormField>
            {formError && (
              <WSAlert
                type="error"
                message={formError}
                onClose={() => setFormError("")}
              />
            )}
          </div>
        )}
        <WSModalActions>
          <WSButton
            label="취소"
            variant="secondary"
            onClick={() => setForm(null)}
          />
          <WSButton
            label="예약"
            disabled={!isFormValid}
            onClick={handleReserve}
          />
        </WSModalActions>
      </WSModal>

      <ResourceManageModal
        isOpen={showManage}
        onClose={() => setShowManage(false)}
        resources={resources}
        onSave={handleSaveResource}
      />
    </div>
  );
}
