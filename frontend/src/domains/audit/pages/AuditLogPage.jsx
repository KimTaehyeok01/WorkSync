import { useState, useEffect, useCallback } from "react";
import {
  LogIn,
  LogOut,
  AlertTriangle,
  Key,
  UserPlus,
  UserMinus,
  UserCheck,
  FileCheck,
  FileX,
  FileText,
  CheckSquare,
  Pencil,
  Trash2,
  Plus,
  Search,
  ChevronRight,
  Globe,
  Monitor,
  Activity,
  Clock,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import {
  WSCard,
  WSAvatar,
  WSPagination,
} from "../../../components/common/CommonWidgets";
import { getAuditLogs, getAuditSummary } from "../services/auditApi";
import s from "./AuditLogPage.module.css";

// 화면 카테고리 표시 설정
const CAT_CFG = {
  auth: { label: "로그인/로그아웃", color: "#10B981", bg: "#D1FAE5" },
  hr: { label: "입사/퇴사", color: "#8B5CF6", bg: "#EDE9FE" },
  approval: { label: "전자결재", color: "#1A73E8", bg: "#EBF4FF" },
  task: { label: "업무", color: "#F59E0B", bg: "#FEF3C7" },
};

const STATUS_CFG = {
  success: { label: "성공", color: "#10B981", bg: "#D1FAE5" },
  failed: { label: "실패", color: "#EF4444", bg: "#FEE2E2" },
  warning: { label: "경고", color: "#F59E0B", bg: "#FEF3C7" },
};

// 탭 (화면 키 <-> 백엔드 targetType)
const TABS = [
  { key: "all", label: "전체", target: null },
  { key: "auth", label: "로그인/로그아웃", target: "AUTH" },
  { key: "hr", label: "입사/퇴사", target: "HR" },
  { key: "approval", label: "전자결재", target: "APPROVAL" },
  { key: "task", label: "업무", target: "TASK" },
];

// 백엔드 targetType -> 화면 카테고리 키
const TARGET_TO_CAT = {
  AUTH: "auth",
  HR: "hr",
  APPROVAL: "approval",
  TASK: "task",
};

const DATE_FILTER_OPTS = [
  { key: "all", label: "전체" },
  { key: "today", label: "오늘" },
  { key: "week", label: "이번 주" },
  { key: "month", label: "이번 달" },
];

// action 기반 기본 상세 문구 (백엔드 detail 미제공 -> 프론트에서 보강)
const DETAIL_MAP = {
  로그인: "정상 로그인 처리",
  로그아웃: "정상 세션 종료",
  "로그인 실패": "로그인 시도 실패",
  "비밀번호 변경": "비밀번호 변경 처리",
  "입사 등록": "신규 구성원 시스템 등록",
  "퇴사 처리": "구성원 퇴사 처리",
  "인사 정보 변경": "인사 정보 변경 처리",
  "결재 요청": "전자결재 상신",
  "결재 승인": "전자결재 승인 처리",
  "결재 반려": "전자결재 반려 처리",
  "업무 생성": "신규 업무 등록",
  "업무 완료": "업무 완료 처리",
  "업무 수정": "업무 정보 수정",
  "업무 삭제": "업무 삭제 처리",
  "담당자 변경": "업무 담당자 재배정",
};

const PAGE_SIZE = 10;

// 액션명 -> 아이콘
function ActionIcon({ action, size = 15 }) {
  const p = { size };
  switch (action) {
    case "로그인":
      return <LogIn {...p} />;
    case "로그인 실패":
      return <AlertTriangle {...p} />;
    case "로그아웃":
      return <LogOut {...p} />;
    case "비밀번호 변경":
      return <Key {...p} />;
    case "입사 등록":
      return <UserPlus {...p} />;
    case "퇴사 처리":
      return <UserMinus {...p} />;
    case "인사 정보 변경":
      return <UserCheck {...p} />;
    case "결재 요청":
      return <FileText {...p} />;
    case "결재 승인":
      return <FileCheck {...p} />;
    case "결재 반려":
      return <FileX {...p} />;
    case "업무 생성":
      return <Plus {...p} />;
    case "업무 완료":
      return <CheckSquare {...p} />;
    case "업무 수정":
      return <Pencil {...p} />;
    case "업무 삭제":
      return <Trash2 {...p} />;
    case "담당자 변경":
      return <UserCheck {...p} />;
    default:
      return <Activity {...p} />;
  }
}

// 액션명 -> 성공/실패/경고 유추
function resolveStatus(action) {
  if (!action) return "success";
  if (action.includes("실패")) return "failed";
  if (action.includes("반려") || action.includes("삭제")) return "warning";
  return "success";
}

// userAgent -> "브라우저 / OS"
function parseDevice(ua) {
  if (!ua) return "Unknown / Unknown";
  let browser = "Unknown";
  if (ua.includes("Edg")) browser = "Edge";
  else if (ua.includes("Chrome")) browser = "Chrome";
  else if (ua.includes("Firefox")) browser = "Firefox";
  else if (ua.includes("Safari")) browser = "Safari";

  let os = "Unknown";
  if (ua.includes("Windows")) os = "Windows";
  else if (ua.includes("Mac")) os = "macOS";
  else if (ua.includes("iPhone") || ua.includes("iPad")) os = "iOS";
  else if (ua.includes("Android")) os = "Android";
  else if (ua.includes("Linux")) os = "Linux";

  return `${browser} / ${os}`;
}

// ISO 일시 -> { date, time }
function splitDateTime(iso) {
  if (!iso) return { date: "", time: "" };
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, "0");
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return { date, time };
}

// 날짜 구분선 라벨 (오늘 / 어제 / N월 N일)
function dateLabel(dateStr) {
  const today = new Date();
  const yest = new Date();
  yest.setDate(today.getDate() - 1);
  const pad = (n) => String(n).padStart(2, "0");
  const fmt = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

  const [, m, dd] = dateStr.split("-");
  const base = `${parseInt(m)}월 ${parseInt(dd)}일`;
  if (dateStr === fmt(today)) return `${base} · 오늘`;
  if (dateStr === fmt(yest)) return `${base} · 어제`;
  return base;
}

// 백엔드 응답 -> 화면용 항목으로 변환
function toEntry(log) {
  const { date, time } = splitDateTime(log.createdAt);
  const member = TEAM_MEMBERS.find((m) => m.name === log.actorName);
  return {
    id: log.id,
    date,
    time,
    cat: TARGET_TO_CAT[log.targetType] || "auth",
    action: log.action,
    name: log.actorName || "알 수 없음",
    role: member?.role || "",
    avatar: member?.avatar,
    detail: DETAIL_MAP[log.action] || log.action,
    ip: log.clientIp || "-",
    device: parseDevice(log.userAgent),
    status: resolveStatus(log.action),
  };
}

export default function AuditLog() {
  const { accessToken } = useAuthContext();

  const [catFilter, setCatFilter] = useState("all");
  const [dateFilter, setDateFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1); // UI는 1-base

  const [logs, setLogs] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [summary, setSummary] = useState({
    totalCount: 0,
    todayCount: 0,
    loginFailCount: 0,
    approvalCount: 0,
  });
  const [tabCounts, setTabCounts] = useState({});
  const [loading, setLoading] = useState(false);

  const targetOf = (key) => TABS.find((t) => t.key === key)?.target ?? null;

  // 목록 조회 (필터/페이지 변경 시마다 서버에서 재조회)
  useEffect(() => {
    if (!accessToken) return;
    let alive = true;
    setLoading(true);

    getAuditLogs(
      {
        category: targetOf(catFilter),
        period: dateFilter,
        keyword: search.trim() || null,
        page: page - 1, // 백엔드는 0-base
        size: PAGE_SIZE,
      },
      accessToken,
    ).then((data) => {
      if (!alive) return;
      setLogs(data?.content ?? []);
      setTotalElements(data?.totalElements ?? 0);
      setLoading(false);
    });

    return () => {
      alive = false;
    };
  }, [accessToken, catFilter, dateFilter, search, page]);

  // 상단 통계 (필터와 무관 — 마운트 시 + 기간 변경 시 갱신)
  useEffect(() => {
    if (!accessToken) return;
    getAuditSummary(accessToken).then((data) => {
      if (data) setSummary(data);
    });
  }, [accessToken, dateFilter, catFilter, page]);

  // 탭별 건수 (기간/검색 반영, 카테고리별 totalElements 병렬 조회)
  const loadTabCounts = useCallback(() => {
    if (!accessToken) return;
    Promise.all(
      TABS.map((tab) =>
        getAuditLogs(
          {
            category: tab.target,
            period: dateFilter,
            keyword: search.trim() || null,
            page: 0,
            size: 1,
          },
          accessToken,
        ).then((data) => [tab.key, data?.totalElements ?? 0]),
      ),
    ).then((pairs) => setTabCounts(Object.fromEntries(pairs)));
  }, [accessToken, dateFilter, search]);

  useEffect(() => {
    loadTabCounts();
  }, [loadTabCounts]);

  const entries = logs.map(toEntry);
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));

  const stats = [
    {
      icon: <Activity size={20} />,
      label: "전체 로그",
      value: summary.totalCount,
      sub: "전체 기간",
      color: "#1A73E8",
      bg: "#EBF4FF",
    },
    {
      icon: <Clock size={20} />,
      label: "오늘 활동",
      value: summary.todayCount,
      sub: "오늘",
      color: "#10B981",
      bg: "#D1FAE5",
    },
    {
      icon: <AlertTriangle size={20} />,
      label: "로그인 실패",
      value: summary.loginFailCount,
      sub: "주의 필요",
      color: "#EF4444",
      bg: "#FEE2E2",
    },
    {
      icon: <FileCheck size={20} />,
      label: "결재 처리",
      value: summary.approvalCount,
      sub: "전자결재 이벤트",
      color: "#8B5CF6",
      bg: "#EDE9FE",
    },
  ];

  return (
    <div>
      <div className={s.header}>
        <div>
          <div className={s.breadcrumb}>
            <span>시스템</span>
            <ChevronRight size={12} />
            <span className={s.bcCurrent}>감사 로그</span>
          </div>
          <h1 className={s.pageTitle}>감사 로그</h1>
        </div>
      </div>

      <div className={s.statsGrid}>
        {stats.map((st) => (
          <div key={st.label} className={s.statCard}>
            <div
              className={s.statIcon}
              style={{ "--stat-bg": st.bg, "--stat-color": st.color }}
            >
              {st.icon}
            </div>
            <div>
              <p className={s.statValue}>{st.value}</p>
              <p className={s.statLabel}>{st.label}</p>
              <p className={s.statSub}>{st.sub}</p>
            </div>
          </div>
        ))}
      </div>

      <div className={s.filterBar}>
        <div className={s.tabGroup}>
          {TABS.map((tab) => {
            const isActive = catFilter === tab.key;
            const color =
              tab.key === "all"
                ? "#1A73E8"
                : CAT_CFG[tab.key]?.color || "#1A73E8";
            const bg =
              tab.key === "all" ? "#EBF4FF" : CAT_CFG[tab.key]?.bg || "#EBF4FF";
            return (
              <button
                key={tab.key}
                onClick={() => {
                  setCatFilter(tab.key);
                  setPage(1);
                }}
                className={`${s.tabBtn} ${isActive ? s.tabBtnActive : ""}`}
                style={isActive ? { color } : undefined}
              >
                {tab.label}
                <span
                  className={s.tabCount}
                  style={isActive ? { background: bg, color } : undefined}
                >
                  {tabCounts[tab.key] ?? 0}
                </span>
              </button>
            );
          })}
        </div>

        <div className={s.dateGroup}>
          {DATE_FILTER_OPTS.map((opt) => (
            <button
              key={opt.key}
              onClick={() => {
                setDateFilter(opt.key);
                setPage(1);
              }}
              className={`${s.dateBtn} ${dateFilter === opt.key ? s.dateBtnActive : ""}`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className={s.search}>
          <Search size={14} className={s.searchIcon} />
          <input
            type="text"
            placeholder="액션, 상세내용, 사용자 검색..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            className={s.searchInput}
          />
        </div>

        <span className={s.resultCount}>
          <strong>{totalElements}</strong>건 조회됨
        </span>
      </div>

      <WSCard>
        {loading ? (
          <div className={s.empty}>
            <Activity size={40} className={s.emptyIcon} />
            <p className={s.emptyTitle}>로그를 불러오는 중...</p>
          </div>
        ) : entries.length === 0 ? (
          <div className={s.empty}>
            <Activity size={40} className={s.emptyIcon} />
            <p className={s.emptyTitle}>조회된 로그가 없습니다</p>
            <p className={s.emptyDesc}>검색 조건을 변경해 보세요.</p>
          </div>
        ) : (
          <div>
            {entries.map((entry, idx) => {
              const cat = CAT_CFG[entry.cat];
              const st = STATUS_CFG[entry.status];
              const prevEntry = entries[idx - 1];
              const showDateHeader =
                idx === 0 || prevEntry?.date !== entry.date;

              return (
                <div key={entry.id}>
                  {showDateHeader && (
                    <div
                      className={`${s.dateDivider} ${idx !== 0 ? s.dateDividerTop : ""}`}
                    >
                      <span className={s.dateDividerLabel}>
                        {dateLabel(entry.date)}
                      </span>
                      <span className={s.dateDividerCount}>
                        {entries.filter((l) => l.date === entry.date).length}건
                      </span>
                    </div>
                  )}

                  <div
                    className={s.logRow}
                    style={{ "--log-color": cat.color }}
                  >
                    <div
                      className={s.catIcon}
                      style={{ "--cat-bg": cat.bg, "--cat-color": cat.color }}
                    >
                      <ActionIcon action={entry.action} size={15} />
                    </div>

                    <div className={s.userCell}>
                      <WSAvatar
                        src={entry.avatar}
                        name={entry.name}
                        size={30}
                      />
                      <div className={s.userBody}>
                        <p className={s.userName}>{entry.name}</p>
                        <p className={s.userRole}>{entry.role}</p>
                      </div>
                    </div>

                    <div className={s.actionCell}>
                      <div className={s.actionRow}>
                        <span
                          className={s.actionBadge}
                          style={{
                            "--cat-bg": cat.bg,
                            "--cat-color": cat.color,
                          }}
                        >
                          {entry.action}
                        </span>
                        <span className={s.catBadge}>{cat.label}</span>
                      </div>
                      <p className={s.actionDetail}>{entry.detail}</p>
                    </div>

                    <div className={s.meta}>
                      <div className={s.metaLine}>
                        <Globe size={11} className={s.metaIcon} />
                        <span className={s.metaText}>{entry.ip}</span>
                      </div>
                      <div className={`${s.metaLine} ${s.metaLineSub}`}>
                        <Monitor size={11} className={s.metaIcon} />
                        <span className={s.metaText}>{entry.device}</span>
                      </div>
                    </div>

                    <div className={s.timeCell}>
                      <p className={s.timeValue}>{entry.time}</p>
                      <p className={s.timeDate}>
                        {entry.date.slice(5).replace("-", "/")}
                      </p>
                    </div>

                    <div className={s.statusCell}>
                      <span
                        className={s.statusBadge}
                        style={{
                          "--status-bg": st.bg,
                          "--status-color": st.color,
                        }}
                      >
                        {st.label}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </WSCard>

      {totalPages > 1 && (
        <div className={s.pagination}>
          <WSPagination
            total={totalElements}
            page={page}
            perPage={PAGE_SIZE}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
}
