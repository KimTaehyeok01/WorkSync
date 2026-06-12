import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
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
  UserRound,
} from "lucide-react";
import { WSAvatar } from "../../components/common/CommonWidgets";
import styles from "./TopBar.module.css";
import useAuthContext from "../../store/AuthContext";
import { getMyInfo, patchStatus } from "../service/TopBarApi";
import {
  getNotifications,
  getUnreadCount,
  putNotifications,
} from "../../domains/notification/services/notificationApi";
import { getEmployee } from "../../domains/chat/services/chatApi";

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
  "/organization/new": {
    title: "조직도",
    breadcrumb: ["홈", "인사", "조직도"],
  },
  "/organization/edit": {
    title: "조직도",
    breadcrumb: ["홈", "인사", "조직도"],
  },
  "/board": { title: "게시판", breadcrumb: ["홈", "게시판"] },
  "/board/new": { title: "글쓰기", breadcrumb: ["홈", "게시판", "글쓰기"] },
  "/audit-log": { titlse: "감사 로그", breadcrumb: ["홈", "감사 로그"] },
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

// 직급
const JOB_GRADE = {
  STAFF: "사원",
  SENIOR: "주임",
  ASSISTANT_MANAGER: "대리",
  MANAGER: "과장",
  GENERAL_MANAGER: "부장",
  DIRECTOR: "이사",
  CEO: "대표",
};

export function TopBar({ pathname }) {
  const navigate = useNavigate();
  // 상태(status)를 전역 Context와 연결 — 메신저 등 다른 화면에 즉시 반영
  const {
    accessToken,
    logout,
    myStatus: status,
    setMyStatus: setStatus,
  } = useAuthContext();
  const [my, setMy] = useState({});
  const [showNotifs, setShowNotifs] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [search, setSearch] = useState("");
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifId, setNotifId] = useState(0);
  const clientRef = useRef(null);
  const page = PAGE_TITLES[pathname] || {
    title: "WorkSync",
    breadcrumb: ["홈"],
  };

  // 외부 클릭 감지
  const notifRef = useRef(null);
  const profileRef = useRef(null);

  useEffect(() => {
    const handleOutsideClick = (e) => {
      // 알림 닫기
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setShowNotifs(false);
      }
      // 프로필 닫기
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setShowProfile(false);
      }
    };

    // 마우스 클릭 감지
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  // 내 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      setMy(data.data || {});
      setStatus(data.data.status || "");
    });

    getNotifications(accessToken).then((data) => {
      const notif = Array.isArray(data) ? data : [];
      setNotifications(notif || []);
    });

    getUnreadCount(accessToken).then((data) => {
      // console.log(data);
      setUnreadCount(data.unreadCount || 0);
    });
  }, [accessToken]);

  // 알림 리스트 클릭
  const handleClick = (notif) => {
    if (!notif) return;

    setNotifId(notif.id);

    putNotifications(accessToken, {
      targetType: notif.targetType,
      targetId: notif.targetId,
    });

    if (notif.type === "APPROVAL") {
      return navigate(`/approval/${notif.targetId}`);
    } else if (notif.type === "MESSAGE") {
      return navigate(`/messenger`);
    } else if (notif.type === "TASK") {
      return navigate(`/tasks/${notif.targetId}`);
    }
  };

  // WebSocket 실시간 알림 갱신
  useEffect(() => {
    if (!accessToken) return;

    if (clientRef.current?.active) return; // 이미 연결되어 있으면 skip

    if (clientRef.current) {
      clientRef.current.deactivate(); // 이전 연결 해제
    }

    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },

      debug: (str) => {
        console.log("STOMP:", str);
      },

      onConnect: () => {
        console.log("연결 성공");

        // 알림 목록 실시간 불러오기
        client.subscribe("/user/queue/notifications", (frame) => {
          const notifList = JSON.parse(frame.body);
          setNotifications(Array.isArray(notifList) ? notifList : []);
        });

        // 알림 unread count 실시간 불러오기
        client.subscribe("/user/queue/notifications/unread-count", (frame) => {
          const unreadCount = JSON.parse(frame.body);
          setUnreadCount(unreadCount || 0);
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

  // AWAYS 상태 변경
  function handleAwayStatus() {
    try {
      patchStatus(accessToken, "AWAY");
      setStatus("AWAY");
      setShowProfile(false); // 클릭했을 때 드롭다운 자동 닫힘
    } catch (errors) {
      console.log(errors);
    }
  }

  // ACTIVE 상태 변경
  function handleActiveStatus() {
    try {
      patchStatus(accessToken, "ACTIVE");
      setStatus("ACTIVE");
      setShowProfile(false); // 클릭했을 때 드롭다운 자동 닫힘
    } catch (errors) {
      console.log(errors);
    }
  }

  // INACTIVE 상태 변경
  function handleInactiveStatus() {
    try {
      logout();
    } catch (errors) {
      console.log(errors);
    }
  }

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
          placeholder="통합 검색하세요."
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

      <div className={styles.bellWrap} ref={notifRef}>
        <button
          onClick={() => {
            setShowNotifs(true);
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
              {notifications
                .filter((notif) => notif.isRead === false)
                .map((notif) => (
                  <div
                    key={notif.id}
                    className={`${styles.notifItem} ${!notif.read ? styles.notifItemUnread : ""}`}
                    onClick={(e) => {
                      e.stopPropagation(); //부모 onClick 버블링 막음
                      handleClick(notif);
                      setShowNotifs(false);
                    }}
                  >
                    <div className={styles.notifBody}>
                      <p className={styles.notifText}>{notif.content}</p>
                      <p className={styles.notifTime}>{notif.createdAt}</p>
                    </div>
                    {!notif.readAt && <span className={styles.notifDot} />}
                  </div>
                ))}
            </div>
            <div className={styles.notifFooter}></div>
          </div>
        )}
      </div>

      <div className={styles.profileWrap} ref={profileRef}>
        <button
          onClick={() => {
            setShowProfile(true);
            setShowNotifs(false);
          }}
          className={styles.profileBtn}
          aria-label="프로필 메뉴 열기"
          aria-haspopup="menu"
          aria-expanded={showProfile}
        >
          <div className={styles.profileAvatarWrap}>
            <WSAvatar src={my.profileImage} name={my.name} size={36} />
            {/* 프로필이미지 상태표시(온라인)  */}
            <span
              className={`${styles.profileStatus} ${status === "AWAY" ? styles.profileStatusAway : ""}`}
            />
          </div>
          <div className={styles.profileMeta}>
            <div className={styles.profileName}>{my.name}</div>
            <div
              className={`${styles.profileSub} ${status === "AWAY" ? styles.profileSubAway : ""}`}
            >
              {status === "AWAY" ? "자리 비움" : JOB_GRADE[my.jobGrade]}
            </div>
          </div>
          <ChevronDown size={14} className={styles.profileChevron} />
        </button>

        {showProfile && (
          <div className={`${styles.dropdown} ${styles.profileDropdown}`}>
            {status === "ACTIVE" ? (
              <button
                className={`${styles.menuItem} ${styles.profileAwayBadge}`}
                onClick={handleAwayStatus}
                type="button"
              >
                <Coffee size={15} />
                자리 비움
              </button>
            ) : (
              <button
                className={`${styles.menuItem} ${styles.profileAwayBadge}`}
                onClick={handleActiveStatus}
                type="button"
              >
                <UserRound size={15} />
                온라인
              </button>
            )}
            <button
              className={`${styles.menuItem} ${styles.logoutItem}`}
              type="button"
              onClick={handleInactiveStatus}
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
