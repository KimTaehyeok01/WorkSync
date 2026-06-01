import { NavLink, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  CheckSquare,
  MessageSquare,
  Users,
  Settings,
  FileCheck,
  ChevronRight,
  Zap,
  LayoutList,
  LogIn,
  Activity,
} from "lucide-react";
import { TEAM_MEMBERS } from "../../constants/mockData";
import styles from "./Sidebar.module.css";

const MAIN_NAV = [
  { path: "/", label: "대시보드", icon: LayoutDashboard, id: "ws-dash" },
  {
    path: "/approval",
    label: "전자결재",
    icon: FileCheck,
    id: "ws-apv",
    badge: 3,
  },
  { path: "/tasks", label: "업무", icon: CheckSquare, id: "ws-task" },
  {
    path: "/messenger",
    label: "메신저",
    icon: MessageSquare,
    id: "ws-msg",
    badge: 5,
  },
  { path: "/organization", label: "조직도", icon: Users, id: "ws-org" },
  { path: "/board", label: "게시판", icon: LayoutList, id: "ws-board" },
];

const BOTTOM_NAV = [{ path: "/audit-log", label: "감사 로그", icon: Activity }];

const me = TEAM_MEMBERS[3];

export function Sidebar() {
  const location = useLocation();

  const isMainActive = (path) =>
    path === "/"
      ? location.pathname === "/"
      : location.pathname === path || location.pathname.startsWith(path + "/");

  return (
    <aside className={styles.aside}>
      <div className={styles.brand}>
        <div className={styles.brandLogo}>
          <Zap size={18} color="#fff" fill="#fff" />
        </div>
        <div>
          <div className={styles.brandName}>WorkSync</div>
          <div className={styles.brandSub}>ENTERPRISE v2.4</div>
        </div>
      </div>

      <nav className={styles.nav}>
        <div className={styles.sectionLabel}>메인 메뉴</div>
        <div className={styles.navGroup}>
          {MAIN_NAV.map((item) => {
            const active = isMainActive(item.path);
            const Icon = item.icon;
            return (
              <NavLink
                key={item.id}
                to={item.path}
                end={item.path === "/"}
                className={
                  active ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
              >
                <Icon size={18} className={styles.navIcon} />
                <span className={styles.navLabel}>{item.label}</span>
                {item.badge && (
                  <span className={styles.badge}>{item.badge}</span>
                )}
                {active && !item.badge && (
                  <ChevronRight size={14} className={styles.chevron} />
                )}
              </NavLink>
            );
          })}
        </div>

        <div className={styles.sectionLabel}>시스템</div>
        <div className={styles.navGroup}>
          {BOTTOM_NAV.map((item) => {
            const Icon = item.icon;
            const active = location.pathname === item.path;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={
                  active ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
              >
                <Icon size={18} className={styles.navIcon} />
                <span className={styles.navLabel}>{item.label}</span>
              </NavLink>
            );
          })}
        </div>
      </nav>

      <div className={styles.footer}>
        <NavLink to="/login" className={styles.loginShortcut}>
          <LogIn size={15} />
          <span>로그인 화면 미리보기</span>
        </NavLink>
        {/* <div className={styles.profile}>
          <div className={styles.avatarWrap}>
            <img src={me.avatar} alt={me.name} className={styles.avatar} />
            <span className={styles.status} />
          </div>
          <div className={styles.profileMeta}>
            <div className={styles.profileName}>{me.name}</div>
            <div className={styles.profileRole}>{me.role}</div>
          </div>
          <Settings size={14} className={styles.profileIcon} />
        </div> */}
      </div>
    </aside>
  );
}
