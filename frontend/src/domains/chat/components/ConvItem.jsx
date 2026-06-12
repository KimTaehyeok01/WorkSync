import { Users } from "lucide-react";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { statusColor, JOB_GRADE } from "./chatUtil";
import s from "../pages/ChatPage.module.css";

// 대화방 리스트
export function ConvItem({ conv, active, onClick }) {
  const name = conv.name;
  const roomType = conv.roomType;
  const avatar = conv.thumbnailImage;
  const status = conv.otherStatus;
  const unread = conv.unreadCount > 0;

  return (
    <button
      onClick={onClick}
      className={`${s.convItem} ${active ? s.convItemActive : ""}`}
      type="button"
      aria-pressed={active}
    >
      {roomType === "DIRECT" ? (
        <div className={s.avatarWrap}>
          <WSAvatar src={avatar} name={name} size={36} />
          <span
            className={s.statusDot}
            style={{
              "--status-color": statusColor(status),
            }}
          />
        </div>
      ) : (
        <div className={s.groupIcon}>
          <Users size={17} />
        </div>
      )}

      <div className={s.convBody}>
        <div className={s.convTop}>
          <span
            className={`${s.convName} ${active ? s.convNameActive : ""} ${unread ? s.convNameUnread : ""}`}
          >
            {name}
          </span>
          <span className={s.convTime}>{conv.lastMessageAt}</span>
        </div>
        <p className={`${s.convLast} ${unread ? s.convLastUnread : ""}`}>
          {conv.lastMessage}
        </p>
      </div>

      {unread && <span className={s.unreadBadge}>{conv.unreadCount}</span>}
    </button>
  );
}
