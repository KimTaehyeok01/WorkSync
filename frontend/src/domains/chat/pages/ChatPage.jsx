import { useState, useRef } from "react";
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
// 파일명에서 확장자 추출 - "report.pdf" -> "pdf"
function getExt(filename) {
  return filename.split(".").pop().toLowerCase();
}

// 바이트 단위 파일 크기를 읽기 좋게 변환 - 1048576 -> "1.0MB"
function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// 현재 시각을 "오전 9:05" 형식으로 반환 - 메세지 전송 시각 표시용
function nowTime() {
  const d = new Date();
  const h = d.getHours();
  const m = String(d.getMinutes()).padStart(2, "0");
  return h < 12 ? `오전 ${h}: ${m}` : ` 오후 ${h - 12 || 12}:${m}`;
}

// 하드코딩 더미 데이터라 실제 업로드와 연동 안됨
// const FILE_ATTACHMENTS = [
//   { name: "design_mockup_v3.fig", size: "4.2 MB", type: "fig" },
//   { name: "sprint7_backlog.xlsx", size: "1.1 MB", type: "xlsx" },
//   { name: "api_docs.pdf", size: "2.8 MB", type: "pdf" },
// ];

// PDF / 워드 / 엑셀 / PPT / PNG
// 확장자별 색상과 라벨 정의 - 파일 아이콘 뱃지에 사용
const EXT_MAP = {
  pdf: { color: "#F40F02", label: "PDF" },
  xlsx: { color: "#217346", label: "XLS" },
  xls: { color: "#217346", label: "XLS" },
  docx: { color: "#2B5797", label: "DOC" },
  pptx: { color: "#D04423", label: "PPT" },
  png: { color: "#0EA5E9", label: "IMG" },
};

// 파일명을 받아서 해당 확장자의 색상+라벨 반환
// EXT_MAP에 없는 확장자면 회색 + 확장자 대문자로 표시
function getFileMeta(filename) {
  const ext = getExt(filename);
  return (
    EXT_MAP[ext] || { color: "#6B7280", label: ext.toUpperCase().slice(0, 4) }
  );
}

// 기존
// const TYPE_COLORS = { fig: "#F24E1E", xlsx: "#217346", pdf: "#F40F02" };

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
// 채팅창 안에서 파일 메시지를 보여주는 말풍선 컴포넌트
function FileBubble({ file }) {
  const meta = getFileMeta(file.name); // 확장자에 맞는 색상 + 라벨 가져오기

  // 브라우저에서 직접 다운로드 - 백엔드 없이도 동작
  function handleDownload() {
    const url = URL.createObjectURL(file.raw); // 업로드한 File 객체로 임시 URL 생성
    const a = document.createElement("a");
    a.href = url;
    a.download = file.name;
    a.click();
    URL.revokeObjectURL(url); // 메모리 누수 방지를 위해 URL 즉시 해제
  }

  return (
    // 버튼 전체를 클릭 가능하게 — Download 버튼 제거
    <div
      className={s.fileBubble}
      onClick={handleDownload}
      style={{ cursor: "pointer" }}
      role="button"
      aria-label={`${file.name} 다운로드`}
    >
      <div className={s.fileBubbleIcon} style={{ "--file-color": meta.color }}>
        {meta.label}
      </div>
      <div className={s.fileBubbleBody}>
        <p className={s.fileBubbleName}>{file.name}</p>
        <p className={s.fileBubbleSize}>{file.size}</p>
      </div>
      {/* Download 버튼 삭제 */}
    </div>
  );
}

export default function Messenger() {
  const [activeConvId, setActiveConvId] = useState(1);
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [showInfo, setShowInfo] = useState(true);
  const [showNewConvModal, setShowNewConvModal] = useState(false);

  // 기존 MESSAGES 더미 + 이후 전송한 텍스트/파일 메시지 통합 관리
  const [chatMessages, setChatMessages] = useState(
    MESSAGES.map((m) => ({ ...m, type: "text" })),
  );
  // sharedFiles : 오른쪽 패널에 보여줄 파일 목록
  const [sharedFiles, setSharedFiles] = useState([]);

  // 숨겨진 <input type="file">을 클립 버튼과 연결하기 위한 ref
  const fileInputRef = useRef(null);

  // 파일 선택 시 실행 - 채팅 말풍선 + 오른쪽 목록 동시 업데이트
  function handleFileSelect(e) {
    const files = Array.from(e.target.files); // FileList -> 배열로 변환
    if (files.length === 0) return; // 선택 취소 시 아무것ㄷ 안함
    const now = nowTime();

    files.forEach((file) => {
      // 파일 정보 객체 생성
      const fileEntry = {
        id: Date.now() + Math.random(), // 고유 key용 임시 id
        name: file.name,
        size: formatSize(file.size), // "1.2MB 형식으로 변환"
        ext: getExt(file.name),
        uploadAt: now,
        raw: file, //다운로드를 위해 원본 File 객체
      };

      // 1) 채팅창에 파일 말풍선 메시지 추가
      setChatMessages((prev) => [
        ...prev,
        {
          id: Date.now() + Math.random(),
          isMine: true,
          sender: { name: "나", avatar: "" },
          content: "",
          time: now,
          type: "file", // "text"가 아닌 "file"로 구분
          file: fileEntry,
        },
      ]);

      // 2) 오른쪽 패널 파일 목록 맨 위에 추가 (최신 파일이 위로)
      setSharedFiles((prev) => [fileEntry, ...prev]);
    });

    // input 초기화 - 같은 파일을 연속으로 선택할 수 있게
    e.target.value = "";
  }

  // 텍스트 메시지 전송 - 기존 setMessage("")만 하던 걸 실제 메시지 추가로 개선
  function handleSend() {
    if (!message.trim()) return; // 공백만 있으면 전송 못 하도록
    setChatMessages((prev) => [
      ...prev,
      {
        id: Date.now(),
        isMine: true,
        sender: { name: "나", avatar: "" },
        content: message.trim(),
        time: nowTime(),
        type: "text",
      },
    ]);
    setMessage(""); // 전송 후 입력창 비우기
  }

  // 오른쪽 패널 다운로드 버튼 클릭 시 동작
  function handleDownload(file) {
    const url = URL.createObjectURL(file.raw);
    const a = document.createElement("a");
    a.href = url;
    a.download = file.name;
    a.click();
    URL.revokeObjectURL(url);
  }

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

          {chatMessages.map((msg) => (
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
                {/* 파일이면 FileBubble, 텍스트면 기존 말풍선 */}
                {msg.type === "file" ? (
                  <FileBubble file={msg.file} />
                ) : (
                  <div
                    className={`${s.bubble} ${msg.isMine ? s.bubbleMine : ""}`}
                  >
                    {msg.content}
                  </div>
                )}
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
          <input
            ref={fileInputRef}
            type="file"
            multiple
            style={{ display: "none" }}
            // 숨겨둔 상태
            onChange={handleFileSelect}
          />
          <div className={s.inputRow}>
            <button
              className={s.inputBtn}
              type="button"
              onClick={() => fileInputRef.current?.click()}
              // 이벤트 추가
            >
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
                  handleSend();
                }
              }}
              className={s.input}
            />
            <button
              className={s.inputBtn}
              onClick={handleSend}
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

          <div className={s.infoPad}>
            <h3 className={s.infoTitle}>공유 파일</h3>
            {/* 파일 개수 동적으로 표시 */}
            <p className={s.infoSub}>
              {sharedFiles.length > 0
                ? `${sharedFiles.length}개 파일 업로드됨`
                : "아직 업로드된 파일이 없습니다."}
            </p>

            <div className={s.fileList}>
              {sharedFiles.length === 0 ? (
                <div className={s.fileEmpty}>
                  <Paperclip size={22} className={s.fileEmptyIcon} />
                  <p>
                    파일을 첨부하면 <br /> 여기에 표시됩니다.
                  </p>
                </div>
              ) : (
                sharedFiles.map((file) => {
                  const meta = getFileMeta(file.name);
                  return (
                    <div
                      key={file.id}
                      className={s.fileRow}
                      onClick={() => handleDownload(file)}
                      style={{ cursor: "pointer" }}
                      role="button"
                      aria-label={`${file.name} 다운로드`}
                    >
                      <div className={s.fileLeft}>
                        <div
                          className={s.fileIcon}
                          style={{ "--file-color": meta.color }}
                        >
                          {meta.label}
                        </div>
                        <div>
                          <p className={s.fileName}>{file.name}</p>
                          <p className={s.fileSize}>
                            {file.size} · {file.uploadedAt}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div> // info
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
