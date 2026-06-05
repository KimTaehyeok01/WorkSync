import { useParams, useNavigate } from "react-router-dom";
import useAuthContext from "../../../store/AuthContext";
import { CheckCircle, XCircle, ChevronRight, Download, X } from "lucide-react";
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

export default function ApprovalDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [status, setStatus] = useState(null);
  const [approvalLines, setApprovalLines] = useState([]);
  const [approval, setApproval] = useState(null);
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
      console.log("items 찐 : " + JSON.stringify(data.items, null, 2));
    });
  }, [accessToken, id]);

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
                <p className={s.requesterDate}>{approval.createdAt}</p>
              </div>
            </div>
          </div>
          <button onClick={() => navigate("/approvals")} className={s.closeBtn}>
            <X size={20} />
          </button>
        </div>
      </div>

      <div className={s.section}>
        <h2 className={s.sectionTitle}>결재선</h2>
        <p className={s.sectionSub}>ws-apv-approval-line</p>

        <div className={s.stepsRow}>
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
                <WSAvatar src={null} name={step.drafterName} size={40} />
                <p className={s.stepName}>{step.drafterName}</p>
                {/* <p className={s.stepRole}>{step.role}</p> */}
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

        <div className={s.previewStack}>
          <div className={s.infoGrid}>
            <div>
              <p className={s.infoLabel}>제목</p>
              <p className={s.infoValue}>{approval.title}</p>
            </div>
            <div>
              <p className={s.infoLabel}>작성자</p>
              <p className={s.infoValue}>{approval.drafterName}</p>
            </div>
            <div>
              <p className={s.infoLabel}>작성일</p>
              <p className={s.infoValue}>{approval.createdAt?.slice(0, 10)}</p>
            </div>
            <div>
              <p className={s.infoLabel}>제출일</p>
              <p className={s.infoValue}>
                {approval.submittedAt?.slice(0, 10) ?? "미제출"}
              </p>
            </div>
          </div>
        </div>
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
