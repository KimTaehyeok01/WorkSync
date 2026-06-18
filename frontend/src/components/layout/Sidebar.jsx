import { useState, useRef, useEffect } from "react";
import { NavLink, useLocation, useParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
import useAuthContext from "../../store/AuthContext";
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
import { getNotifications } from "../../domains/notification/services/notificationApi";
import styles from "./Sidebar.module.css";
import { getMyInfo } from "../service/TopBarApi";

const MAIN_NAV = [
  { path: "/", label: "대시보드", icon: LayoutDashboard, id: "ws-dash" },
  {
    path: "/approval",
    label: "전자결재",
    icon: FileCheck,
    id: "ws-apv",
    type: "APPROVAL",
  },
  {
    path: "/tasks",
    label: "업무",
    icon: CheckSquare,
    id: "ws-task",
    type: "TASK",
  },
  {
    path: "/messenger",
    label: "메신저",
    icon: MessageSquare,
    id: "ws-msg",
    type: "MESSAGE",
  },
  {
    path: "/organization",
    label: "조직도",
    icon: Users,
    id: "ws-org",
    type: "ORGANIZATION",
  },
  {
    path: "/board",
    label: "게시판",
    icon: LayoutList,
    id: "ws-board",
    type: "BOARD",
  },
];

const BOTTOM_NAV = [{ path: "/audit-log", label: "감사 로그", icon: Activity }];

export function Sidebar() {
  const location = useLocation();
  const { accessToken } = useAuthContext();
  const [unreadBadge, setUnreadBadge] = useState({});
  const [my, setMy] = useState({});
  const clientRef = useRef(null);

  const isMainActive = (path) =>
    path === "/"
      ? location.pathname === "/"
      : location.pathname === path || location.pathname.startsWith(path + "/");

  // 내 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      setMy(data.data || {});
    });
  }, [accessToken]);

  // WebSocket 실시간 알림 갱신
  useEffect(() => {
    if (!accessToken) return;
    if (clientRef.current?.active) return; // 이미 연결되어 있으면 skip
    if (clientRef.current) {
      clientRef.current.deactivate(); // 이전 연결 해제
    }

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        // 실시간 알림 목록으로 안읽음 갯수 카운트
        client.subscribe("/user/queue/notifications", (frame) => {
          const notifList = JSON.parse(frame.body);
          const list = Array.isArray(notifList) ? notifList : [];
          const listUnread = list.filter((item) => item.isRead === false);
          const groupCount = listUnread.reduce((acc, item) => {
            acc[item.type] = (acc[item.type] || 0) + 1;
            return acc;
          }, {});

          setUnreadBadge(groupCount ? groupCount : 0);
        });
      },
      onStompError: (frame) => {
        console.error("STOMP ERROR", frame);
      },
      onWebSocketError: (event) => {
        console.error("WS ERROR", event);
      },
    });
    clientRef.current = client;
    client.activate();
    return () => client.deactivate();
  }, [accessToken]);

  // 알림 목록 전자결재/업무/메신저 별 안읽음 개수
  useEffect(() => {
    getNotifications(accessToken).then((data) => {
      const list = Array.isArray(data) ? data : [];
      const listUnread = list.filter((item) => item.isRead === false);
      const groupCount = listUnread.reduce((acc, item) => {
        acc[item.type] = (acc[item.type] || 0) + 1;
        return acc;
      }, {});

      setUnreadBadge(groupCount ? groupCount : 0);
    });
  }, [accessToken]);

  return (
    <aside className={styles.aside}>
      <div className={styles.brand}>
        <div className={styles.brandLogo}>
          <img src="/favicon_32x32.png" alt="WorkSync"/>
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
                {item.type && unreadBadge[item.type] > 0 && (
                  <span className={styles.badge}>{unreadBadge[item.type]}</span>
                )}
                {active && !(item.type && unreadBadge[item.type] > 0) && (
                  <ChevronRight size={14} className={styles.chevron} />
                )}
              </NavLink>
            );
          })}
        </div>

        {my.role === "ADMIN" ? (
          <>
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
                      active
                        ? `${styles.navItem} ${styles.active}`
                        : styles.navItem
                    }
                  >
                    <Icon size={18} className={styles.navIcon} />
                    <span className={styles.navLabel}>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          </>
        ) : (
          <></>
        )}
      </nav>
    </aside>
  );
}
