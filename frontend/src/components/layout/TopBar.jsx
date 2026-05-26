import { useState } from "react";
import {
  Search,
  Bell,
  ChevronDown,
  Settings,
  LogOut,
  User,
  HelpCircle,
  X,
  CheckCircle,
  MessageSquare,
  FileCheck,
  AlertCircle,
  Coffee,
} from "lucide-react";
import { NOTIFICATIONS, TEAM_MEMBERS } from "../../constants/mockData";
import styles from "./TopBar.module.css";
import useAuthContext from "../../store/AuthContext";

const me = TEAM_MEMBERS[3];

const PAGE_TITLES = {
  "/": { title: "대시보드", breadcrumb: ["홈", "대시보드"] },
  "/approval": { title: "전자결재", breadcrumb: ["홈", "전자결재"] },
  "/approval/new": {
    title: "기안서 작성",
    breadcrumb: ["홈", "전자결재", "기안서 작성"],
  },
  "/tasks": { title: "업무 보드", breadcrumb: ["홈", "업무"] },
  "/tasks/new": {
    title: "새 작업 등록",
    breadcrumb: ["홈", "업무", "새 작업"],
  },
  "/messenger": { title: "메신저", breadcrumb: ["홈", "메신저"] },
  "/organization": { title: "조직도", breadcrumb: ["홈", "인사", "조직도"] },
  "/board": { title: "게시판", breadcrumb: ["홈", "게시판"] },
  "/board/new": { title: "글쓰기", breadcrumb: ["홈", "게시판", "글쓰기"] },
  "/audit-log": { title: "감사 로그", breadcrumb: ["홈", "감사 로그"] },
};

const notifIcons = {
  approval: <FileCheck size={13} />,
  mention: <MessageSquare size={13} />,
  message: <MessageSquare size={13} />,
  task: <CheckCircle size={13} />,
  system: <AlertCircle size={13} />,
};

const notifColors = {
  approval: "#F59E0B",
  mention: "#1A73E8",
  message: "#10B981",
  task: "#8B5CF6",
  system: "#6B7280",
};

export function TopBar({ pathname }) {
  const [showNotifs, setShowNotifs] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [search, setSearch] = useState("");
  const [isAway, setIsAway] = useState(false);
  const page = PAGE_TITLES[pathname] || {
    title: "WorkSync",
    breadcrumb: ["홈"],
  };
  const unreadCount = NOTIFICATIONS.filter((n) => !n.read).length;
  const { logout } = useAuthContext();

  return (
    <header className={styles.header}>
      <div className={styles.titleArea}>
        <h1 className={styles.title}>{page.title}</h1>
        <div className={styles.breadcrumb}>
          {page.breadcrumb.map((crumb, idx) => {
            const isLast = idx === page.breadcrumb.length - 1;
            return (
              <span key={idx} className={styles.crumbItem}>
                <span
                  className={isLast ? styles.crumbCurrent : styles.crumbLink}
                >
                  {crumb}
                </span>
                {!isLast && <span className={styles.crumbSep}>/</span>}
              </span>
            );
          })}
        </div>
      </div>

      <div className={styles.search}>
        <Search size={15} className={styles.searchIcon} />
        <input
          type="text"
          placeholder="통합 검색..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className={styles.searchInput}
        />
        {search && (
          <button
            onClick={() => setSearch("")}
            className={styles.searchClear}
            aria-label="검색어 지우기"
          >
            <X size={13} />
          </button>
        )}
      </div>

      <div className={styles.bellWrap}>
        <button
          onClick={() => {
            setShowNotifs(!showNotifs);
            setShowProfile(false);
          }}
          className={styles.iconBtn}
          aria-label={`알림 ${unreadCount}건 보기`}
          aria-haspopup="menu"
          aria-expanded={showNotifs}
        >
          <Bell size={19} />
          {unreadCount > 0 && (
            <span className={styles.bellBadge}>{unreadCount}</span>
          )}
        </button>

        {showNotifs && (
          <div className={`${styles.dropdown} ${styles.notifDropdown}`}>
            <div className={styles.notifHeader}>
              <span className={styles.notifTitle}>알림</span>
              <span className={styles.notifCount}>새 알림 {unreadCount}건</span>
            </div>
            <div className={styles.notifList}>
              {NOTIFICATIONS.map((notif) => (
                <div
                  key={notif.id}
                  className={`${styles.notifItem} ${!notif.read ? styles.notifItemUnread : ""}`}
                >
                  <div
                    className={styles.notifIcon}
                    style={{
                      "--notif-color": notifColors[notif.type] || "#6B7280",
                    }}
                  >
                    {notif.actor ? (
                      <img src={notif.actor.avatar} alt="" />
                    ) : (
                      notifIcons[notif.type]
                    )}
                  </div>
                  <div className={styles.notifBody}>
                    <p className={styles.notifText}>{notif.text}</p>
                    <p className={styles.notifTime}>{notif.time}</p>
                  </div>
                  {!notif.read && <span className={styles.notifDot} />}
                </div>
              ))}
            </div>
            <div className={styles.notifFooter}></div>
          </div>
        )}
      </div>

      <div className={styles.profileWrap}>
        <button
          onClick={() => {
            setShowProfile(!showProfile);
            setShowNotifs(false);
          }}
          className={styles.profileBtn}
          aria-label="프로필 메뉴 열기"
          aria-haspopup="menu"
          aria-expanded={showProfile}
        >
          <div className={styles.profileAvatarWrap}>
            <img
              src={me.avatar}
              alt={me.name}
              className={styles.profileAvatar}
            />
            {/* 프로필이미지 상태표시(온라인)  */}
            <span
              className={`${styles.profileStatus} ${isAway ? styles.profileStatusAway : ""}`}
            />
          </div>
          <div className={styles.profileMeta}>
            <div className={styles.profileName}>{me.name}</div>
            <div
              className={`${styles.profileSub} ${isAway ? styles.profileSubAway : ""}`}
            >
              {isAway ? "자리 비움" : me.role}
            </div>
          </div>
          <ChevronDown size={14} className={styles.profileChevron} />
        </button>

        {showProfile && (
          <div className={`${styles.dropdown} ${styles.profileDropdown}`}>
            {/* <div className={styles.profileDropdownHeader}>
              <div className={styles.profileDropdownName}>{me.name}</div>
              <div className={styles.profileDropdownEmail}>{me.email}</div>
              {isAway && <div className={styles.profileAwayBadge}></div>}
            </div> */}
            {/* {[
              { icon: User, label: "내 프로필" },
              { icon: Settings, label: "계정 설정" },
              { icon: HelpCircle, label: "도움말 및 지원" },
            ].map((item) => {
              const Icon = item.icon;
              return (
                <button
                  key={item.label}
                  className={styles.menuItem}
                  type="button"
                >
                  <Icon size={15} className={styles.menuIcon} />
                  <span>{item.label}</span>
                </button>
              );
            })} */}

            <button
              className={`${styles.menuItem} ${styles.profileAwayBadge}`}
              onClick={() => {
                setIsAway(!isAway);
                setShowProfile(false); // 자리비움 클릭했을 때 드롭다운 자동 닫힘
                console.log("isAway 변경:", !isAway);
              }}
              type="button"
            >
              <Coffee size={15} />
              {isAway ? "온라인으로 변경" : "자리비움"}
            </button>
            <button
              className={`${styles.menuItem} ${styles.logoutItem}`}
              type="button"
              onClick={logout}
            >
              <LogOut size={15} />
              <span>로그아웃</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
