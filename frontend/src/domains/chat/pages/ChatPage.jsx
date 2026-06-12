import { useState, useRef, useEffect } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import useAuthContext from "../../../store/AuthContext";
import {
  Search,
  Send,
  Paperclip,
  MoreHorizontal,
  Plus,
  Users,
  CheckCheck,
} from "lucide-react";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import {
  getChatRoom,
  getMember,
  getMessages,
  sendMessage,
  readMessage,
} from "../services/chatApi";
import { getMyInfo } from "../../../components/service/TopBarApi";
import {
  statusColor,
  JOB_GRADE,
  formatTime,
  formatDay,
  isToday,
} from "../components/chatUtil";
import { ConvItem } from "../components/ConvItem";
import { NewConvModal } from "../components/NewConvModal";
import s from "./ChatPage.module.css";
import { putNotifications } from "../../notification/services/notificationApi";
import { log } from "sockjs-client/dist/sockjs";

export default function Messenger() {
  const { accessToken, myStatus } = useAuthContext();
  const [activeConvId, setActiveConvId] = useState(0);
  const [message, setMessage] = useState("");
  const [search, setSearch] = useState("");
  const [unread, setUnread] = useState(0);
  const [showInfo, setShowInfo] = useState(true);
  const [showNewConvModal, setShowNewConvModal] = useState(false);
  const [conversation, setConversation] = useState([]);
  const [teamMember, setTeamMember] = useState([]);
  const [chatMessages, setChatMessages] = useState([]);
  const [my, setMy] = useState([]);
  const bottomRef = useRef(null);

  // 새 메신저 아래로 스트롤 이동
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatMessages]);

  // // 채팅방 입장해 있을 때, 새 메시지가 오면 바로 읽음 처리
  // useEffect(() => {
  //   if (!targetId || targetId === 0) return;
  //   if (String(targetId) !== String(activeConvId)) return;

  //   if (targetId === activeConvId) {

  //   }
  // }, [targetId, activeConvId]);

  // 멤버 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !activeConvId) return;

    getMember(accessToken, activeConvId).then((data) => {
      setTeamMember(Array.isArray(data.data) ? data.data : []);
    });
  }, [activeConvId]);

  // 구성원 상태 실시간 구독 — 누군가 온라인/자리비움 변경 시 점 즉시 갱신
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        client.subscribe("/topic/status", (frame) => {
          const data = JSON.parse(frame.body);

          setTeamMember((prev) =>
            prev.map((m) =>
              m.employeeId === data.employeeId
                ? { ...m, status: data.status }
                : m,
            ),
          );
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, []);

  // WebSocket 메시지 실시간 구독 — 채팅방 입장 시 해당 방 토픽 구독, 나가면 해제
  useEffect(() => {
    if (!activeConvId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        client.subscribe(`/topic/room/${activeConvId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          console.log(msg);

          setChatMessages((prev) => {
            const exists = prev.some((m) => m.id === msg.id);
            // 이미 있는 메시지면 무시 (중복 방지)
            if (exists) return prev;
            return [
              ...prev,
              {
                id: msg.id,
                isMine: msg.senderId === my.id,
                sender: {
                  name: msg.senderName,
                  avatar: msg.senderProfileImage,
                },
                content: msg.content,
                time: msg.sentAt,
                type: msg.msgType,
              },
            ];
          });
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [activeConvId, my]);
  const TEAM_MEMBERS = Array.isArray(teamMember)
    ? teamMember.map((member) => ({
        employeeId: member.employeeId,
        name: member.name,
        jobGrade: member.jobGrade,
        profileImage: member.profileImage,
        status: member.status,
      }))
    : [];

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
  }, [accessToken, showNewConvModal]);
  const CONVERSATIONS = conversation.map((conv) => ({
    id: conv.id,
    roomType: conv.roomType,
    name: conv.name,
    thumbnailImage: conv.thumbnailImage,
    otherStatus: conv.otherStatus,
    members: TEAM_MEMBERS,
    lastMessage: conv.lastMessage,
    lastMessageAt: formatTime(conv.lastMessageAt),
    unreadCount: conv.unreadCount,
    pinned: false,
  }));

  // 메시지 데이터 불러오기
  useEffect(() => {
    if (!accessToken || !activeConvId) return;

    getMessages(accessToken, activeConvId).then((data) => {
      const list = Array.isArray(data.data) ? data.data : [];

      setChatMessages(
        list.reverse().map((msg) => ({
          id: msg.id,
          isMine: msg.senderId === my.id ? true : false,
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

    // 메시지 읽음 처리
    readMessage(accessToken, activeConvId).then((data) => {
      setConversation((prev) =>
        prev.map((msg) =>
          msg.id === activeConvId ? { ...msg, unreadCount: 0 } : msg,
        ),
      );
    });
  }, [accessToken, activeConvId, my]);

  // 대화 리스트 클릭 시
  function handleConvClick(conv) {
    setActiveConvId(conv.id);
    setUnread(conv.unreadCount);
  }

  // 기존 MESSAGES 더미 + 이후 전송한 텍스트/파일 메시지 통합 관리
  // sharedFiles : 오른쪽 패널에 보여줄 파일 목록
  // const [sharedFiles, setSharedFiles] = useState([]);

  // 숨겨진 <input type="file">을 클립 버튼과 연결하기 위한 ref
  // const fileInputRef = useRef(null);

  // 파일 선택 시 실행 - 채팅 말풍선 + 오른쪽 목록 동시 업데이트
  // function handleFileSelect(e) {
  //   const files = Array.from(e.target.files); // FileList -> 배열로 변환
  //   if (files.length === 0) return; // 선택 취소 시 아무것도 안함
  //   const now = formatTime(new Date().toISOString());

  //   files.forEach((file) => {
  //     // 파일 정보 객체 생성
  //     const fileEntry = {
  //       id: Date.now() + Math.random(), // 고유 key용 임시 id
  //       name: file.name,
  //       size: formatSize(file.size), // "1.2MB 형식으로 변환"
  //       ext: getExt(file.name),
  //       uploadAt: now,
  //       raw: file, //다운로드를 위해 원본 File 객체
  //     };

  //     // 1) 채팅창에 파일 말풍선 메시지 추가
  //     setChatMessages((prev) => [
  //       ...prev,
  //       {
  //         id: Date.now(),
  //         isMine: my.id,
  //         sender: { name: my.name, avatar: my.profileImage },
  //         content: "",
  //         time: now,
  //         type: "file", // "text"가 아닌 "file"로 구분
  //         file: fileEntry,
  //       },
  //     ]);

  //     // 2) 오른쪽 패널 파일 목록 맨 위에 추가 (최신 파일이 위로)
  //     setSharedFiles((prev) => [fileEntry, ...prev]);
  //   });

  //   // input 초기화 - 같은 파일을 연속으로 선택할 수 있게
  //   e.target.value = "";
  // }

  // 텍스트 메시지 전송 - POST 후 GET으로 메시지 목록 갱신
  async function handleSend() {
    if (!message.trim()) return;
    const content = message.trim();
    setMessage("");

    try {
      // 전송만 하면 WebSocket 구독(/topic/room)으로 본인 포함 실시간 반영됨
      await sendMessage(accessToken, activeConvId, content);
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
              placeholder="이름으로 검색하세요."
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
                  onClick={() => {
                    handleConvClick(conv);
                  }}
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
          {chatMessages.map((msg, index) => {
            // 이전 메시지와 날짜가 다를때만 날짜 표시
            const prevMsg = chatMessages[index - 1];
            const showDate =
              !prevMsg || formatDay(msg.time) !== formatDay(prevMsg.time);

            return (
              <div key={msg.id}>
                {showDate && (
                  <div className={s.dateRow}>
                    <div className={s.dateLine} />
                    <span className={s.dateLabel}>
                      {isToday(msg.time)
                        ? `오늘, ${formatDay(msg.time)}`
                        : `${formatDay(msg.time)}`}
                    </span>
                    <div className={s.dateLine} />
                  </div>
                )}

                <div className={`${s.msg} ${msg.isMine ? s.msgMine : ""}`}>
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
                      <span className={s.msgTime}>{formatTime(msg.time)}</span>
                      {msg.unreadCount && (
                        <CheckCheck size={12} color="#60A5FA" />
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={bottomRef} />
        </div>

        <div className={s.inputBar}>
          {/* <input
            ref={fileInputRef}
            type="file"
            multiple
            style={{ display: "none" }}
            // 숨겨둔 상태
            onChange={handleFileSelect}
          /> */}
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
                      />
                      <span
                        className={s.statusDot}
                        style={{
                          // 본인 멤버는 전역 myStatus로 즉시 반영, 나머지는 서버값
                          "--status-color": statusColor(
                            member.employeeId === my.id && myStatus
                              ? myStatus
                              : member.status,
                          ),
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

            {/* <div className={s.fileList}>
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
            </div> */}
          </div>
        </div> // info
      )}

      {showNewConvModal && (
        <NewConvModal
          onClose={() => setShowNewConvModal(false)}
          onCreate={() => setShowNewConvModal(false)}
          my={my}
        />
      )}
    </div>
  );
}
