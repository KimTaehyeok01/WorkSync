// 감사 로그 페이지 — 요약, 필터, 표, 상세 패널로 누가 언제 무엇을 했는지 추적한다
import { useState, useEffect } from "react";
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
  Activity,
  Clock,
  Download,
  X,
  ChevronRight,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSAvatar, WSButton, WSPagination } from "../../../components/common/CommonWidgets";
import { WEEKDAYS } from "../../../constants/statusLabels";
import { toDateString, downloadCsv } from "../../../utils/dateFormatter";
import { getAuditLogs, getAuditSummary } from "../services/auditApi";
import s from "./AuditLogPage.module.css";

// 분류 탭 (화면 키 <-> 백엔드 targetType)
const CATEGORIES = [
  { key: "all", label: "전체", target: null },
  { key: "auth", label: "로그인", target: "AUTH" },
  { key: "hr", label: "인사", target: "HR" },
  { key: "approval", label: "전자결재", target: "APPROVAL" },
  { key: "task", label: "업무", target: "TASK" },
];

const CATEGORY_BY_TARGET = Object.fromEntries(
  CATEGORIES.filter((c) => c.target).map((c) => [c.target, c]),
);

const PERIODS = [
  { key: "all", label: "전체 기간" },
  { key: "today", label: "오늘" },
  { key: "week", label: "이번 주" },
  { key: "month", label: "이번 달" },
];

const PAGE_SIZES = [10, 20, 50];
const SEARCH_DELAY = 300;

const RESULT_LABEL = { success: "성공", failed: "실패", warning: "주의" };

const ACTION_ICONS = {
  로그인: LogIn,
  "로그인 실패": AlertTriangle,
  로그아웃: LogOut,
  "비밀번호 변경": Key,
  "입사 등록": UserPlus,
  "퇴사 처리": UserMinus,
  "인사 정보 변경": UserCheck,
  "근태 수정": Clock,
  "결재 요청": FileText,
  "결재 승인": FileCheck,
  "결재 반려": FileX,
  "업무 생성": Plus,
  "업무 완료": CheckSquare,
  "업무 수정": Pencil,
  "업무 삭제": Trash2,
  "담당자 변경": UserCheck,
};

// 행위명으로 결과 유추 — 실패는 빨강, 반려·삭제·퇴사는 주의
function resolveResult(action = "") {
  if (action.includes("실패")) return "failed";
  if (action.includes("반려") || action.includes("삭제") || action.includes("퇴사")) return "warning";
  return "success";
}

// userAgent -> "브라우저 · OS"
function parseDevice(ua) {
  if (!ua) return "-";
  const browser = ua.includes("Edg")
    ? "Edge"
    : ua.includes("Chrome")
      ? "Chrome"
      : ua.includes("Firefox")
        ? "Firefox"
        : ua.includes("Safari")
          ? "Safari"
          : "기타";
  const os = ua.includes("Windows")
    ? "Windows"
    : ua.includes("iPhone") || ua.includes("iPad")
      ? "iOS"
      : ua.includes("Mac")
        ? "macOS"
        : ua.includes("Android")
          ? "Android"
          : ua.includes("Linux")
            ? "Linux"
            : "기타";
  return `${browser} · ${os}`;
}

// 날짜 구분줄 라벨 (오늘 / 어제 / M월 D일 (요일))
function dateLabel(date) {
  const today = new Date();
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  const weekday = WEEKDAYS[new Date(`${date}T00:00:00`).getDay()];
  const base = `${Number(date.slice(5, 7))}월 ${Number(date.slice(8))}일 (${weekday})`;
  if (date === toDateString(today)) return `오늘 · ${base}`;
  if (date === toDateString(yesterday)) return `어제 · ${base}`;
  return base;
}

// 백엔드 응답 -> 화면용 항목
function toEntry(log) {
  const createdAt = log.createdAt ?? "";
  return {
    id: log.id,
    date: createdAt.slice(0, 10),
    time: createdAt.slice(11, 19),
    category: CATEGORY_BY_TARGET[log.targetType] ?? null,
    action: log.action,
    actorId: log.actorId,
    actorName: log.actorName || "알 수 없음",
    targetId: log.targetId,
    ip: log.clientIp || "-",
    device: parseDevice(log.userAgent),
    userAgent: log.userAgent || "-",
    result: resolveResult(log.action),
  };
}

export default function AuditLog() {
  const { accessToken } = useAuthContext();

  const [category, setCategory] = useState("all");
  const [period, setPeriod] = useState("all");
  const [searchInput, setSearchInput] = useState("");
  // 입력이 멈춘 뒤에만 서버로 보내는 검색어
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(PAGE_SIZES[1]);

  const [logs, setLogs] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [summary, setSummary] = useState(null);
  const [counts, setCounts] = useState({});
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);

  const targetOf = (key) => CATEGORIES.find((c) => c.key === key)?.target ?? null;

  // 검색어는 입력이 멈춘 뒤 한 번만 반영 — 글자마다 조회하지 않는다
  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(searchInput.trim());
      setPage(1);
    }, SEARCH_DELAY);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // 목록 조회
  useEffect(() => {
    if (!accessToken) return;
    let alive = true;
    setLoading(true);
    getAuditLogs(
      {
        category: targetOf(category),
        period,
        keyword: keyword || null,
        page: page - 1,
        size: pageSize,
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
  }, [accessToken, category, period, keyword, page, pageSize]);

  // 상단 요약 — 화면에 들어올 때 한 번
  useEffect(() => {
    if (!accessToken) return;
    getAuditSummary(accessToken).then((data) => setSummary(data));
  }, [accessToken]);

  // 분류별 건수 — 기간·검색어가 바뀔 때만 다시 센다
  useEffect(() => {
    if (!accessToken) return;
    Promise.all(
      CATEGORIES.map((c) =>
        getAuditLogs(
          { category: c.target, period, keyword: keyword || null, page: 0, size: 1 },
          accessToken,
        ).then((data) => [c.key, data?.totalElements ?? 0]),
      ),
    ).then((pairs) => setCounts(Object.fromEntries(pairs)));
  }, [accessToken, period, keyword]);

  const entries = logs.map(toEntry);

  const changeFilter = (setter) => (value) => {
    setter(value);
    setPage(1);
    setSelected(null);
  };

  // 로그인 실패만 빠르게 보기
  const showLoginFailures = () => {
    setCategory("auth");
    setSearchInput("로그인 실패");
    setSelected(null);
  };

  const resetFilters = () => {
    setCategory("all");
    setPeriod("all");
    setSearchInput("");
    setSelected(null);
  };

  const exportPage = () =>
    downloadCsv(`감사로그_${toDateString(new Date())}.csv`, [
      ["일시", "분류", "행위", "결과", "사용자", "IP", "기기"],
      ...entries.map((e) => [
        `${e.date} ${e.time}`,
        e.category?.label ?? "",
        e.action,
        RESULT_LABEL[e.result],
        e.actorName,
        e.ip,
        e.device,
      ]),
    ]);

  const isFiltered = category !== "all" || period !== "all" || searchInput !== "";

  const stats = [
    { label: "전체 로그", value: summary?.totalCount, icon: Activity, tone: s.toneBlue },
    { label: "오늘 활동", value: summary?.todayCount, icon: Clock, tone: s.toneGreen },
    {
      label: "로그인 실패",
      value: summary?.loginFailCount,
      icon: AlertTriangle,
      tone: s.toneRed,
      onClick: showLoginFailures,
      hint: "눌러서 모아 보기",
    },
    { label: "결재 처리", value: summary?.approvalCount, icon: FileCheck, tone: s.tonePurple },
  ];

  return (
    <div className={s.root}>
      {/* ── 요약 ── */}
      <div className={s.stats}>
        {stats.map(({ label, value, icon: Icon, tone, onClick, hint }) => {
          const Tag = onClick ? "button" : "div";
          return (
            <Tag
              key={label}
              type={onClick ? "button" : undefined}
              className={`${s.stat} ${onClick ? s.statClickable : ""}`}
              onClick={onClick}
            >
              <span className={`${s.statIcon} ${tone}`}>
                <Icon size={18} />
              </span>
              <span className={s.statBody}>
                <span className={s.statLabel}>{label}</span>
                <span className={s.statValue}>{value ?? "-"}</span>
              </span>
              {hint && <span className={s.statHint}>{hint}</span>}
            </Tag>
          );
        })}
      </div>

      <div className={s.panel}>
        {/* ── 필터 ── */}
        <div className={s.tabs}>
          {CATEGORIES.map((c) => (
            <button
              key={c.key}
              type="button"
              className={`${s.tab} ${category === c.key ? s.tabOn : ""}`}
              onClick={() => changeFilter(setCategory)(c.key)}
            >
              {c.label}
              <span className={s.tabCount}>{counts[c.key] ?? 0}</span>
            </button>
          ))}
        </div>

        <div className={s.toolbar}>
          <div className={s.search}>
            <Search size={14} className={s.searchIcon} />
            <input
              type="text"
              placeholder="행위 또는 사용자 이름으로 검색"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className={s.searchInput}
              aria-label="감사 로그 검색"
            />
            {searchInput && (
              <button
                type="button"
                className={s.searchClear}
                onClick={() => setSearchInput("")}
                aria-label="검색어 지우기"
              >
                <X size={13} />
              </button>
            )}
          </div>

          <div className={s.segment}>
            {PERIODS.map((p) => (
              <button
                key={p.key}
                type="button"
                className={`${s.segmentBtn} ${period === p.key ? s.segmentBtnOn : ""}`}
                onClick={() => changeFilter(setPeriod)(p.key)}
              >
                {p.label}
              </button>
            ))}
          </div>

          <div className={s.toolbarRight}>
            {isFiltered && (
              <button type="button" className={s.reset} onClick={resetFilters}>
                필터 초기화
              </button>
            )}
            <span className={s.total}>
              <strong>{totalElements.toLocaleString()}</strong>건
            </span>
            <select
              className={s.sizeSelect}
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setPage(1);
              }}
              aria-label="페이지당 표시 건수"
            >
              {PAGE_SIZES.map((size) => (
                <option key={size} value={size}>
                  {size}건씩
                </option>
              ))}
            </select>
            <WSButton
              label="CSV"
              size="sm"
              variant="secondary"
              icon={<Download size={13} />}
              disabled={entries.length === 0}
              onClick={exportPage}
            />
          </div>
        </div>

        {/* ── 표 + 상세 ── */}
        <div className={`${s.body} ${selected ? s.bodyWithDetail : ""}`}>
          <div className={s.tableWrap}>
            <div className={`${s.row} ${s.head}`}>
              <span>시각</span>
              <span>분류</span>
              <span>행위</span>
              <span>사용자</span>
              <span>IP</span>
              <span>기기</span>
              <span />
            </div>

            {loading && entries.length === 0 ? (
              <p className={s.empty}>로그를 불러오는 중입니다.</p>
            ) : entries.length === 0 ? (
              <div className={s.empty}>
                <Activity size={32} />
                <p>조건에 맞는 로그가 없습니다.</p>
                {isFiltered && (
                  <button type="button" className={s.reset} onClick={resetFilters}>
                    필터 초기화
                  </button>
                )}
              </div>
            ) : (
              entries.map((entry, idx) => {
                const Icon = ACTION_ICONS[entry.action] ?? Activity;
                const isNewDate = idx === 0 || entries[idx - 1].date !== entry.date;
                return (
                  <div key={entry.id}>
                    {isNewDate && <div className={s.dateRow}>{dateLabel(entry.date)}</div>}
                    <button
                      type="button"
                      className={`${s.row} ${s.dataRow} ${s[`row_${entry.result}`]} ${selected?.id === entry.id ? s.rowSelected : ""}`}
                      onClick={() => setSelected(selected?.id === entry.id ? null : entry)}
                    >
                      <span className={s.time}>{entry.time}</span>
                      <span>
                        <span className={`${s.badge} ${s[`cat_${entry.category?.key}`] ?? ""}`}>
                          {entry.category?.label ?? "기타"}
                        </span>
                      </span>
                      <span className={s.action}>
                        <span className={`${s.actionIcon} ${s[`result_${entry.result}`]}`}>
                          <Icon size={14} />
                        </span>
                        {entry.action}
                      </span>
                      <span className={s.user}>
                        <WSAvatar name={entry.actorName} size={24} />
                        {entry.actorName}
                      </span>
                      <span className={s.mono}>{entry.ip}</span>
                      <span className={s.device}>{entry.device}</span>
                      <ChevronRight size={14} className={s.chevron} />
                    </button>
                  </div>
                );
              })
            )}
          </div>

          {selected && (
            <aside className={s.detail}>
              <div className={s.detailHeader}>
                <span className={`${s.actionIcon} ${s[`result_${selected.result}`]}`}>
                  {(() => {
                    const Icon = ACTION_ICONS[selected.action] ?? Activity;
                    return <Icon size={16} />;
                  })()}
                </span>
                <h3 className={s.detailTitle}>{selected.action}</h3>
                <button
                  type="button"
                  className={s.detailClose}
                  onClick={() => setSelected(null)}
                  aria-label="상세 닫기"
                >
                  <X size={16} />
                </button>
              </div>
              <dl className={s.detailList}>
                {[
                  ["결과", RESULT_LABEL[selected.result]],
                  ["일시", `${selected.date} ${selected.time}`],
                  ["분류", selected.category?.label ?? "기타"],
                  ["사용자", `${selected.actorName}${selected.actorId ? ` (ID ${selected.actorId})` : ""}`],
                  ["대상 ID", selected.targetId ?? "-"],
                  ["IP 주소", selected.ip],
                  ["기기", selected.device],
                  ["User-Agent", selected.userAgent],
                ].map(([label, value]) => (
                  <div key={label} className={s.detailRow}>
                    <dt>{label}</dt>
                    <dd>{value}</dd>
                  </div>
                ))}
              </dl>
              <WSButton
                label="이 사용자의 로그만 보기"
                size="sm"
                variant="secondary"
                onClick={() => setSearchInput(selected.actorName)}
              />
            </aside>
          )}
        </div>

        {totalElements > pageSize && (
          <div className={s.pagination}>
            <WSPagination
              total={totalElements}
              page={page}
              perPage={pageSize}
              onPageChange={(next) => {
                setPage(next);
                setSelected(null);
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
}
