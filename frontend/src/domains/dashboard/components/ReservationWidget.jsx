// 홈 화면의 내 예약 위젯 — 앞으로 예정된 자원 예약을 보여준다
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import { getMyReservations } from "../../reservation/services/reservationApi";
import s from "./HomeWidgets.module.css";

const MAX_ITEMS = 3;

export default function ReservationWidget() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [reservations, setReservations] = useState([]);

  useEffect(() => {
    if (!accessToken) return;
    getMyReservations(accessToken).then((json) =>
      setReservations(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken]);

  return (
    <WSCard
      title="내 예약"
      subtitle={`예정 ${reservations.length}건`}
      action={
        <WSButton
          label="예약"
          variant="secondary"
          size="sm"
          icon={<ArrowRight size={12} />}
          onClick={() => navigate("/reservation")}
        />
      }
    >
      {reservations.length === 0 ? (
        <p className={s.empty}>예정된 예약이 없습니다.</p>
      ) : (
        reservations.slice(0, MAX_ITEMS).map((reservation) => (
          <div key={reservation.id} className={s.dayItem}>
            <span className={s.dayItemTime}>
              {reservation.startAt.slice(5, 10)} {reservation.startAt.slice(11, 16)}
            </span>
            <span className={s.dayItemTitle}>
              {reservation.resourceName} · {reservation.title}
            </span>
          </div>
        ))
      )}
    </WSCard>
  );
}
