import { useParams, useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import {
  CheckCircle,
  XCircle,
  ChevronRight,
  Download,
  X,
  Clock,
} from "lucide-react";
import { APPROVAL_DOCS, TEAM_MEMBERS } from "../../../constants/mockData";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { getMyInfo, getApprovalById } from "../services/approvalApi";
import s from "./ApprovalDetailPage.module.css";
import { useState, useEffect } from "react";

const STATUS_CONFIG = {
  PENDING: { label: "대기", bg: "#FEF3C7", text: "#92400E" },
  APPROVED: { label: "승인", bg: "#D1FAE5", text: "#065F46" },
  REJECTED: { label: "반려", bg: "#FEE2E2", text: "#991B1B" },
};

const APPROVAL_STEPS = [
  { role: "기안자", member: TEAM_MEMBERS[1], status: "approved" },
  { role: "검토자", member: TEAM_MEMBERS[3], status: "rejected" },
  { role: "최종 승인자", member: TEAM_MEMBERS[0], status: "pending" },
];

function stepClass(status) {
  if (status === "APPROVED") return s.stepApproved;
  if (status === "REJECTED") return s.stepRejected;
  return s.stepPending;
}

function stepLabelColor(status) {
  if (status === "APPROVED") return "#16A34A";
  if (status === "REJECTED") return "#DC2626";
  return "#9CA3AF";
}

// 연차 신청서
function LeaveDetail({ items }) {
  return (
    <div className={s.detailTableWrap}>
      <table className={s.detailTable}>
        <tbody>
          <tr>
            <th>소속</th>
            <td>{items.departmentName ?? "-"}</td>
            <th>작성자</th>
            <td>{items.name ?? "-"}</td>
          </tr>
          <tr>
            <th>휴가 종류</th>
            <td>{items.leaveType ?? "-"}</td>
            <th>휴가 기간</th>
            <td>{items.days ?? "-"}</td>
          </tr>
          <tr>
            <th>휴가 사유</th>
            <td colSpan={3}>{items.reason ?? "-"}</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

export default function ApprovalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [status, setStatus] = useState(null);
  const [approvalLines, setApprovalLines] = useState([]);
  const [approval, setApproval] = useState(null);
  const [me, setMe] = useState(null);
  const fallbackStatusConfig = {
    label: "알 수 없음",
    bg: "#E5E7EB",
    text: "#374151",
  };

  useEffect(() => {
    if (!accessToken) return;
    getApprovalById(accessToken, id).then((data) => {
      if (!data) return;
      setApproval(data);
      setStatus(data.status);
      setApprovalLines(data.approvalLines ?? []);
      console.log("items : ", data.items);
    });
  }, [accessToken, id]);

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setMe(data);
    });
  });

  if (!approval) {
    return (
      <div className={s.notFound}>
        <p>문서를 찾을 수 없습니다</p>
      </div>
    );
  }

  const statusConfig = STATUS_CONFIG[approval.status] ?? fallbackStatusConfig;

  return (
    <div className={s.root}>
      <div className={s.section}>
        <div className={s.headerRow}>
          <div className={s.headerLeft}>
            <div
              className={s.statusBadge}
              style={{
                "--status-bg": statusConfig.bg,
                "--status-color": statusConfig.text,
              }}
            >
              {statusConfig.label}
            </div>
            <h1 className={s.title}>{approval.title}</h1>
            <div className={s.requesterRow}>
              <WSAvatar src={null} name={approval.drafterName} size={32} />
              <div>
                <p className={s.requesterName}>{approval.drafterName}</p>
                <div style={{ display: "flex" }}>
                  <p className={s.requesterDate} style={{ marginRight: "5px" }}>
                    {me.jobGrade} ·
                  </p>
                  <p className={s.requesterDate}>
                    {new Date(approval.createdAt).toLocaleDateString("ko-KR")}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <button onClick={() => navigate("/approval")} className={s.closeBtn}>
            <X size={20} />
          </button>
        </div>
      </div>

      <div className={s.section}>
        <h2 className={s.sectionTitle}>결재선</h2>
        <p className={s.sectionSub}>결재 진행 상황</p>

        <div className={s.stepsRow}>
          <div className={s.stepGroup}>
            <div className={`${s.step} ${s.stepApproved}`}>
              <div className={s.stepStatusRow}>
                <CheckCircle size={14} color="#16A34A"></CheckCircle>
                <span
                  className={s.stepStatusLabel}
                  style={{ "--step-color": "#16A34A" }}
                >
                  승인됨
                </span>
              </div>
              <WSAvatar src={null} name={approval.drafterName} size={40} />
              <p className={s.stepName}>{approval.drafterName}</p>
              <p className={s.stepRole}>기안자</p>
            </div>
            {approvalLines.length > 0 && (
              <ChevronRight size={16} className={s.stepArrow} />
            )}
          </div>
          {approvalLines.map((step, idx) => (
            <div key={idx} className={s.stepGroup}>
              <div className={`${s.step} ${stepClass(step.status)}`}>
                <div className={s.stepStatusRow}>
                  {step.status === "APPROVED" && (
                    <CheckCircle size={14} color="#16A34A" />
                  )}
                  {step.status === "REJECTED" && (
                    <XCircle size={14} color="#DC2626" />
                  )}
                  {step.status === "WAITING" && (
                    <Clock size={14} color="#9CA3AF" />
                  )}
                  <span
                    className={s.stepStatusLabel}
                    style={{ "--step-color": stepLabelColor(step.status) }}
                  >
                    {step.status === "APPROVED"
                      ? "승인"
                      : step.status === "REJECTED"
                        ? "반려"
                        : "대기"}
                  </span>
                </div>
                <WSAvatar src={null} name={step.approverName} size={40} />
                <p className={s.stepName}>{step.approverName}</p>
                <p className={s.stepRole}>
                  {step.stepType === "REVIEW"
                    ? "검토자"
                    : step.stepType === "APPROVE"
                      ? "최종 승인자"
                      : step.stepType === "REFERENCE"
                        ? "참조자"
                        : "-"}
                </p>
              </div>
              {idx < approvalLines.length - 1 && (
                <ChevronRight size={16} className={s.stepArrow} />
              )}
            </div>
          ))}
        </div>
      </div>

      <div className={s.section}>
        <h2 className={s.sectionTitle}>{approval.formName}</h2>
        <p className={s.sectionSub}>제출된 문서 미리보기</p>
        {approval.formId === 1 && (
          <LeaveDetail items={approval.items}></LeaveDetail>
        )}
        {approval.formId === 2 && (
          <ExpenseDetail items={approval.items}></ExpenseDetail>
        )}
        {approval.formId === 3 && (
          <PurchaseDetail items={approval.items}></PurchaseDetail>
        )}
        {approval.formId === 4 && (
          <BusinessTripDetail items={approval.items}></BusinessTripDetail>
        )}
      </div>

      <div className={s.section}>
        <div className={s.attach}>
          <div className={s.attachLeft}>
            <div className={s.attachIcon}>XLSX</div>
            <div>
              <p className={s.attachName}>Q3_예산_요청.xlsx</p>
              <p className={s.attachSize}>1.2 MB</p>
            </div>
          </div>
          <button className={s.attachDl}>
            <Download size={18} />
          </button>
        </div>
      </div>

      <div className={s.actions}>
        <button className={s.btnReject}>결재 반려</button>
        <button className={s.btnApprove}>결재 승인</button>
      </div>
    </div>
  );
}
