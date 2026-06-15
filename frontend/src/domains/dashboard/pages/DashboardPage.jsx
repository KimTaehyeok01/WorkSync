import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
MessageCircle,
CalendarDays,
CheckSquare,
ClipboardCheck,
Users,
FileCheck,
ArrowRight,
Newspaper,
CheckCircle2,
} from "lucide-react";
import { getDashboard, getPendingApprovals, getRecentPosts, getDepartmentAttendance, getMyPendingApprovals , getUnreadMessageCount} from "../services/dashboardApi";
import { WSCard, WSStatCard, WSAvatar, WSButton } from "../../../components/common/CommonWidgets";
import useAuthContext from "../../../store/AuthContext";
import s from "./DashboardPage.module.css";

const ATTENDANCE_STATUS_LABEL = {
NORMAL:      "정상 출근",
LATE:        "지각",
EARLY_LEAVE: "조퇴",
ABSENT:      "결근",
};

const TASK_STATUS_LABEL = {
TODO:        "준비중",
IN_PROGRESS: "진행 중",
DONE:        "완료",
};

const TASK_STATUS_COLOR = {
TODO:        { bg: "#f3f4f6", text: "#6B7280" },
IN_PROGRESS: { bg: "#dbeafe", text: "#1A73E8" },
DONE:        { bg: "#d1fae5", text: "#059669" },
};

function formatTime(isoStr) {
if (!isoStr) return null;
const d = new Date(isoStr);
return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

function formatDate(isoStr) {
if (!isoStr) return "";
const d = new Date(isoStr);
return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

export default function DashboardPage() {
const navigate = useNavigate();
const { accessToken } = useAuthContext();

const [loading,         setLoading]         = useState(true);
const [dashboard,       setDashboard]       = useState(null);
const [myPendingDocs, setMyPendingDocs] = useState([]);
const [unreadMessages, setUnreadMessages] = useState(0);
const [pendingDocs,     setPendingDocs]     = useState([]);
const [recentPosts,     setRecentPosts]     = useState([]);
const [teamAttendance,  setTeamAttendance]  = useState([]);
const [emptyPosts,      setEmptyPosts]      = useState(false);
const [emptyApprovals,  setEmptyApprovals]  = useState(false);

useEffect(() => {
    if (!accessToken) return;
    fetchDashboard();
}, [accessToken]);


const fetchDashboard = async () => {
    try {
    setLoading(true);
    const today = new Date().toISOString().split("T")[0];
    const [dashboardRes, approvalRes, postRes, attendanceRes, myDocsRes, unreadMsgRes] = await Promise.all([
        getDashboard(accessToken),
        getPendingApprovals(accessToken),
        getRecentPosts(accessToken),
        getDepartmentAttendance(accessToken, today),
        getMyPendingApprovals(accessToken),
        getUnreadMessageCount(accessToken),
      ]);
      setUnreadMessages(unreadMsgRes ?? 0);
      setDashboard(dashboardRes ?? null);
      setPendingDocs(approvalRes ?? []);
      setRecentPosts(postRes ?? []);
      setTeamAttendance(attendanceRes ?? []);
      
      const combined = [...(approvalRes ?? []), ...(myDocsRes ?? [])];
      const unique = combined.filter((item, index, self) =>
        self.findIndex((i) => i.id === item.id) === index
      );
      setMyPendingDocs(unique);
    } catch (error) {
    console.error("Dashboard API Error", error);
    } finally {
    setLoading(false);
    }
};

const taskProgress = useMemo(() => {
    if (!dashboard) return "0/0";
    const total =
    dashboard.todoTaskCount +
    dashboard.inProgressTaskCount +
    dashboard.doneTaskCount;
    return `${dashboard.doneTaskCount}/${total}`;
}, [dashboard]);

const today = new Date().toLocaleDateString("ko-KR", {
    year: "numeric", month: "long", day: "numeric", weekday: "long",
});
const todayShort = new Date().toLocaleDateString("ko-KR", {
    month: "long", day: "numeric",
});
const sprintMonth = new Date().toLocaleDateString("ko-KR", {
    year: "numeric", month: "long",
});

if (loading || !dashboard) {
    return <div className={s.root}>로딩 중...</div>;
}
const presentMembers = teamAttendance.filter(a => a.status !== "ABSENT");

return (
    <div className={s.root}>

    {/* ── 배너 ── */}
    <div className={s.banner}>
        <div>
        <h2 className={s.bannerTitle}>안녕하세요 👋</h2>
        <p className={s.bannerSub}>
        오늘 <strong>결재 대기 {myPendingDocs.length}건</strong>과{" "}
            <strong>새 알림 {dashboard.unreadNotificationCount}건</strong>이 있습니다.
        </p>
        </div>
        <div className={s.bannerRight}>
        <div>
            <div className={s.bannerDateLabel}>오늘</div>
            <div className={s.bannerDate}>{today}</div>
        </div>
        <div className={s.bannerCalIcon}>
            <CalendarDays size={22} />
        </div>
        </div>
    </div>

    {/* ── 통계 카드 4개 ── */}
    <div className={s.statsGrid}>
        <WSStatCard
        label="결재 대기"
        value={String(myPendingDocs.length)}
        icon={<ClipboardCheck size={22} />}
        color="#F59E0B"
        />
        <WSStatCard
        label="오늘 팀원 출근"
        value={`${presentMembers.length}/${teamAttendance.length}`}
        icon={<Users size={22} />}
        color="#8B5CF6"
        />
        <WSStatCard
        label="업무 진행률"
        value={taskProgress}
        icon={<CheckSquare size={22} />}
        color="#1A73E8"
        />
        <WSStatCard
        label="읽지 않은 메세지"
        value={String(unreadMessages)}
        icon={<MessageCircle size={22} />}
        color="#10B981"
        />
    </div>

    {/* ── 메인 그리드 ── */}
    <div className={s.mainGrid}>

        {/* 왼쪽 2칸 */}
        <div className={`${s.mainCol} ${s.mainColLeft}`}>

        {/* ─ 최근 게시글 ─ */}
        <WSCard
            title="최근 게시글"
            subtitle="공지사항"
            action={
            <div className={s.headerActions}>
                <WSButton
                label="전체 보기"
                variant="secondary"
                size="sm"
                icon={<ArrowRight size={12} />}
                onClick={() => navigate("/board")}
                />
            </div>
            }
        >
            {emptyPosts || recentPosts.length === 0 ? (
            <div className={s.emptyBlock}>
                <div className={`${s.emptyIconWrap} ${s.emptyIconWrapPosts}`}>
                <Newspaper size={26} className={s.emptyIconPosts} />
                </div>
                <div>
                <p className={s.emptyTitle}>아직 게시물이 없습니다</p>
                <p className={s.emptySub}>새 게시물이 등록되면 여기에 표시됩니다.</p>
                </div>
            </div>
            ) : (
            <div className={s.postList}>
                {recentPosts.map((post) => {
                const isNotice = post.boardName?.includes("공지");
                return (
                    <div
                    key={post.id}
                    onClick={() => navigate(`/board/${post.boardId}/${post.id}`)}
                    className={s.postItem}
                    >
                    <div className={s.postBody}>
                        <p className={s.postTitle}>
                        {isNotice && (
                            <span className={s.postNoticeBadge}>공지사항</span>
                        )}
                        {post.title}
                        </p>
                        <p className={s.postContent}>{post.content}</p>
                        <div className={s.postMeta}>
                        <span style={{ marginRight: 6 }}>
                            <WSAvatar name={post.authorName} size={21} />
                        </span>
                        <span style={{ marginRight: 10 }}>{post.authorName}</span>
                        ·{" "}
                        <span style={{ marginLeft: 10 }}>{formatDate(post.createdAt)}</span>
                        </div>
                    </div>
                    </div>
                );
                })}
            </div>
            )}
        </WSCard>

        {/* ─ 결재 대기 문서 ─ */}
        <WSCard
            title="결재 대기 문서"
            subtitle="처리가 필요한 문서"
            action={
            <div className={s.headerActions}>
                <WSButton
                label="전체 보기"
                variant="secondary"
                size="sm"
                icon={<ArrowRight size={12} />}
                onClick={() => navigate("/approval")}
                />
            </div>
            }
        >
            {emptyApprovals || pendingDocs.length === 0 ? (
            <div className={s.emptyBlock}>
                <div className={s.successIconStack}>
                <div className={`${s.emptyIconWrap} ${s.emptyIconWrapApprovals}`}>
                    <FileCheck size={26} className={s.emptyIconApprovals} />
                </div>
                <span className={s.successDoneBadge}>
                    <CheckCircle2 size={13} color="#fff" />
                </span>
                </div>
                <div>
                <p className={s.emptyTitle}>모든 결재가 완료되었습니다</p>
                <p className={s.emptySub}>새 결재 요청이 오면 여기에 표시됩니다.</p>
                </div>
                <div className={s.successBadgeRow}>
                <CheckCircle2 size={14} className={s.successInlineIcon} />
                <span className={s.successBadgeStrong}>오늘 결재 처리 완료 ·</span>
                <span className={s.successBadgeSub}>수고하셨습니다!</span>
                </div>
            </div>
            ) : (
            <div className={s.approvalList}>
                {pendingDocs.map((doc) => (
                <div
                    key={doc.id}
                    onClick={() => navigate(`/approval/${doc.id}`)}
                    className={s.approvalItem}
                >
                    <div className={s.approvalIconBox}>
                    <FileCheck size={16} className={s.approvalIcon} />
                    </div>
                    <div className={s.approvalBody}>
                    <p className={s.approvalTitle}>{doc.title}</p>
                    <p className={s.approvalMeta}>
                        {doc.drafterName} · {formatDate(doc.submittedAt)}
                        {doc.formName ? ` · ${doc.formName}` : ""}
                    </p>
                    </div>
                </div>
                ))}
            </div>
            )}
        </WSCard>
        </div>

        {/* 오른쪽 1칸 */}
        <div className={s.mainCol}>

        {/* ─ 나의 팀 현황 ─ */}
        <WSCard title="나의 팀 현황" subtitle={`오늘, ${todayShort}`}>
  {teamAttendance.length === 0 ? (
    <div className={s.emptyBlock}>
      <div className={`${s.emptyIconWrap} ${s.emptyIconWrapPosts}`}>
        <Users size={24} className={s.emptyIconPosts} />
      </div>
      <p className={s.emptyTitle}>팀원 정보가 없습니다</p>
      <p className={s.emptySub}>오늘 출근 기록이 있는 팀원이 없습니다.</p>
    </div>
  ) : (
    <div className={s.attendList}>
      {teamAttendance.map((a) => (
        <div key={a.employeeId} className={s.attendRow}>
          <WSAvatar
            src={a.profileImage}
            name={a.employeeName}
            size={28}
          />
          <span className={s.attendName}>{a.employeeName}</span>
          <span className={`${s.attendBadge} ${a.status === "ABSENT" ? s.attendAbsent : s.attendPresent}`}>
            {formatTime(a.checkInTime) ?? "결근"}
          </span>
        </div>
      ))}
    </div>
  )}
</WSCard>

        {/* ─ 최근 업무 ─ */}
        <WSCard title="업무 진행률" subtitle={`현재 스프린트 — ${sprintMonth}`}>
  <div className={s.approvalList}>

    <div className={s.approvalItem}>
      <div className={s.approvalBody} style={{ width: "100%" }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
          <span style={{ fontSize: 13, color: "#6B7280" }}>준비중</span>
          <span style={{ fontSize: 13, fontWeight: 600 }}>{dashboard.todoTaskCount}</span>
        </div>
        <div style={{ background: "#e5e7eb", borderRadius: 999, height: 6 }}>
          <div style={{ width: `${(dashboard.todoTaskCount / (dashboard.todoTaskCount + dashboard.inProgressTaskCount + dashboard.doneTaskCount)) * 100}%`, background: "#6B7280", borderRadius: 999, height: 6 }} />
        </div>
      </div>
    </div>

    <div className={s.approvalItem}>
      <div className={s.approvalBody} style={{ width: "100%" }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
          <span style={{ fontSize: 13, color: "#1A73E8" }}>진행중</span>
          <span style={{ fontSize: 13, fontWeight: 600, color: "#1A73E8" }}>{dashboard.inProgressTaskCount}</span>
        </div>
        <div style={{ background: "#e5e7eb", borderRadius: 999, height: 6 }}>
          <div style={{ width: `${(dashboard.inProgressTaskCount / (dashboard.todoTaskCount + dashboard.inProgressTaskCount + dashboard.doneTaskCount)) * 100}%`, background: "#1A73E8", borderRadius: 999, height: 6 }} />
        </div>
      </div>
    </div>

    <div className={s.approvalItem}>
      <div className={s.approvalBody} style={{ width: "100%" }}>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
          <span style={{ fontSize: 13, color: "#059669" }}>완료</span>
          <span style={{ fontSize: 13, fontWeight: 600, color: "#059669" }}>{dashboard.doneTaskCount}</span>
        </div>
        <div style={{ background: "#e5e7eb", borderRadius: 999, height: 6 }}>
          <div style={{ width: `${(dashboard.doneTaskCount / (dashboard.todoTaskCount + dashboard.inProgressTaskCount + dashboard.doneTaskCount)) * 100}%`, background: "#059669", borderRadius: 999, height: 6 }} />
        </div>
      </div>
    </div>

  </div>
</WSCard>
        </div>
    </div>
    </div>
);
}