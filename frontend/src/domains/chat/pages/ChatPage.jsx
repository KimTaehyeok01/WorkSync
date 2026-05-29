import { useState, useRef, useEffect } from "react";
import useAuthContext from "../../../store/AuthContext";
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
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { WSEmptyState } from "../../../components/common/LayoutComponents";
import {
  getChatRoom,
  getEmployee,
  createChatRoom,
  getMember,
  getMessages,
  sendMessage,
  getMyInfo,
} from "../services/chatApi";
import s from "./ChatPage.module.css";

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
  return h < 12 ? `오전 ${h}:${m}` : `오후 ${h - 12 || 12}:${m}`;
}

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
function getFileMeta(filename) {
  const ext = getExt(filename);
  return (
    EXT_MAP[ext] || { color: "#6B7280", label: ext.toUpperCase().slice(0, 4) }
  );
}

function statusColor(status) {
  if (status === "ACTIVE") return "#48BB78";
  if (status === "AWAY") return "#F6AD55";
  return "#A0AEC0";
}

// 대화방 리스트
function ConvItem({ conv, active, onClick }) {
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

      {unread && <span className={s.unreadBadge}>{conv.unread}</span>}
    </button>
  );
}

// 새 대화 시작 모달창
function NewConvModal({ onClose, onCreate }) {
  const { accessToken } = useAuthContext();
  const [roomType, setRoomType] = useState("DIRECT");
  const [memberIds, setMemberIds] = useState([]);
  const [name, setName] = useState("");
  const [employee, setEmployee] = useState([]);

  // 직원 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getEmployee(accessToken).then((data) => {
      // console.log(data);
      setEmployee(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  function toggle(id) {
    setMemberIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }

  // 대화 추가 API
  async function handleCreate() {
    if (memberIds.length === 0) return;
    try {
      const data = { roomType, name, memberIds };
      await createChatRoom(accessToken, data).then((response) => {
        const roomName =
          roomType === "GROUP"
            ? name || `그룹 채팅 (${memberIds.length}명)`
            : employee.find((m) => m.id === memberIds[0])?.name || "";
        onCreate(roomName, memberIds);
      });
    } catch (error) {
      console.log("대화 추가 에러: " + error);
    }
    onClose();
  }

  const otherMembers = employee.filter((m) => m.id !== 4);

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
              { id: "DIRECT", label: "1:1 대화" },
              { id: "GROUP", label: "그룹 채팅" },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => {
                  setRoomType(t.id);
                  setMemberIds([]);
                }}
                className={`${s.tabBtn} ${roomType === t.id ? s.tabBtnActive : ""}`}
                type="button"
                aria-pressed={roomType === t.id}
              >
                {t.label}
              </button>
            ))}
          </div>

          {roomType === "GROUP" && (
            <input
              type="text"
              placeholder="그룹 이름 (선택사항)"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={s.groupNameInput}
            />
          )}

          <div>
            <p className={s.memberLabel}>
              {roomType === "DIRECT"
                ? "대화 상대 선택"
                : `참여 멤버 선택 (${memberIds.length}명)`}
            </p>
            <div className={s.memberList}>
              {otherMembers.map((m) => {
                const isSelected = memberIds.includes(m.id);
                const isDisabled =
                  roomType === "DIRECT" && memberIds.length > 0 && !isSelected;
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
                      <WSAvatar
                        src={m.profileImage}
                        name={m.name}
                        size={36}
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
                      <p className={s.memberRole}>
                        {JOB_GRADE[m.jobGrade] || "-"}
                      </p>
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
            disabled={memberIds.length === 0}
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
  const { accessToken } = useAuthContext();
  const [activeConvId, setActiveConvId] = useState(0);
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [showInfo, setShowInfo] = useState(true);
  const [showNewConvModal, setShowNewConvModal] = useState(false);
  const [conversation, setConversation] = useState([]);
  const [chatRoomId, setChatRoomId] = useState(0);
  const [teamMember, setTeamMember] = useState([]);
  const [chatMessages, setChatMessages] = useState([]);
  const [my, setMy] = useState([]);

  // 멤버 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !activeConvId) return;
    getMember(accessToken, activeConvId).then((data) => {
      setTeamMember(Array.isArray(data.data) ? data.data : []);
    });
  }, [activeConvId]);
  const TEAM_MEMBERS = Array.isArray(teamMember)
    ? teamMember.map((member) => ({
        employeeId: member.employeeId,
        name: member.name,
        jobGrade: member.jobGrade,
        profileImage: member.profileImage,
        status: member.status,
      }))
    : [];

  // 메시지 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !activeConvId) return;
    getMessages(accessToken, activeConvId).then((data) => {
      const list = Array.isArray(data.data) ? data.data : [];
      setChatMessages(
        list.map((msg) => ({
          id: msg.id,
          isMine: msg.senderId === my.id,
          sender: {
            name: msg.senderName,
            avatar: msg.senderProfileImage,
          },
          content: msg.content,
          time: msg.sentAt,
          type: msg.msgType,
        })),
      );
    });
  }, [accessToken, activeConvId]);

  // 내 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      setMy(data.data || {});
    });
  }, [accessToken]);

  // 대화 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getChatRoom(accessToken).then((data) => {
      setConversation(Array.isArray(data.data) ? data.data : []);
      setActiveConvId(data.data[0].id);
    });
  }, [accessToken]);
  const CONVERSATIONS = conversation.map((conv) => ({
    id: conv.id,
    roomType: conv.roomType,
    name: conv.name,
    thumbnailImage: conv.thumbnailImage,
    otherStatus: conv.otherStatus,
    members: TEAM_MEMBERS,
    lastMessage: conv.lastMessage,
    lastMessageAt: conv.lastMessageAt,
    unreadCount: conv.unreadCount,
    pinned: false,
  }));

  // 기존 MESSAGES 더미 + 이후 전송한 텍스트/파일 메시지 통합 관리
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
          id: Date.now(),
          isMine: my.id,
          sender: { name: my.name, avatar: my.profileImage },
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

  // 텍스트 메시지 전송 - POST 후 GET으로 메시지 목록 갱신
  async function handleSend() {
    if (!message.trim()) return;
    const content = message.trim();
    setMessage("");

    try {
      await sendMessage(accessToken, activeConvId, content);
      // 전송 완료 후 메시지 목록 재조회
      const data = await getMessages(accessToken, activeConvId);
      const list = Array.isArray(data.data) ? data.data : [];
      setChatMessages(
        list.map((msg) => ({
          id: msg.id,
          isMine: msg.senderId === my.id,
          sender: { name: msg.senderName, avatar: msg.senderProfileImage },
          content: msg.content,
          time: msg.sentAt,
          type: msg.msgType,
        })),
      );
    } catch (error) {
      console.log("메시지 전송 에러: " + error);
    }
  }

  const activeConv = CONVERSATIONS.find((c) => c.id === activeConvId);
  const filteredConvs = CONVERSATIONS.filter((c) => {
    const label = c.name || "";
    return label.toLowerCase().includes(search.toLowerCase());
  });

  const activeName = activeConv?.name;
  const activeAvatar =
    activeConv?.roomType === "DIRECT" ? activeConv?.thumbnailImage : null;
  const activeStatus =
    activeConv?.roomType === "DIRECT" ? activeConv?.otherStatus : null;

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
          {activeConv?.roomType === "DIRECT" ? (
            <div className={s.avatarWrap}>
              <WSAvatar src={activeAvatar} name={activeName} size={36} />
            </div>
          ) : (
            <div className={s.groupIcon}>
              <Users size={17} />
            </div>
          )}
          <div>
            <p className={s.chatHeaderName}>{activeName}</p>
            <p className={s.chatHeaderStatus}>
              {activeConv?.roomType === "DIRECT"
                ? activeStatus === "ACTIVE"
                  ? "온라인 · 활성"
                  : activeStatus === "AWAY"
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
                <WSAvatar
                  src={msg.sender.avatar}
                  name={msg.sender.name}
                  size={36}
                />
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
            {activeConv?.roomType === "GROUP" && (
              <div className={s.infomemberList}>
                <h3 className={s.memberTitle}>
                  구성원 ({activeConv?.members?.length}명)
                </h3>

                {activeConv?.members?.map((member) => (
                  <div key={member.employeeId} className={s.memberItem}>
                    <div className={s.avatarWrap}>
                      <WSAvatar
                        src={member.profileImage}
                        name={member.name}
                        size={36}
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
                      <p className={s.memberRole}>
                        {JOB_GRADE[member.jobGrade] || "-"}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className={s.infoPad}>
            <h3 className={s.infoTitle}>공유 파일</h3>
            {/* 파일 개수 동적으로 표시 */}

            <div className={s.fileList}>
              {sharedFiles.length === 0 ? (
                <WSEmptyState
                  title="아직 업로드된 파일이 없습니다."
                  icon={<Paperclip />}
                />
              ) : (
                sharedFiles.map((file) => {
                  const meta = getFileMeta(file.name);
                  return (
                    <>
                      <p className={s.infoSub}>
                        `${sharedFiles.length}개 파일 업로드됨`
                      </p>
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
                    </>
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
