import { useState } from "react";
import {
  Search,
  Send,
  Paperclip,
  MoreHorizontal,
  Plus,
  Users,
  CheckCheck,
  X,
  MessageSquare,
  Download,
} from "lucide-react";
import { TEAM_MEMBERS, MESSAGES } from "../../../constants/mockData";
import s from "./ChatPage.module.css";

const CONVERSATIONS = [
  {
    id: 1,
    type: "direct",
    member: TEAM_MEMBERS[1],
    lastMessage: "응, 오후 3시로 하자. 캘린더 초대 보낼게.",
    time: "오전 10:39",
    unread: 2,
    pinned: true,
  },
  {
    id: 2,
    type: "direct",
    member: TEAM_MEMBERS[2],
    lastMessage: "API 엔드포인트 테스트 준비 완료.",
    time: "오전 9:15",
    unread: 0,
    pinned: false,
  },
  {
    id: 3,
    type: "group",
    name: "🚀 스프린트 7 팀",
    avatar: null,
    members: TEAM_MEMBERS.slice(0, 4),
    lastMessage: "James: 대시보드 재디자인 수고했어요!",
    time: "어제",
    unread: 5,
    pinned: true,
  },
  {
    id: 4,
    type: "direct",
    member: TEAM_MEMBERS[4],
    lastMessage: "디자인 스펙 보냈어.",
    time: "어제",
    unread: 0,
    pinned: false,
  },
  {
    id: 5,
    type: "group",
    name: "# 일반",
    avatar: null,
    members: TEAM_MEMBERS,
    lastMessage: "Aisha: 오늘 오후 4시 회고 잊지 마세요!",
    time: "7월 10일",
    unread: 0,
    pinned: false,
  },
  {
    id: 6,
    type: "direct",
    member: TEAM_MEMBERS[0],
    lastMessage: "3분기 예산 문서 검토 부탁드립니다.",
    time: "7월 9일",
    unread: 0,
    pinned: false,
  },
];

const FILE_ATTACHMENTS = [
  { name: "design_mockup_v3.fig", size: "4.2 MB", type: "fig" },
  { name: "sprint7_backlog.xlsx", size: "1.1 MB", type: "xlsx" },
  { name: "api_docs.pdf", size: "2.8 MB", type: "pdf" },
];

const TYPE_COLORS = { fig: "#F24E1E", xlsx: "#217346", pdf: "#F40F02" };

function statusColor(status) {
  if (status === "online") return "#48BB78";
  if (status === "away") return "#F6AD55";
  return "#A0AEC0";
}

function ConvItem({ conv, active, onClick }) {
  const name = conv.type === "direct" ? conv.member?.name : conv.name;
  const avatar = conv.type === "direct" ? conv.member?.avatar : null;
  const status = conv.type === "direct" ? conv.member?.status : null;
  const unread = conv.unread > 0;

  return (
    <button
      onClick={onClick}
      className={`${s.convItem} ${active ? s.convItemActive : ""}`}
      type="button"
      aria-pressed={active}
    >
      {avatar ? (
        <div className={s.avatarWrap}>
          <img src={avatar} alt={name || ""} className={s.avatarImg} />
          {status && (
            <span
              className={s.statusDot}
              style={{ "--status-color": statusColor(status) }}
            />
          )}
        </div>
      ) : (
        <div className={s.groupIcon}>
          <Users size={15} />
        </div>
      )}

      <div className={s.convBody}>
        <div className={s.convTop}>
          <span
            className={`${s.convName} ${active ? s.convNameActive : ""} ${unread ? s.convNameUnread : ""}`}
          >
            {name}
          </span>
          <span className={s.convTime}>{conv.time}</span>
        </div>
        <p className={`${s.convLast} ${unread ? s.convLastUnread : ""}`}>
          {conv.lastMessage}
        </p>
      </div>

      {unread && <span className={s.unreadBadge}>{conv.unread}</span>}
    </button>
  );
}

function NewConvModal({ onClose, onCreate }) {
  const [convType, setConvType] = useState("direct");
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [groupName, setGroupName] = useState("");

  function toggle(id) {
    setSelectedMembers((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }
  function handleCreate() {
    if (selectedMembers.length === 0) return;
    const name =
      convType === "group"
        ? groupName || `그룹 채팅 (${selectedMembers.length}명)`
        : TEAM_MEMBERS.find((m) => m.id === selectedMembers[0])?.name || "";
    onCreate(name, selectedMembers);
    onClose();
  }

  const otherMembers = TEAM_MEMBERS.filter((m) => m.id !== 4);

  return (
    <div
      className={s.modalBackdrop}
      onClick={(e) => e.target === e.currentTarget && onClose()}
      role="presentation"
    >
      <div
        className={s.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-conversation-title"
      >
        <div className={s.modalHeader}>
          <div className={s.modalHeaderLeft}>
            <MessageSquare size={18} className={s.modalTitleIcon} />
            <h2 id="new-conversation-title" className={s.modalTitle}>
              새 대화 시작
            </h2>
          </div>
          <button
            onClick={onClose}
            className={s.modalClose}
            type="button"
            aria-label="새 대화 모달 닫기"
          >
            <X size={18} />
          </button>
        </div>

        <div className={s.modalBody}>
          <div className={s.tabRow}>
            {[
              { id: "direct", label: "1:1 대화" },
              { id: "group", label: "그룹 채팅" },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => {
                  setConvType(t.id);
                  setSelectedMembers([]);
                }}
                className={`${s.tabBtn} ${convType === t.id ? s.tabBtnActive : ""}`}
                type="button"
                aria-pressed={convType === t.id}
              >
                {t.label}
              </button>
            ))}
          </div>

          {convType === "group" && (
            <input
              type="text"
              placeholder="그룹 이름 (선택사항)"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              className={s.groupNameInput}
            />
          )}

          <div>
            <p className={s.memberLabel}>
              {convType === "direct"
                ? "대화 상대 선택"
                : `참여 멤버 선택 (${selectedMembers.length}명)`}
            </p>
            <div className={s.memberList}>
              {otherMembers.map((m) => {
                const isSelected = selectedMembers.includes(m.id);
                const isDisabled =
                  convType === "direct" &&
                  selectedMembers.length > 0 &&
                  !isSelected;
                return (
                  <button
                    key={m.id}
                    onClick={() => !isDisabled && toggle(m.id)}
                    disabled={isDisabled}
                    className={`${s.memberBtn} ${isSelected ? s.memberBtnSelected : ""} ${isDisabled ? s.memberBtnDisabled : ""}`}
                    type="button"
                    aria-pressed={isSelected}
                  >
                    <div className={s.avatarWrap}>
                      <img
                        src={m.avatar}
                        alt={m.name}
                        className={s.avatarImg}
                      />
                      <span
                        className={s.statusDot}
                        style={{ "--status-color": statusColor(m.status) }}
                      />
                    </div>
                    <div className={s.memberBody}>
                      <p
                        className={`${s.memberName} ${isSelected ? s.memberNameSelected : ""}`}
                      >
                        {m.name}
                      </p>
                      <p className={s.memberRole}>{m.role}</p>
                    </div>
                    {isSelected && <span className={s.checkMark}>✓</span>}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        <div className={s.modalFooter}>
          <button onClick={onClose} className={s.modalCancel} type="button">
            취소
          </button>
          <button
            onClick={handleCreate}
            disabled={selectedMembers.length === 0}
            className={s.modalConfirm}
            type="button"
          >
            대화 시작
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Messenger() {
  const [activeConvId, setActiveConvId] = useState(1);
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [showInfo, setShowInfo] = useState(true);
  const [showNewConvModal, setShowNewConvModal] = useState(false);

  const activeConv = CONVERSATIONS.find((c) => c.id === activeConvId);
  const filteredConvs = CONVERSATIONS.filter((c) => {
    const label = c.type === "direct" ? c.member.name : c.name || "";
    return label.toLowerCase().includes(search.toLowerCase());
  });

  const activeName =
    activeConv?.type === "direct" ? activeConv.member?.name : activeConv?.name;
  const activeAvatar =
    activeConv?.type === "direct" ? activeConv.member?.avatar : null;
  const activeStatus =
    activeConv?.type === "direct" ? activeConv.member?.status : "online";

  return (
    <div className={s.root}>
      <div className={s.sidebar}>
        <div className={s.sidebarHeader}>
          <div className={s.sidebarTitleRow}>
            <h2 className={s.sidebarTitle}>메시지</h2>
            <button
              onClick={() => setShowNewConvModal(true)}
              className={s.newBtn}
              title="새 대화 시작"
              type="button"
              aria-label="새 대화 시작"
            >
              <Plus size={14} />
            </button>
          </div>
          <div className={s.searchWrap}>
            <Search size={13} className={s.searchIcon} />
            <input
              type="text"
              placeholder="대화 검색..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className={s.searchInput}
            />
          </div>
        </div>

        <div className={s.convList}>
          <div className={s.convSection}>
            <p className={s.convLabel}>전체 대화</p>
            {filteredConvs
              .filter((c) => !c.pinned)
              .map((conv) => (
                <ConvItem
                  key={conv.id}
                  conv={conv}
                  active={activeConvId === conv.id}
                  onClick={() => setActiveConvId(conv.id)}
                />
              ))}
          </div>
        </div>
      </div>

      <div className={s.chat}>
        <div className={s.chatHeader}>
          {activeAvatar ? (
            <div className={s.avatarWrap}>
              <img src={activeAvatar} alt="" className={s.avatarImg} />
              <span
                className={s.statusDot}
                style={{ "--status-color": statusColor(activeStatus) }}
              />
            </div>
          ) : (
            <div className={s.groupIcon}>
              <Users size={17} />
            </div>
          )}
          <div>
            <p className={s.chatHeaderName}>{activeName}</p>
            <p className={s.chatHeaderStatus}>
              {activeConv?.type === "direct"
                ? activeStatus === "online"
                  ? "온라인 · 활성"
                  : activeStatus === "away"
                    ? "자리 비움"
                    : "오프라인"
                : `${activeConv?.members?.length}명`}
            </p>
          </div>

          <div className={s.chatActions}>
            <button
              onClick={() => setShowInfo(!showInfo)}
              className={s.iconBtn}
              type="button"
              aria-label="대화 정보 패널 토글"
              aria-expanded={showInfo}
            >
              <MoreHorizontal size={17} />
            </button>
          </div>
        </div>

        <div className={s.messages}>
          <div className={s.dateRow}>
            <div className={s.dateLine} />
            <span className={s.dateLabel}>오늘, 7월 11일</span>
            <div className={s.dateLine} />
          </div>

          {MESSAGES.map((msg) => (
            <div
              key={msg.id}
              className={`${s.msg} ${msg.isMine ? s.msgMine : ""}`}
            >
              {!msg.isMine && (
                <img src={msg.sender.avatar} alt="" className={s.msgAvatar} />
              )}
              <div
                className={`${s.msgBody} ${msg.isMine ? s.msgBodyMine : ""}`}
              >
                {!msg.isMine && (
                  <span className={s.msgSender}>{msg.sender.name}</span>
                )}
                <div
                  className={`${s.bubble} ${msg.isMine ? s.bubbleMine : ""}`}
                >
                  {msg.content}
                </div>
                <div
                  className={`${s.msgMeta} ${msg.isMine ? s.msgMetaMine : ""}`}
                >
                  <span className={s.msgTime}>{msg.time}</span>
                  {msg.isMine && <CheckCheck size={12} color="#60A5FA" />}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className={s.inputBar}>
          <div className={s.inputRow}>
            <button className={s.inputBtn} type="button" aria-label="파일 첨부">
              <Paperclip size={18} />
            </button>
            <input
              type="text"
              placeholder="메시지 입력... (Enter로 전송)"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  setMessage("");
                }
              }}
              className={s.input}
            />
            <button
              className={s.inputBtn}
              onClick={() => setMessage("")}
              type="button"
              aria-label="메시지 전송"
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      </div>

      {showInfo && activeConv && (
        <div className={s.info}>
          {/* 단체 대화 시 구성원 표시 */}
          <div className={s.memberPad}>
            {activeConv?.type === "group" && (
              <div className={s.infomemberList}>
                <h3 className={s.memberTitle}>
                  구성원 ({activeConv.members.length}명)
                </h3>

                {activeConv.members.map((member) => (
                  <div key={member.id} className={s.memberItem}>
                    <div className={s.avatarWrap}>
                      <img
                        src={member.avatar}
                        alt={member.name}
                        className={s.avatarImg}
                      />
                      <span
                        className={s.statusDot}
                        style={{
                          "--status-color": statusColor(member.status),
                        }}
                      />
                    </div>
                    <div>
                      <p className={s.memberName}>{member.name}</p>
                      <p className={s.memberRole}>{member.role}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 공유파일 */}
          <div className={s.infoPad}>
            <h3 className={s.infoTitle}>공유 파일</h3>
            <p className={s.infoSub}>1개 파일 업로드</p>

            <div className={s.fileList}>
              {FILE_ATTACHMENTS.map((file) => (
                <div key={file.name} className={s.fileRow}>
                  <div className={s.fileLeft}>
                    <div
                      className={s.fileIcon}
                      style={{
                        "--file-color": TYPE_COLORS[file.type] || "#6B7280",
                      }}
                    >
                      {file.type.toUpperCase()}
                    </div>
                    <div>
                      <p className={s.fileName}>{file.name}</p>
                      <p className={s.fileSize}>{file.size}</p>
                    </div>
                  </div>
                  <button
                    className={s.fileDl}
                    type="button"
                    aria-label={`${file.name} 다운로드`}
                  ></button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {showNewConvModal && (
        <NewConvModal
          onClose={() => setShowNewConvModal(false)}
          onCreate={() => setShowNewConvModal(false)}
        />
      )}
    </div>
  );
}
