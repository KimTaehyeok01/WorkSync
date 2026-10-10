// 직원의 인사 발령 이력(부서·직급·권한 변경) 카드
import { useState, useEffect } from "react";
import { ArrowRight } from "lucide-react";
import { WSCard } from "../../../components/common/CommonWidgets";
import { JOB_GRADE_LABEL, ROLE_LABEL } from "../../../constants/statusLabels";
import { formatDateTime } from "../../../utils/dateFormatter";
import { getEmployeeHistory } from "../services/employeeAdminApi";
import s from "./EmployeeHistoryCard.module.css";

const TYPE_LABEL = {
  DEPARTMENT: "부서",
  JOB_GRADE: "직급",
  ROLE: "권한",
};

// 직급·권한은 코드값으로 저장되므로 화면에서는 한글로 바꿔 보여준다
function display(changeType, value) {
  if (!value || value === "null") return "없음";
  if (changeType === "JOB_GRADE") return JOB_GRADE_LABEL[value] ?? value;
  if (changeType === "ROLE") return ROLE_LABEL[value] ?? value;
  return value;
}

export default function EmployeeHistoryCard({ accessToken, employeeId }) {
  const [history, setHistory] = useState([]);

  useEffect(() => {
    if (!accessToken || !employeeId) return;
    getEmployeeHistory(accessToken, employeeId).then((json) =>
      setHistory(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken, employeeId]);

  return (
    <WSCard
      title="인사 발령 이력"
      subtitle="부서·직급·권한이 바뀔 때마다 자동으로 기록됩니다."
      className={s.card}
    >
      {history.length === 0 ? (
        <p className={s.empty}>발령 이력이 없습니다.</p>
      ) : (
        <div className={s.list}>
          {history.map((item) => (
            <div key={item.id} className={s.row}>
              <span className={s.type}>{TYPE_LABEL[item.changeType] ?? item.changeType}</span>
              <span className={s.change}>
                {display(item.changeType, item.beforeValue)}
                <ArrowRight size={13} className={s.arrow} />
                <strong>{display(item.changeType, item.afterValue)}</strong>
              </span>
              <span className={s.meta}>
                {item.actorName ?? "-"} · {formatDateTime(item.createdAt)}
              </span>
            </div>
          ))}
        </div>
      )}
    </WSCard>
  );
}
