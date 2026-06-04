import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  FileCheck,
  CheckSquare,
  MessageSquare,
  ThumbsUp,
  MessageCircle,
  ArrowRight,
  CalendarDays,
  Users,
  Newspaper,
  CheckCircle2,
  Plus,
} from "lucide-react";
import {
  TEAM_MEMBERS,
  KANBAN_TASKS,
  APPROVAL_DOCS,
  BOARD_POSTS,
} from "../../../constants/mockData";
import {
  WSCard,
  WSStatCard,
  WSAvatar,
  WSButton,
} from "../../../components/common/CommonWidgets";
import s from "./DashboardPage.module.css";

const ATTENDANCE = [
  {
    member: TEAM_MEMBERS[0],
    checkIn: "08:45 AM",
    checkOut: null,
    status: "present",
  },
  {
    member: TEAM_MEMBERS[1],
    checkIn: "09:02 AM",
    checkOut: null,
    status: "present",
  },
  {
    member: TEAM_MEMBERS[2],
    checkIn: "09:15 AM",
    checkOut: null,
    status: "present",
  },
  {
    member: TEAM_MEMBERS[3],
    checkIn: "08:30 AM",
    checkOut: null,
    status: "present",
  },
  { member: TEAM_MEMBERS[4], checkIn: null, checkOut: null, status: "absent" },
];

const TASK_OVERVIEW = [
  { label: "준비중", count: KANBAN_TASKS.todo.length, color: "#6B7280" },
  { label: "진행 중", count: KANBAN_TASKS.inProgress.length, color: "#1A73E8" },
  { label: "완료", count: KANBAN_TASKS.done.length, color: "#10B981" },
];

export default function Dashboard() {
  const navigate = useNavigate();
  const [emptyPosts, setEmptyPosts] = useState(false);
  const [emptyApprovals, setEmptyApprovals] = useState(false);

  const totalTasks = Object.values(KANBAN_TASKS).flat().length;
  const doneTasks = KANBAN_TASKS.done.length;

  return (
    <div className={s.root}>
      <div className={s.banner}>
        <div>
          <h2 className={s.bannerTitle}>안녕하세요, Marcus님! 👋</h2>
          <p className={s.bannerSub}>
            오늘 <strong>결재 대기 3건</strong>과 <strong>새 메시지 5건</strong>
            이 있습니다.
          </p>
        </div>
        <div className={s.bannerRight}>
          <div>
            <div className={s.bannerDateLabel}>오늘</div>
            <div className={s.bannerDate}>2024년 7월 11일 월요일</div>
          </div>
          <div className={s.bannerCalIcon}>
            <CalendarDays size={22} />
          </div>
        </div>
      </div>

      <div className={s.statsGrid}>
        <WSStatCard
          label="결재 대기"
          value="3"
          icon={<FileCheck size={22} />}
          color="#F59E0B"
        />
        <WSStatCard
          label="오늘 출근 현황"
          value="4/5"
          icon={<Users size={22} />}
          color="#10B981"
        />
        <WSStatCard
          label="업무 완료율"
          value={`${doneTasks}/${totalTasks}`}
          icon={<CheckSquare size={22} />}
          color="#1A73E8"
        />
        <WSStatCard
          label="읽지 않은 메시지"
          value="5"
          icon={<MessageSquare size={22} />}
          color="#8B5CF6"
        />
      </div>

      <div className={s.mainGrid}>
        <div className={`${s.mainCol} ${s.mainColLeft}`}>
          <WSCard
            title="최근 게시물"
            subtitle="전사 공지 및 업데이트"
            action={
              <div className={s.headerActions}>
                <button
                  onClick={() => setEmptyPosts(!emptyPosts)}
                  className={`${s.toggleBtn} ${emptyPosts ? s.toggleBtnOn : s.toggleBtnOff}`}
                  title="빈 화면 미리보기"
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
            {emptyPosts ? (
              <div className={s.emptyBlock}>
                <div className={`${s.emptyIconWrap} ${s.emptyIconWrapPosts}`}>
                  <Newspaper size={26} className={s.emptyIconPosts} />
                </div>
                <div>
                  <p className={s.emptyTitle}>아직 게시물이 없습니다</p>
                  <p className={s.emptySub}>
                    팀 소식, 공지사항, 자유로운 이야기를
                    <br />첫 번째로 올려보세요.
                  </p>
                </div>
                <button
                  onClick={() => navigate("/board/new")}
                  className={s.emptyAction}
                >
                  <Plus size={13} />첫 게시글 작성하기
                </button>
              </div>
            ) : (
              <div className={s.postList}>
                {BOARD_POSTS.slice(0, 4).map((post) => {
                  const isNotice = post.category === "notice";
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
                          <span style={{ marginRight: 6 }}>
                            <WSAvatar src={post.author.avatar} size={21} />
                          </span>
                          <span style={{ marginRight: 10 }}>
                            {post.author.name}
                          </span>
                          ·{" "}
                          <span style={{ marginLeft: 10 }}>
                            {post.createdAt}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </WSCard>

          <WSCard
            title="결재 대기 문서"
            subtitle="처리가 필요한 문서"
            action={
              <div className={s.headerActions}>
                <button
                  onClick={() => setEmptyApprovals(!emptyApprovals)}
                  className={`${s.toggleBtn} ${emptyApprovals ? s.toggleBtnOn : s.toggleBtnOff}`}
                  title="빈 화면 미리보기"
                >
                  {emptyApprovals ? "목록 보기" : "빈 화면"}
                </button>
                <WSButton
                  label="전체 보기"
                  variant="secondary"
                  size="sm"
                  icon={<ArrowRight size={12} />}
                />
              </div>
            }
          >
            {emptyApprovals ? (
              <div className={s.emptyBlock}>
                <div className={s.successIconStack}>
                  <div
                    className={`${s.emptyIconWrap} ${s.emptyIconWrapApprovals}`}
                  >
                    <FileCheck size={26} className={s.emptyIconApprovals} />
                  </div>
                  <span className={s.successDoneBadge}>
                    <CheckCircle2 size={13} color="#fff" />
                  </span>
                </div>
                <div>
                  <p className={s.emptyTitle}>모든 결재가 완료되었습니다</p>
                  <p className={s.emptySub}>
                    현재 처리 대기 중인 문서가 없습니다.
                    <br />새 결재 요청이 오면 여기에 표시됩니다.
                  </p>
                </div>
                <div className={s.successBadgeRow}>
                  <CheckCircle2 size={14} className={s.successInlineIcon} />
                  <span className={s.successBadgeStrong}>
                    오늘 결재 처리 완료 ·
                  </span>
                  <span className={s.successBadgeSub}>수고하셨습니다!</span>
                </div>
              </div>
            ) : (
              <div className={s.approvalList}>
                {APPROVAL_DOCS.filter((d) => d.status === "pending").map(
                  (doc) => (
                    <div key={doc.id} className={s.approvalItem}>
                      <div className={s.approvalIconBox}>
                        <FileCheck size={16} className={s.approvalIcon} />
                      </div>
                      <div className={s.approvalBody}>
                        <p className={s.approvalTitle}>{doc.title}</p>
                        <p className={s.approvalMeta}>
                          {doc.requester.name} · {doc.date}
                        </p>
                      </div>
                    </div>
                  ),
                )}
              </div>
            )}
          </WSCard>
        </div>

        <div className={s.mainCol}>
          <WSCard title="나의 팀 현황" subtitle="오늘, 7월 11일">
            <div className={s.attendList}>
              {ATTENDANCE.map((a, i) => (
                <div key={i} className={s.attendRow}>
                  <WSAvatar
                    src={a.member.avatar}
                    name={a.member.name}
                    size={28}
                  />
                  <span className={s.attendName}>{a.member.name}</span>
                  <span
                    className={`${s.attendBadge} ${a.status === "present" ? s.attendPresent : s.attendAbsent}`}
                  >
                    {a.checkIn || "결근"}
                  </span>
                </div>
              ))}
            </div>
          </WSCard>

          <WSCard title="업무 진행률" subtitle="현재 스프린트 — 2024년 7월">
            <div className={s.taskProgressList}>
              {TASK_OVERVIEW.map((t) => (
                <div key={t.label}>
                  <div className={s.taskProgressTop}>
                    <span className={s.taskProgressLabel}>{t.label}</span>
                    <span
                      className={s.taskProgressValue}
                      style={{ "--progress-color": t.color }}
                    >
                      {t.count}
                    </span>
                  </div>
                  <div className={s.taskProgressTrack}>
                    <div
                      className={s.taskProgressFill}
                      style={{
                        "--progress-width": `${(t.count / totalTasks) * 100}%`,
                        "--progress-color": t.color,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </WSCard>
        </div>
      </div>
    </div>
  );
}
