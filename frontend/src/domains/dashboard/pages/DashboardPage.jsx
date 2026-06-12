import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Bell,
  CalendarDays,
  CheckSquare,
  ClipboardCheck,
  Users,
  FileCheck,
  ArrowRight,
  Newspaper,
  CheckCircle2,
} from "lucide-react";
import { getDashboard, getPendingApprovals, getRecentPosts } from "../services/dashboardApi";
import { getMyTaskList } from "../../task/services/taskApi";
import { WSCard, WSStatCard, WSAvatar, WSButton } from "../../../components/common/CommonWidgets";
import useAuthContext from "../../../store/AuthContext";
import s from "./DashboardPage.module.css";

// ── 상수 ─────────────────────────────────────────────────────────────────────

// 출근 상태 → 한글
const ATTENDANCE_STATUS_LABEL = {
  NORMAL:      "정상 출근",
  LATE:        "지각",
  EARLY_LEAVE: "조퇴",
  ABSENT:      "결근",
};

// 업무 상태 → 한글
const TASK_STATUS_LABEL = {
  TODO:        "준비중",
  IN_PROGRESS: "진행 중",
  DONE:        "완료",
};

// 업무 상태 뱃지 색상 (CSS 없는 항목이라 inline style 사용)
const TASK_STATUS_COLOR = {
  TODO:        { bg: "#f3f4f6", text: "#6B7280" },
  IN_PROGRESS: { bg: "#dbeafe", text: "#1A73E8" },
  DONE:        { bg: "#d1fae5", text: "#059669" },
};

// LocalDateTime → "2026.06.11" 포맷
function formatDate(isoStr) {
  if (!isoStr) return "";
  const d = new Date(isoStr);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

// ── 컴포넌트 ──────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();

  const [loading,      setLoading]      = useState(true);
  const [dashboard,    setDashboard]    = useState(null);
  const [tasks,        setTasks]        = useState([]); // GET /api/tasks/my
  const [pendingDocs,  setPendingDocs]  = useState([]); // GET /api/approvals/pending
  const [recentPosts,  setRecentPosts]  = useState([]); // GET /api/boards (NOTICE) → posts
  const [emptyPosts,      setEmptyPosts]      = useState(false);
  const [emptyApprovals,  setEmptyApprovals]  = useState(false);

  useEffect(() => {
    if (!accessToken) return;
    fetchDashboard();
  }, [accessToken]);

  const fetchDashboard = async () => {
    try {
      setLoading(true);

      // 4개 API 병렬 호출
      const [dashboardRes, taskRes, approvalRes, postRes] = await Promise.all([
        getDashboard(accessToken),          // → json.data (DashboardResponse)
        getMyTaskList(accessToken),         // → json.data (Page<TaskResponse>)
        getPendingApprovals(accessToken),   // → json.data[] (ApprovalListResponse[])
        getRecentPosts(accessToken),        // → PostResponse[]
      ]);

      setDashboard(dashboardRes ?? null);
      setTasks(taskRes?.content ?? []);    // Page 구조에서 content 추출
      setPendingDocs(approvalRes ?? []);
      setRecentPosts(postRes ?? []);
    } catch (error) {
      console.error("Dashboard API Error", error);
    } finally {
      setLoading(false);
    }
  };

  // 업무 진행률 계산
  const taskProgress = useMemo(() => {
    if (!dashboard) return "0/0";
    const total =
      dashboard.todoTaskCount +
      dashboard.inProgressTaskCount +
      dashboard.doneTaskCount;
    return `${dashboard.doneTaskCount}/${total}`;
  }, [dashboard]);

  // 날짜 포맷
  const today = new Date().toLocaleDateString("ko-KR", {
    year: "numeric", month: "long", day: "numeric", weekday: "long",
  });
  const todayShort = new Date().toLocaleDateString("ko-KR", {
    month: "long", day: "numeric",
  });
  const sprintMonth = new Date().toLocaleDateString("ko-KR", {
    year: "numeric", month: "long",
  });

  // 출근 상태 뱃지 CSS 클래스 결정
  const attendanceBadgeClass =
    dashboard?.checkedIn ? s.attendPresent : s.attendAbsent;

  if (loading || !dashboard) {
    return <div className={s.root}>로딩 중...</div>;
  }

  return (
    <div className={s.root}>

      {/* ── 배너 ── */}
      <div className={s.banner}>
        <div>
          <h2 className={s.bannerTitle}>안녕하세요 👋</h2>
          <p className={s.bannerSub}>
            오늘 <strong>결재 대기 {dashboard.pendingApprovalCount}건</strong>과{" "}
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
          value={String(dashboard.pendingApprovalCount)}
          icon={<ClipboardCheck size={22} />}
          color="#F59E0B"
        />
        <WSStatCard
          label="읽지 않은 알림"
          value={String(dashboard.unreadNotificationCount)}
          icon={<Bell size={22} />}
          color="#8B5CF6"
        />
        <WSStatCard
          label="업무 진행률"
          value={taskProgress}
          icon={<CheckSquare size={22} />}
          color="#1A73E8"
        />
        <WSStatCard
          label="남은 연차"
          value={dashboard.remainingLeaveDays != null ? String(dashboard.remainingLeaveDays) : "-"}
          icon={<Users size={22} />}
          color="#10B981"
        />
      </div>

      {/* ── 메인 그리드 ── */}
      <div className={s.mainGrid}>

        {/* 왼쪽 2칸 */}
        <div className={`${s.mainCol} ${s.mainColLeft}`}>

          {/* ─ 최근 게시글 — GET /api/boards?boardType=NOTICE → /posts ─ */}
          <WSCard
            title="최근 게시글"
            subtitle="공지사항"
            action={
              <div className={s.headerActions}>
                <button
                  onClick={() => setEmptyPosts(!emptyPosts)}
                  className={`${s.toggleBtn} ${emptyPosts ? s.toggleBtnOn : s.toggleBtnOff}`}
                >
                  {emptyPosts ? "목록 보기" : "빈 화면"}
                </button>
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
                      onClick={() => navigate("/board")}
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
                          {/* 실제 API에는 프로필 이미지 없음 → name으로 이니셜 아바타 */}
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

          {/* ─ 결재 대기 문서 — GET /api/approvals/pending ─ */}
          <WSCard
            title="결재 대기 문서"
            subtitle="처리가 필요한 문서"
            action={
              <div className={s.headerActions}>
                <button
                  onClick={() => setEmptyApprovals(!emptyApprovals)}
                  className={`${s.toggleBtn} ${emptyApprovals ? s.toggleBtnOn : s.toggleBtnOff}`}
                >
                  {emptyApprovals ? "목록 보기" : "빈 화면"}
                </button>
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
                      {/* drafterName: 기안자 / submittedAt: 상신일 / formName: 양식명 */}
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

          {/* ─ 오늘 출근 상태 — dashboard API 실데이터 ─ */}
          <WSCard title="오늘 출근 상태" subtitle={`오늘, ${todayShort}`}>
            <div className={s.attendList}>
              {/* 출근 상태 */}
              <div className={s.attendRow}>
                <span className={s.attendName}>출근 상태</span>
                <span className={`${s.attendBadge} ${attendanceBadgeClass}`}>
                  {dashboard.checkedIn
                    ? ATTENDANCE_STATUS_LABEL[dashboard.todayAttendanceStatus] ?? "출근"
                    : "미출근"}
                </span>
              </div>
              {/* 퇴근 여부 */}
              <div className={s.attendRow}>
                <span className={s.attendName}>퇴근 여부</span>
                <span
                  className={`${s.attendBadge} ${
                    dashboard.checkedOut ? s.attendPresent : s.attendAbsent
                  }`}
                >
                  {dashboard.checkedOut ? "퇴근 완료" : "근무중"}
                </span>
              </div>
              {/* 잔여 연차 */}
              {dashboard.remainingLeaveDays != null && (
                <div className={s.attendRow}>
                  <span className={s.attendName}>잔여 연차</span>
                  <span className={`${s.attendBadge} ${s.attendPresent}`}>
                    {dashboard.remainingLeaveDays}일
                  </span>
                </div>
              )}
            </div>
          </WSCard>

          {/* ─ 최근 업무 — GET /api/tasks/my ─ */}
          <WSCard title="최근 업무" subtitle={`현재 스프린트 — ${sprintMonth}`}>
            {tasks.length === 0 ? (
              <div className={s.emptyBlock}>
                <div className={`${s.emptyIconWrap} ${s.emptyIconWrapPosts}`}>
                  <CheckSquare size={24} className={s.emptyIconPosts} />
                </div>
                <p className={s.emptyTitle}>배정된 업무가 없습니다</p>
                <p className={s.emptySub}>새 업무가 배정되면 여기에 표시됩니다.</p>
              </div>
            ) : (
              <div className={s.approvalList}>
                {tasks.slice(0, 5).map((task) => {
                  const statusColor =
                    TASK_STATUS_COLOR[task.status] ?? TASK_STATUS_COLOR.TODO;
                  return (
                    <div key={task.id} className={s.approvalItem}>
                      <div className={s.approvalBody} style={{ marginTop: 0 }}>
                        <p className={s.approvalTitle}>{task.title}</p>
                        <p className={s.approvalMeta}>
                          {/* 업무 상태 뱃지 — CSS 클래스 없어 inline style 사용 */}
                          <span
                            style={{
                              background: statusColor.bg,
                              color: statusColor.text,
                              padding: "2px 8px",
                              borderRadius: 999,
                              fontSize: 10.5,
                              fontWeight: 600,
                            }}
                          >
                            {TASK_STATUS_LABEL[task.status] ?? task.status}
                          </span>
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </WSCard>
        </div>
      </div>
    </div>
  );
}