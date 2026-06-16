import { useState, useRef, useEffect } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { log } from "sockjs-client/dist/sockjs";
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
  Download,
} from "lucide-react";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { WSEmptyState } from "../../../components/common/LayoutComponents";
import {
  getChatRoom,
  getMember,
  getMessages,
  sendMessage,
  readMessage,
  enterRoom,
  leaveRoom,
} from "../services/chatApi";
import { getMyInfo } from "../../../components/service/TopBarApi";
import { putNotifications } from "../../notification/services/notificationApi";
import {
  statusColor,
  JOB_GRADE,
  formatTime,
  formatDay,
  isToday,
} from "../components/chatUtil";
import { ConvItem } from "../components/ConvItem";
import { NewConvModal } from "../components/NewConvModal";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";
import { getSize, getFileMeta } from "../../file/components/fileUtil";
import { FileBubble } from "../components/FileBubble";
import s from "./ChatPage.module.css";

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

  // 파일 선언
  const {
    files,
    isDragging,
    setIsDragging,
    uploadedFile,
    uploadedFileRef,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "CHAT");

  // 기존 MESSAGES 더미 + 이후 전송한 텍스트/파일 메시지 통합 관리
  // sharedFiles : 오른쪽 패널에 보여줄 파일 목록
  const [sharedFiles, setSharedFiles] = useState([]);

  // 숨겨진 <input type="file">을 클립 버튼과 연결하기 위한 ref
  const fileInputRef = useRef(null);

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
                fileId: msg.fileId,
              },
            ];
          });
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [activeConvId, my.id]);

  // 실시간 대화 안읽음 뱃지
  useEffect(() => {
    if (!accessToken) return;

    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },

      // debug: (str) => {
      //   console.log("STOMP:", str);
      // },

      onConnect: () => {
        client.subscribe(`/user/queue/chat/unread`, (frame) => {
          const { roomId, unreadCount } = JSON.parse(frame.body);
          setConversation((prev) =>
            prev.map((conv) =>
              conv.id === roomId ? { ...conv, unreadCount: unreadCount } : conv,
            ),
          );
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [accessToken]);

  // 실시간 공유 파일
  useEffect(() => {
    if (!accessToken) return;

    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
      reconnectDelay: 5000,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },

      debug: (str) => {
        console.log("STOMP:", str);
      },

      onConnect: () => {
        console.log("연결");

        client.subscribe(`/topic/chat/${activeConvId}/files`, (frame) => {
          const file = JSON.parse(frame.body);
          setSharedFiles((prev) => [
            {
              id: file.id,
              originalName: file.originalName,
              fileSize: getSize(file.fileSize),
              filePath: file.filePath,
              createdAt: file.createdAt,
            },
            ...prev,
          ]);
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [accessToken, activeConvId]);

  // 새 메신저 아래로 스트롤 이동
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatMessages, sharedFiles]);

  // 내 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((data) => {
      setMy(data.data || {});
    });
  }, [accessToken]);

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

  // 대화 데이터 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getChatRoom(accessToken).then((data) => {
      setConversation(Array.isArray(data.data) ? data.data : []);
      setActiveConvId(data.data[0].id);
      setUnread(data.data.unreadCount);
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
          fileId: msg.fileId,
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
  }, [accessToken, activeConvId, my.id]);

  useEffect(() => {
    if (!accessToken || !activeConvId) return;
    // 채팅방 입/퇴장 처리
    enterRoom(accessToken, activeConvId);
    return () => {
      leaveRoom(accessToken, activeConvId);
    };
  }, [accessToken, activeConvId]);

  // 대화 리스트 클릭 시
  function handleConvClick(conv) {
    setActiveConvId(conv.id);
    setUnread(0);

    if (!accessToken) return;
    putNotifications(accessToken, {
      targetType: "CHAT",
      targetId: conv.id,
    });
  }

  // 기존 파일 목록 불러오기
  useEffect(() => {
    if (!accessToken || !activeConvId) return;

    getFile(accessToken, "CHAT", activeConvId).then((data) => {
      const list = Array.isArray(data.data) ? data.data : [];

      setSharedFiles(
        list.reverse().map((file) => {
          return {
            id: file.id,
            originalName: file.originalName,
            fileSize: getSize(file.fileSize),
            filePath: file.filePath,
            createdAt: file.createdAt,
          };
        }),
      );
    });
  }, [accessToken, activeConvId]);

  // 파일 선택 시 스토리지 업로드
  function handleFileSelect(files) {
    if (!files || files.length === 0) return;
    addFiles(files);
  }

  // 텍스트 메시지 전송 - POST 후 GET으로 메시지 목록 갱신
  async function handleSend() {
    const content = message.trim();
    if (!content && files.length === 0) return;
    setMessage("");

    try {
      // 전송만 하면 WebSocket 구독(/topic/room)으로 본인 포함 실시간 반영됨
      if (content) {
        await sendMessage(accessToken, activeConvId, content, "TEXT");
      }

      // DB에 파일 저장
      for (const uploaded of uploadedFileRef.current) {
        if (!uploaded?.filePath) continue;

        const saved = await saveFile(accessToken, {
          ...uploaded,
          refType: "CHAT",
          refId: activeConvId,
        });
        const fileId = saved?.data?.id;

        if (!fileId) {
          console.log("fileId 없음" + saved.data);
          continue;
        }

        await sendMessage(
          accessToken,
          activeConvId,
          uploaded.originalName,
          "FILE",
          fileId,
        );
      }

      // 파일 초기화
      clearFiles();
    } catch (error) {
      console.log("메시지 전송 에러: " + error);
    }
  }

  // 브라우저에서 직접 다운로드 - 백엔드 없이도 동작
  function handleDownload(file) {
    if (file.filePath) {
      // DB에서 불러온 파일 다운로드 (supabase URL)
      fetch(file.filePath)
        .then((res) => res.blob())
        .then((blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          a.download = file.originalName;
          a.click();
          URL.revokeObjectURL(url);
        });
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
                    {msg.type === "FILE" || msg.type === "file" ? (
                      <FileBubble
                        file={sharedFiles.find((f) => f.id === msg.fileId)}
                        onDownload={handleDownload}
                      />
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
          <input
            ref={fileInputRef}
            type="file"
            style={{ display: "none" }}
            // 숨겨둔 상태
            onChange={(e) => handleFileSelect(Array.from(e.target.files))}
          />
          {files.length > 0 && (
            // 파일 미리보기
            <div className={s.pendingFiles}>
              {files.map((file, index) => (
                <div key={index} className={`${s.pendingItem} ${s.inputRow}`}>
                  <div className={s.pendingText}>
                    <span>{file.file.name}</span>
                    <span>{getSize(file.file.size)}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      removeFiles(index);
                    }}
                  >
                    <X size={18} />
                  </button>
                </div>
              ))}
            </div>
          )}
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
              placeholder="메시지를 입력하세요. (Enter로 전송)"
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

            <div className={s.fileList}>
              {sharedFiles.length === 0 ? (
                <WSEmptyState
                  title="아직 업로드된 파일이 없습니다."
                  icon={<Paperclip />}
                />
              ) : (
                <>
                  {sharedFiles.length > 0 && (
                    <p className={s.infoSub}>
                      {sharedFiles.length}개 파일 업로드됨
                    </p>
                  )}
                  {sharedFiles.map((file, idx) => {
                    const meta = getFileMeta(file.originalName);
                    return (
                      <div
                        key={idx}
                        className={s.fileRow}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDownload(file);
                        }}
                        style={{ cursor: "pointer" }}
                        role="button"
                        aria-label={`${file.originalName} 다운로드`}
                      >
                        <div className={s.fileLeft}>
                          <div
                            className={s.fileIcon}
                            style={{ "--file-color": meta.color }}
                          >
                            {meta.label}
                          </div>
                          <div className={s.fileText}>
                            <p className={s.fileName}>{file.originalName}</p>
                            <p className={s.fileSize}>
                              {file.fileSize} · {formatTime(file.createdAt)}
                            </p>
                          </div>
                        </div>
                        <button
                          className={s.attachDl}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDownload(file);
                          }}
                        >
                          <Download size={18} />
                        </button>
                      </div>
                    );
                  })}
                </>
              )}
            </div>
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
