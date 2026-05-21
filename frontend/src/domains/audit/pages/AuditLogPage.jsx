import { useState } from "react";
import {
  LogIn, LogOut, AlertTriangle, Key, UserPlus, UserMinus, UserCheck,
  FileCheck, FileX, FileText, CheckSquare, Pencil, Trash2, Plus,
  Search, Download, ChevronRight, Globe, Monitor, Activity, Clock,
} from "lucide-react";
import { TEAM_MEMBERS } from "../../../constants/mockData";
import { WSCard, WSAvatar, WSPagination } from "../../../components/common/CommonWidgets";
import s from "./AuditLogPage.module.css";

const CAT_CFG = {
  auth:     { label: "로그인/로그아웃", color: "#10B981", bg: "#D1FAE5" },
  hr:       { label: "입사/퇴사",       color: "#8B5CF6", bg: "#EDE9FE" },
  approval: { label: "전자결재",        color: "#1A73E8", bg: "#EBF4FF" },
  task:     { label: "업무",            color: "#F59E0B", bg: "#FEF3C7" },
};

const STATUS_CFG = {
  success: { label: "성공", color: "#10B981", bg: "#D1FAE5" },
  failed:  { label: "실패", color: "#EF4444", bg: "#FEE2E2" },
  warning: { label: "경고", color: "#F59E0B", bg: "#FEF3C7" },
};

const DATE_FILTER_OPTS = [
  { key: "all",   label: "전체" },
  { key: "today", label: "오늘" },
  { key: "week",  label: "이번 주" },
  { key: "month", label: "7월" },
];

const LOGS = [
  { id:"AL-001", date:"2024-07-12", time:"09:02", cat:"auth",     action:"로그인",         userId:4, detail:"정상 로그인 처리",                                  ip:"192.168.1.45",  device:"Chrome / macOS",    status:"success" },
  { id:"AL-002", date:"2024-07-12", time:"09:01", cat:"auth",     action:"로그인 실패",    userId:2, detail:"비밀번호 오류 3회 연속 시도",                       ip:"192.168.1.30",  device:"Safari / iOS",      status:"failed"  },
  { id:"AL-003", date:"2024-07-12", time:"08:55", cat:"auth",     action:"로그아웃",       userId:3, detail:"정상 세션 종료",                                    ip:"192.168.1.22",  device:"Firefox / Windows", status:"success" },
  { id:"AL-004", date:"2024-07-12", time:"08:40", cat:"approval", action:"결재 승인",      userId:1, detail:"APV-2024-0890 재택근무 정책 개정안 승인",           ip:"10.0.0.5",      device:"Chrome / macOS",    status:"success" },
  { id:"AL-005", date:"2024-07-12", time:"08:32", cat:"task",     action:"업무 생성",      userId:3, detail:"T-104 API 엔드포인트 리팩토링 등록",               ip:"192.168.1.22",  device:"Chrome / macOS",    status:"success" },
  { id:"AL-006", date:"2024-07-12", time:"08:15", cat:"auth",     action:"로그인",         userId:1, detail:"정상 로그인 처리",                                  ip:"10.0.0.5",      device:"Safari / macOS",    status:"success" },
  { id:"AL-007", date:"2024-07-11", time:"18:30", cat:"auth",     action:"로그아웃",       userId:4, detail:"정상 세션 종료",                                    ip:"192.168.1.45",  device:"Chrome / macOS",    status:"success" },
  { id:"AL-008", date:"2024-07-11", time:"17:45", cat:"task",     action:"업무 완료",      userId:4, detail:"T-095 메신저 읽음 확인 기능 완료 처리",            ip:"192.168.1.45",  device:"Chrome / macOS",    status:"success" },
  { id:"AL-009", date:"2024-07-11", time:"16:20", cat:"approval", action:"결재 요청",      userId:3, detail:"APV-2024-0889 클라우드 인프라 업그레이드 결재 요청", ip:"192.168.1.22", device:"Chrome / macOS",    status:"success" },
  { id:"AL-010", date:"2024-07-11", time:"15:10", cat:"task",     action:"담당자 변경",    userId:2, detail:"T-099 담당자 → Marcus Lee로 재배정",               ip:"192.168.1.30",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-011", date:"2024-07-11", time:"14:55", cat:"hr",       action:"인사 정보 변경", userId:1, detail:"Aisha Park 직급 변경: 개발자 → 시니어 개발자",     ip:"10.0.0.5",      device:"Safari / macOS",    status:"success" },
  { id:"AL-012", date:"2024-07-11", time:"12:00", cat:"auth",     action:"비밀번호 변경",  userId:5, detail:"보안 정책 60일 주기 강제 변경 완료",               ip:"192.168.1.60",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-013", date:"2024-07-11", time:"09:15", cat:"auth",     action:"로그인",         userId:5, detail:"정상 로그인 처리",                                  ip:"192.168.1.60",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-014", date:"2024-07-10", time:"17:00", cat:"approval", action:"결재 반려",      userId:1, detail:"APV-2024-0888 디자인 시스템 라이선스 — 예산 초과 사유", ip:"10.0.0.5",  device:"Safari / macOS",    status:"warning" },
  { id:"AL-015", date:"2024-07-10", time:"16:30", cat:"task",     action:"업무 수정",      userId:4, detail:"T-100 인증 토큰 버그 우선순위 변경: medium → urgent", ip:"192.168.1.45", device:"Chrome / macOS",   status:"success" },
  { id:"AL-016", date:"2024-07-10", time:"14:45", cat:"hr",       action:"입사 등록",      userId:1, detail:"신규 구성원 이민준 (개발팀 사원) 시스템 등록",      ip:"10.0.0.5",      device:"Chrome / macOS",    status:"success" },
  { id:"AL-017", date:"2024-07-10", time:"11:20", cat:"approval", action:"결재 요청",      userId:2, detail:"APV-2024-0891 3분기 마케팅 부서 예산 요청",        ip:"192.168.1.30",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-018", date:"2024-07-10", time:"10:05", cat:"task",     action:"업무 생성",      userId:2, detail:"T-101 WorkSync 2.0 온보딩 플로우 디자인 등록",     ip:"192.168.1.30",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-019", date:"2024-07-10", time:"09:00", cat:"auth",     action:"로그인",         userId:2, detail:"정상 로그인 처리",                                  ip:"192.168.1.30",  device:"Chrome / Windows",  status:"success" },
  { id:"AL-020", date:"2024-07-09", time:"18:00", cat:"hr",       action:"퇴사 처리",      userId:1, detail:"박지현 (디자인팀) 퇴사 처리 완료 — 최종 근무일 2024-07-09", ip:"10.0.0.5", device:"Chrome / macOS",   status:"success" },
  { id:"AL-021", date:"2024-07-09", time:"16:10", cat:"approval", action:"결재 승인",      userId:1, detail:"APV-2024-0887 팀 워크숍 기획 승인 완료",           ip:"10.0.0.5",      device:"Safari / macOS",    status:"success" },
  { id:"AL-022", date:"2024-07-09", time:"14:00", cat:"auth",     action:"로그인 실패",    userId:3, detail:"미인가 기기에서 외부 IP 접속 시도 탐지",           ip:"203.0.113.52",  device:"Unknown / Unknown", status:"failed"  },
  { id:"AL-023", date:"2024-07-09", time:"11:30", cat:"task",     action:"업무 삭제",      userId:3, detail:"T-088 중복 태스크 삭제 처리",                       ip:"192.168.1.22",  device:"Firefox / Windows", status:"warning" },
  { id:"AL-024", date:"2024-07-09", time:"09:45", cat:"hr",       action:"입사 등록",      userId:1, detail:"신규 구성원 김수빈 (제품팀 주임) 시스템 등록",     ip:"10.0.0.5",      device:"Chrome / macOS",    status:"success" },
  { id:"AL-025", date:"2024-07-09", time:"09:05", cat:"auth",     action:"로그인",         userId:3, detail:"정상 로그인 처리",                                  ip:"192.168.1.22",  device:"Firefox / Windows", status:"success" },
];

function dateLabel(d) {
  if (d === "2024-07-12") return "7월 12일 · 오늘";
  if (d === "2024-07-11") return "7월 11일 · 어제";
  const [, m, dd] = d.split("-");
  return `${parseInt(m)}월 ${parseInt(dd)}일`;
}

function ActionIcon({ action, size = 15 }) {
  const p = { size };
  switch (action) {
    case "로그인":          return <LogIn {...p} />;
    case "로그인 실패":     return <AlertTriangle {...p} />;
    case "로그아웃":        return <LogOut {...p} />;
    case "비밀번호 변경":   return <Key {...p} />;
    case "입사 등록":       return <UserPlus {...p} />;
    case "퇴사 처리":       return <UserMinus {...p} />;
    case "인사 정보 변경":  return <UserCheck {...p} />;
    case "결재 요청":       return <FileText {...p} />;
    case "결재 승인":       return <FileCheck {...p} />;
    case "결재 반려":       return <FileX {...p} />;
    case "업무 생성":       return <Plus {...p} />;
    case "업무 완료":       return <CheckSquare {...p} />;
    case "업무 수정":       return <Pencil {...p} />;
    case "업무 삭제":       return <Trash2 {...p} />;
    case "담당자 변경":     return <UserCheck {...p} />;
    default:                return <Activity {...p} />;
  }
}

export default function AuditLog() {
  const [catFilter, setCatFilter] = useState("all");
  const [dateFilter, setDateFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);

  const filtered = LOGS.filter((entry) => {
    const catOk = catFilter === "all" || entry.cat === catFilter;
    const user = TEAM_MEMBERS.find((m) => m.id === entry.userId);
    const searchOk =
      !search ||
      entry.action.includes(search) ||
      entry.detail.includes(search) ||
      user?.name.toLowerCase().includes(search.toLowerCase());
    const dateOk =
      dateFilter === "all" ? true :
      dateFilter === "today" ? entry.date === "2024-07-12" :
      dateFilter === "week"  ? entry.date >= "2024-07-08" && entry.date <= "2024-07-14" :
      entry.date.startsWith("2024-07");
    return catOk && searchOk && dateOk;
  });

  const PAGE_SIZE = 10;
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const todayCount    = LOGS.filter((l) => l.date === "2024-07-12").length;
  const failedCount   = LOGS.filter((l) => l.status === "failed").length;
  const approvalCount = LOGS.filter((l) => l.cat === "approval").length;

  const stats = [
    { icon: <Activity size={20} />,     label: "전체 로그",   value: LOGS.length,   sub: "전체 기간",        color: "#1A73E8", bg: "#EBF4FF" },
    { icon: <Clock size={20} />,        label: "오늘 활동",   value: todayCount,    sub: "2024-07-12",       color: "#10B981", bg: "#D1FAE5" },
    { icon: <AlertTriangle size={20} />,label: "로그인 실패", value: failedCount,   sub: "주의 필요",        color: "#EF4444", bg: "#FEE2E2" },
    { icon: <FileCheck size={20} />,    label: "결재 처리",   value: approvalCount, sub: "전자결재 이벤트",  color: "#8B5CF6", bg: "#EDE9FE" },
  ];

  const tabs = [
    { key: "all",      label: "전체",          count: LOGS.length },
    { key: "auth",     label: "로그인/로그아웃", count: LOGS.filter(l=>l.cat==="auth").length },
    { key: "hr",       label: "입사/퇴사",      count: LOGS.filter(l=>l.cat==="hr").length },
    { key: "approval", label: "전자결재",       count: LOGS.filter(l=>l.cat==="approval").length },
    { key: "task",     label: "업무",           count: LOGS.filter(l=>l.cat==="task").length },
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
          <p className={s.pageSub}>시스템 내 주요 활동과 보안 이벤트를 추적합니다</p>
        </div>
        <button className={s.exportBtn}>
          <Download size={15} />
          CSV 내보내기
        </button>
      </div>

      <div className={s.statsGrid}>
        {stats.map((st) => (
          <div key={st.label} className={s.statCard}>
            <div className={s.statIcon} style={{ "--stat-bg": st.bg, "--stat-color": st.color }}>
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
          {tabs.map((tab) => {
            const isActive = catFilter === tab.key;
            const color = tab.key === "all" ? "#1A73E8" : CAT_CFG[tab.key]?.color || "#1A73E8";
            const bg = tab.key === "all" ? "#EBF4FF" : CAT_CFG[tab.key]?.bg || "#EBF4FF";
            return (
              <button
                key={tab.key}
                onClick={() => { setCatFilter(tab.key); setPage(1); }}
                className={`${s.tabBtn} ${isActive ? s.tabBtnActive : ""}`}
                style={isActive ? { color } : undefined}
              >
                {tab.label}
                <span
                  className={s.tabCount}
                  style={isActive ? { background: bg, color } : undefined}
                >
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        <div className={s.dateGroup}>
          {DATE_FILTER_OPTS.map((opt) => (
            <button
              key={opt.key}
              onClick={() => { setDateFilter(opt.key); setPage(1); }}
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
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            className={s.searchInput}
          />
        </div>

        <span className={s.resultCount}>
          <strong>{filtered.length}</strong>건 조회됨
        </span>
      </div>

      <WSCard>
        {paged.length === 0 ? (
          <div className={s.empty}>
            <Activity size={40} className={s.emptyIcon} />
            <p className={s.emptyTitle}>조회된 로그가 없습니다</p>
            <p className={s.emptyDesc}>검색 조건을 변경해 보세요.</p>
          </div>
        ) : (
          <div>
            {paged.map((entry, idx) => {
              const user = TEAM_MEMBERS.find((m) => m.id === entry.userId);
              const cat  = CAT_CFG[entry.cat];
              const st   = STATUS_CFG[entry.status];
              const prevEntry = paged[idx - 1];
              const showDateHeader = idx === 0 || prevEntry?.date !== entry.date;

              return (
                <div key={entry.id}>
                  {showDateHeader && (
                    <div className={`${s.dateDivider} ${idx !== 0 ? s.dateDividerTop : ""}`}>
                      <span className={s.dateDividerLabel}>{dateLabel(entry.date)}</span>
                      <span className={s.dateDividerCount}>
                        {paged.filter((l) => l.date === entry.date).length}건
                      </span>
                    </div>
                  )}

                  <div className={s.logRow} style={{ "--log-color": cat.color }}>
                    <div className={s.catIcon} style={{ "--cat-bg": cat.bg, "--cat-color": cat.color }}>
                      <ActionIcon action={entry.action} size={15} />
                    </div>

                    <div className={s.userCell}>
                      <WSAvatar src={user.avatar} name={user.name} size={30} />
                      <div className={s.userBody}>
                        <p className={s.userName}>{user.name}</p>
                        <p className={s.userRole}>{user.role}</p>
                      </div>
                    </div>

                    <div className={s.actionCell}>
                      <div className={s.actionRow}>
                        <span className={s.actionBadge} style={{ "--cat-bg": cat.bg, "--cat-color": cat.color }}>
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
                      <p className={s.timeDate}>{entry.date.slice(5).replace("-", "/")}</p>
                    </div>

                    <div className={s.statusCell}>
                      <span className={s.statusBadge} style={{ "--status-bg": st.bg, "--status-color": st.color }}>
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
          <WSPagination current={page} total={totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
}
