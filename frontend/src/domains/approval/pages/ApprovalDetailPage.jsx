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
import {
  getMyInfo,
  getApprovalById,
  processApproval,
  getLeaveBalance,
} from "../services/approvalApi";
import s from "./ApprovalDetailPage.module.css";
import { useState, useEffect, Fragment } from "react";

const STATUS_CONFIG = {
  IN_PROGRESS: { label: "대기", bg: "#FEF3C7", text: "#92400E" },
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

// 양식 items  JSON문자열 배열 변환 함수
function parseJSON(str) {
  try {
    return JSON.parse(str ?? "[]");
  } catch {
    return [];
  }
}

// 연차 신청서
function LeaveDetail({ items, approval }) {
  const { accessToken } = useAuthContext();
  const [balance, setBalance] = useState(null);

  useEffect(() => {
    getLeaveBalance(accessToken).then((data) => {
      setBalance(data);
    }, []);
  });

  //휴가 종류 매핑
  const LEAVE_TYPE = {
    ANNUAL: "연차",
    HALF: "반차",
    SICK: "병가",
    OTHER: "휴가",
  };
  // 날짜 표시 함수
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    const [year, month, day] = dateStr.split("-");
    return `${year}.${month}.${day}`;
  };
  // 휴가 기간 표시 함수
  const leavePeriod = () => {
    if (items.halfDate) {
      return `${formatDate(items.halfDate)} ${items.halfTime === "AM" ? "오전" : "오후"}`;
    }
    if (items.startDate && items.endDate) {
      return `${formatDate(items.startDate)} - ${formatDate(items.endDate)}`;
    }
    return items.days ?? "-";
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <tbody>
            <tr>
              <th>제목</th>
              <td>{approval.title ?? "-"}</td>
            </tr>
            <tr>
              <th>소속</th>
              <td>{items.departmentName ?? "-"}</td>
            </tr>
            <tr>
              <th>작성자</th>
              <td>{items.name ?? "-"}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <tbody>
            <tr>
              <th>휴가 종류</th>
              <td>{LEAVE_TYPE[items.leaveType] ?? "-"}</td>
              <th>잔여일</th>
              <td>{balance ? `${balance.remainingDays}일` : "0일"}</td>
            </tr>
            <tr>
              <th>휴가 기간</th>
              <td colSpan={3}>{leavePeriod()}</td>
            </tr>
            <tr>
              <th>휴가 사유</th>
              <td colSpan={3}>{items.reason ?? "-"}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

// 구매요청서
function PurchaseDetail({ items, approval }) {
  const rows = parseJSON(items.items);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <tbody>
            <tr>
              {" "}
              <th>제목</th>
              <td colSpan={3}>{approval.title ?? "-"}</td>
            </tr>
            <tr>
              <th>소속</th>
              <td>{items.departmentName ?? "-"}</td>
              <th>작성자</th>
              <td>{items.name ?? "-"}</td>
            </tr>
            <tr>
              <th>구매 용도</th>
              <td colSpan={3}>{items.purpose ?? "-"}</td>
            </tr>
            <tr>
              <th>합계</th>
              <td colSpan={3}>
                {Number(items.amount).toLocaleString() ?? "-"}원
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <thead>
            <tr>
              <th>요청 품목</th>
              <th>수량</th>
              <th>단가</th>
              <th>금액</th>
              <th>비고</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => (
              <tr key={idx}>
                <td>{row.item ?? "-"}</td>
                <td>{Number(row.quantity).toLocaleString() ?? "-"}</td>
                <td>{Number(row.unitPrice).toLocaleString() ?? "-"}</td>
                <td>{Number(row.amount).toLocaleString() ?? "-"}</td>
                <td>{row.note ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// 지출결의서
function ExpenseDetail({ items, approval }) {
  const rows = parseJSON(items.items);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <tbody>
            <tr>
              <th>제목</th>
              <td>{approval.title ?? "-"}</td>
            </tr>
            <tr>
              <th>소속</th>
              <td>{items.departmentName ?? "-"}</td>
              <th>작성자</th>
              <td>{items.name ?? "-"}</td>
            </tr>
            <tr>
              <th>지출 사유</th>
              <td colSpan={3}>{items.reason ?? "-"}</td>
            </tr>
            <tr>
              <th>금액</th>
              <td colSpan={3}>
                {Number(items.amount).toLocaleString() ?? "-"}원
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className={s.detailTableWrap}>
        <table className={s.detailTable}>
          <thead>
            <tr>
              <th>일자</th>
              <th>분류</th>
              <th>사용 내역</th>
              <th>금액</th>
              <th>비고</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => (
              <tr key={idx}>
                <td>{row.date ?? "-"}</td>
                <td>{row.category ?? "-"}</td>
                <td>{row.description ?? "-"}</td>
                <td>{Number(row.amount).toLocaleString() ?? "-"}</td>
                <td>{row.note ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// 출장신청서
function BusinessTripDetail({ items }) {
  const travelers = parseJSON(items.travelers);
  const expenses = parseJSON(items.expenses);
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    const [year, month, day] = dateStr.split("-");
    return `${year}.${month}.${day}`;
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
      {/* 시행 정보 */}
      <div>
        <p className={s.sectionDivider} style={{ marginBottom: "8px" }}>
          시행 정보
        </p>
        <div className={s.detailTableWrap}>
          <table className={s.detailTable}>
            <tbody>
              <tr>
                <th>시행자</th>
                <td>{items.name ?? "-"}</td>
                <th>소속</th>
                <td>{items.departmentName ?? "-"}</td>
              </tr>
              <tr>
                <th>시행 일자</th>
                <td colSpan={3}>
                  {formatDate(items.executionStartDate) ?? "-"}
                  <span style={{ padding: "5px" }}>-</span>
                  {formatDate(items.executionEndDate) ?? "-"}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* 출장자 */}
      <div>
        <p className={s.sectionSubTitle} style={{ marginBottom: "8px" }}>
          출장자
        </p>
        <div className={s.detailTableWrap}>
          <table className={s.detailTable}>
            <thead>
              <tr>
                <th>성명</th>
                <th>소속</th>
                <th>직급</th>
                <th>사번</th>
              </tr>
            </thead>
            <tbody>
              {travelers.map((row, idx) => (
                <tr key={idx}>
                  <td>{row.name ?? "-"}</td>
                  <td>{row.dept ?? "-"}</td>
                  <td>{row.grade ?? "-"}</td>
                  <td>{row.empNo ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 출장지 및 일정별 */}
      <div>
        <p className={s.sectionSubTitle} style={{ marginBottom: "8px" }}>
          출장지 및 일정별
        </p>
        <div className={s.detailTableWrap}>
          <table className={s.detailTable}>
            <tbody>
              <tr>
                <th>출장지</th>
                <td>{items.destination ?? "-"}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* 출장비 */}
      <div>
        <p className={s.sectionSubTitle} style={{ marginBottom: "8px" }}>
          출장비
        </p>
        <div className={s.detailTableWrap}>
          <table className={s.detailTable}>
            <thead>
              <tr>
                <th>항목</th>
                <th>금액</th>
                <th>산출 내역</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map((row, idx) => (
                <tr key={idx}>
                  <td>{row.category ?? "-"}</td>
                  <td>{Number(row.amount).toLocaleString() ?? "-"}</td>
                  <td>{row.note ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
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
    });
  }, [accessToken, id]);

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      if (!data) return;
      setMe(data);
      console.log("me.id : ", me?.id);
    });
  }, [accessToken]);

  if (!approval) {
    return (
      <div className={s.notFound}>
        <p>문서를 찾을 수 없습니다</p>
      </div>
    );
  }

  // 결재자 확인
  const myLine = approvalLines.find((line) => line.approverId === me?.id);
  // 참조자 확인
  const isReference = myLine?.stepType === "REFERENCE";
  // 결재 버튼 활성화 조건
  const canProcess = myLine && myLine.status === "WAITING" && !isReference;

  const handleApprove = async () => {
    const result = await processApproval(accessToken, id, "APPROVED");
    if (result?.status === 200) {
      alert("결재 승인이 완료되었습니다.");
      navigate("/approval");
    } else {
      alert("처리 중 오류가 발생했습니다.");
    }
    getApprovalById(accessToken, id).then((data) => {
      if (!data) return;
      setApproval(data);
      setStatus(data.status);
      setApprovalLines(data.approvalLines ?? []);
    });
  };

  const handleReject = async () => {
    const result = await processApproval(accessToken, id, "REJECTED");
    if (result?.status === 200) {
      alert("결재 반려가 완료되었습니다.");
      navigate("/approval");
    } else {
      alert("처리 중 오류가 발생했습니다.");
    }
    getApprovalById(accessToken, id).then((data) => {
      if (!data) return;
      setApproval(data);
      setStatus(data.status);
      setApprovalLines(data.approvalLines ?? []);
    });
  };

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
                    {me?.jobGrade} ·
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

          {approvalLines
            .filter((step) => step.stepType !== "REFERENCE")
            .map((step, idx) => (
              <Fragment key={idx}>
                <ChevronRight size={16} className={s.stepArrow} />
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
              </Fragment>
            ))}
        </div>
      </div>

      <div className={s.section}>
        <h2 className={s.sectionTitle}>{approval.formName}</h2>
        <p className={s.sectionSub}>제출된 문서</p>
        {approval.formId === 1 && (
          <LeaveDetail items={approval.items} approval={approval}></LeaveDetail>
        )}
        {approval.formId === 2 && (
          <ExpenseDetail
            items={approval.items}
            approval={approval}
          ></ExpenseDetail>
        )}
        {approval.formId === 3 && (
          <PurchaseDetail
            items={approval.items}
            approval={approval}
          ></PurchaseDetail>
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

      {canProcess && (
        <div className={s.actions}>
          <button className={s.btnReject} onClick={handleReject}>
            결재 반려
          </button>
          <button className={s.btnApprove} onClick={handleApprove}>
            결재 승인
          </button>
        </div>
      )}
    </div>
  );
}
