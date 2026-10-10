// 알림 센터 페이지 — 지난 알림까지 유형별로 모아 보고 읽음 처리한다
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Bell,
  CheckCheck,
  FileCheck,
  CheckSquare,
  MessageSquare,
  Mail,
  CalendarDays,
  CalendarClock,
  LayoutList,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSButton,
  WSEmptyState,
  WSPagination,
} from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { NOTIFICATION_TYPE_LABEL } from "../../../constants/statusLabels";
import { formatRelative } from "../../../utils/dateFormatter";
import {
  getNotificationCenter,
  readNotification,
  readAllNotifications,
  getNotificationPath,
} from "../services/notificationCenterApi";
import s from "./NotificationPage.module.css";

const PER_PAGE = 20;

const TYPE_ICON = {
  APPROVAL: FileCheck,
  TASK: CheckSquare,
  MESSAGE: MessageSquare,
  MAIL: Mail,
  SCHEDULE: CalendarDays,
  RESERVATION: CalendarClock,
  BOARD: LayoutList,
};

const TYPE_FILTERS = [
  { key: "", label: "전체" },
  ...Object.entries(NOTIFICATION_TYPE_LABEL).map(([key, label]) => ({ key, label })),
];

export default function NotificationPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [type, setType] = useState("");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(1);
  const [data, setData] = useState({ content: [], totalElements: 0 });

  const load = async () => {
    const json = await getNotificationCenter(accessToken, {
      type,
      unreadOnly,
      page: page - 1,
    });
    setData(json?.data ?? { content: [], totalElements: 0 });
  };

  useEffect(() => {
    if (!accessToken) return;
    load();
  }, [accessToken, type, unreadOnly, page]);

  // 알림을 누르면 읽음 처리하고 해당 화면으로 이동
  const handleClick = async (notification) => {
    if (!notification.isRead) {
      await readNotification(accessToken, notification.id);
    }
    const path = getNotificationPath(notification);
    if (path) {
      navigate(path);
    } else {
      load();
    }
  };

  const handleReadAll = async () => {
    const json = await readAllNotifications(accessToken);
    if (isOk(json)) load();
  };

  return (
    <div className={s.root}>
      <div className={s.toolbar}>
        <div className={s.filters}>
          {TYPE_FILTERS.map((filter) => (
            <button
              key={filter.key}
              type="button"
              className={`${s.chip} ${type === filter.key ? s.chipOn : ""}`}
              onClick={() => {
                setType(filter.key);
                setPage(1);
              }}
            >
              {filter.label}
            </button>
          ))}
        </div>
        <div className={s.actions}>
          <label className={s.toggle}>
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(e) => {
                setUnreadOnly(e.target.checked);
                setPage(1);
              }}
            />
            안 읽은 알림만
          </label>
          <WSButton
            label="모두 읽음"
            size="sm"
            variant="secondary"
            icon={<CheckCheck size={14} />}
            onClick={handleReadAll}
          />
        </div>
      </div>

      <div className={s.list}>
        {data.content.length === 0 ? (
          <WSEmptyState
            icon={<Bell size={32} />}
            title="알림이 없습니다"
            description="조건에 맞는 알림이 없습니다."
          />
        ) : (
          data.content.map((notification) => {
            const Icon = TYPE_ICON[notification.type] ?? Bell;
            return (
              <button
                key={notification.id}
                type="button"
                className={`${s.item} ${notification.isRead ? "" : s.itemUnread}`}
                onClick={() => handleClick(notification)}
              >
                <span className={`${s.icon} ${s[`icon${notification.type}`] ?? ""}`}>
                  <Icon size={16} />
                </span>
                <span className={s.body}>
                  <span className={s.content}>{notification.content}</span>
                  <span className={s.meta}>
                    {NOTIFICATION_TYPE_LABEL[notification.type] ?? notification.type} ·{" "}
                    {formatRelative(notification.createdAt)}
                  </span>
                </span>
                {!notification.isRead && <span className={s.dot} />}
              </button>
            );
          })
        )}
      </div>

      {data.totalElements > PER_PAGE && (
        <WSPagination
          total={data.totalElements}
          page={page}
          perPage={PER_PAGE}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
