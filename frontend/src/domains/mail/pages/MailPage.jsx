// 사내 메일 페이지 — 왼쪽 메일함 메뉴, 오른쪽은 표 모양 목록과 상세가 전환되는 구성
import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import {
  PenSquare,
  Inbox,
  Send,
  Reply,
  ReplyAll,
  Forward,
  Trash2,
  Mail,
  ChevronLeft,
  ChevronRight,
  Check,
  Paperclip,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSAvatar,
  WSButton,
  WSEmptyState,
  WSSearchInput,
  WSFileList,
} from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { trackEvent } from "../../../utils/matomo";
import { formatDateTime, toDateString } from "../../../utils/dateFormatter";
import {
  getMyInfo,
  getEmployees,
  getMails,
  getMail,
  getUnreadMailCount,
  sendMail,
  deleteMail,
} from "../services/mailApi";
import { getFile, saveFile } from "../../file/services/fileApi";
import MailComposeModal from "../components/MailComposeModal";
import s from "./MailPage.module.css";

const BOXES = [
  { key: "inbox", label: "받은 메일함", icon: Inbox },
  { key: "sent", label: "보낸 메일함", icon: Send },
];

const EMPTY_LIST = { content: [], totalPages: 0, totalElements: 0 };

// 답장·전달 본문에 붙일 원문 인용
function quote(mail) {
  return `\n\n----- 원본 메일 -----\n보낸 사람: ${mail.sender.name}\n보낸 날짜: ${formatDateTime(mail.createdAt)}\n제목: ${mail.subject}\n\n${mail.content}`;
}

function withPrefix(prefix, subject) {
  return subject.startsWith(prefix) ? subject : `${prefix} ${subject}`;
}

// 목록의 날짜 — 오늘이면 시각만, 아니면 MM-DD HH:mm
function formatListDate(dateTime) {
  if (!dateTime) return "";
  if (dateTime.slice(0, 10) === toDateString(new Date())) {
    return dateTime.slice(11, 16);
  }
  return `${dateTime.slice(5, 10)} ${dateTime.slice(11, 16)}`;
}

export default function MailPage() {
  const { accessToken } = useAuthContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = searchParams.get("id");

  const [me, setMe] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [box, setBox] = useState("inbox");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [list, setList] = useState(EMPTY_LIST);
  const [listError, setListError] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [detail, setDetail] = useState(null);
  const [attachments, setAttachments] = useState([]);
  // draft: null(닫힘) | 작성 모달 초깃값
  const [draft, setDraft] = useState(null);

  const requestList = () =>
    getMails(accessToken, box, { page, unreadOnly, keyword });

  // 조회에 실패하면 "메일이 없습니다" 로 위장하지 않고 에러 상태를 보여준다 (보이던 목록은 그대로 둔다)
  const applyList = (json) => {
    if (!isOk(json)) {
      setListError(true);
      return;
    }
    const data = json.data ?? EMPTY_LIST;
    // 마지막 페이지의 마지막 메일을 지우는 등으로 현재 페이지가 비면 이전 페이지로 물러난다
    if (data.content.length === 0 && page > 0) {
      setPage(page - 1);
      return;
    }
    setList(data);
    setListError(false);
  };

  const loadList = async () => applyList(await requestList());

  // 받은 메일함 배지·목록 상단의 안 읽은 수 — 실패하면 보이던 값을 그대로 둔다
  const loadUnreadCount = async () => {
    const json = await getUnreadMailCount(accessToken);
    if (isOk(json)) setUnreadCount(json.data?.unreadCount ?? 0);
  };

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => setMe(json?.data ?? null));
    getEmployees(accessToken).then((json) =>
      setEmployees(Array.isArray(json?.data) ? json.data : []),
    );
    loadUnreadCount();
  }, [accessToken]);

  // 박스·검색이 빠르게 바뀌어도 늦게 도착한 이전 응답이 최신 목록을 덮어쓰지 않게 한다
  useEffect(() => {
    if (!accessToken) return undefined;
    let cancelled = false;
    requestList().then((json) => {
      if (!cancelled) applyList(json);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken, box, page, unreadOnly, keyword]);

  // 주소의 id 가 바뀌면 그 메일을 연다 (열면 서버에서 읽음 처리)
  useEffect(() => {
    if (!accessToken) return undefined;
    if (!selectedId) {
      setDetail(null);
      return undefined;
    }
    let cancelled = false;
    getMail(accessToken, selectedId).then((json) => {
      if (cancelled) return;
      setDetail(isOk(json) ? json.data : null);
      // 목록의 안 읽음 표시를 바로 지우고, 배지 숫자를 다시 센다
      setList((prev) => ({
        ...prev,
        content: prev.content.map((mail) =>
          String(mail.id) === String(selectedId) ? { ...mail, isRead: true } : mail,
        ),
      }));
      loadUnreadCount();
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken, selectedId]);

  // 열린 메일의 첨부파일 — 다른 메일로 넘어가면 이전 첨부를 비우고 새로 불러온다
  useEffect(() => {
    setAttachments([]);
    if (!accessToken || !detail?.id) return undefined;
    let cancelled = false;
    getFile(accessToken, "MAIL", detail.id).then((json) => {
      if (cancelled) return;
      setAttachments(Array.isArray(json?.data) ? json.data : []);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken, detail?.id]);

  const changeBox = (next) => {
    setBox(next);
    setPage(0);
    setSearchParams({});
  };

  // 메일을 먼저 보내고, 성공하면 그 메일 id 로 첨부파일을 저장한다
  const handleSend = async ({ files = [], ...mail }) => {
    const json = await sendMail(accessToken, mail);
    if (!isOk(json)) return json;
    trackEvent("mail", "send");

    const failedNames = [];
    for (const file of files) {
      const saved = await saveFile(accessToken, {
        originalName: file.originalName,
        filePath: file.filePath,
        fileSize: file.fileSize,
        mimeType: file.mimeType,
        refType: "MAIL",
        refId: json.data?.id,
      });
      if (!isOk(saved)) failedNames.push(file.originalName);
    }

    setDraft(null);
    loadList();
    loadUnreadCount();
    if (failedNames.length > 0) {
      alert(
        `메일은 보냈지만 다음 첨부파일을 저장하지 못했습니다: ${failedNames.join(", ")}`,
      );
    }
    return json;
  };

  const handleDelete = async () => {
    if (!confirm("이 메일을 내 메일함에서 삭제하시겠습니까?")) return;
    // 내가 보낸 메일을 보낸 메일함에서 보고 있으면 SENT, 그 외에는 받은 메일로 지운다
    const targetBox = box === "sent" && detail.isSentByMe ? "SENT" : "INBOX";
    const json = await deleteMail(accessToken, detail.id, targetBox);
    if (!isOk(json)) {
      alert(json?.message ?? "삭제에 실패했습니다.");
      return;
    }
    setSearchParams({});
    loadList();
    loadUnreadCount();
  };

  // 첨부파일 다운로드
  const handleDownload = async (file) => {
    try {
      const response = await fetch(file.url);
      // 오류 응답 본문(JSON·HTML)을 파일로 저장하지 않는다
      if (!response.ok) {
        alert("파일을 내려받지 못했습니다.");
        return;
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      const a = document.createElement("a");
      a.href = url;
      a.download = file.name;
      a.click();

      // 브라우저가 다운로드를 시작하기 전에 주소가 해제되지 않도록 잠깐 뒤에 해제한다
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      alert("파일을 내려받지 못했습니다.");
    }
  };

  const reply = (toAll) => {
    const others = detail.recipients.filter((r) => r.person.id !== me?.id);
    setDraft({
      toIds: toAll
        ? [...new Set([detail.sender.id, ...others.filter((r) => r.type === "TO").map((r) => r.person.id)])]
        : [detail.sender.id],
      ccIds: toAll ? others.filter((r) => r.type === "CC").map((r) => r.person.id) : [],
      subject: withPrefix("RE:", detail.subject),
      content: quote(detail),
    });
  };

  const forward = () =>
    setDraft({ subject: withPrefix("FW:", detail.subject), content: quote(detail) });

  const recipientsOf = (type) =>
    detail.recipients.filter((r) => r.type === type);

  const boxLabel = BOXES.find((b) => b.key === box).label;

  return (
    <div className={s.root}>
      {/* ── 왼쪽: 메일함 메뉴 ── */}
      <aside className={s.side}>
        <WSButton
          label="메일 쓰기"
          icon={<PenSquare size={15} />}
          onClick={() => setDraft({})}
          className={s.composeBtn}
        />
        <nav className={s.menu} aria-label="메일함">
          {BOXES.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              type="button"
              className={`${s.menuItem} ${box === key ? s.menuItemActive : ""}`}
              onClick={() => changeBox(key)}
            >
              <Icon size={16} />
              <span className={s.menuLabel}>{label}</span>
              {key === "inbox" && unreadCount > 0 && (
                <span className={s.badge}>{unreadCount > 99 ? "99+" : unreadCount}</span>
              )}
            </button>
          ))}
        </nav>
      </aside>

      {/* ── 오른쪽: 목록 ↔ 상세 ── */}
      <section className={s.main}>
        {detail ? (
          <>
            <div className={s.detailBar}>
              <button
                type="button"
                className={s.backBtn}
                onClick={() => setSearchParams({})}
                aria-label="목록으로"
              >
                <ChevronLeft size={16} />
                목록
              </button>
              <div className={s.detailButtons}>
                <WSButton label="답장" size="sm" variant="secondary" icon={<Reply size={14} />} onClick={() => reply(false)} />
                <WSButton label="전체 답장" size="sm" variant="secondary" icon={<ReplyAll size={14} />} onClick={() => reply(true)} />
                <WSButton label="전달" size="sm" variant="secondary" icon={<Forward size={14} />} onClick={forward} />
                <WSButton label="삭제" size="sm" variant="ghost" icon={<Trash2 size={14} />} onClick={handleDelete} />
              </div>
            </div>

            <div className={s.detailBody}>
              <h2 className={s.subject}>{detail.subject}</h2>

              <div className={s.meta}>
                <WSAvatar src={detail.sender.profileImage} name={detail.sender.name} size={40} />
                <div className={s.metaBody}>
                  <p className={s.metaSender}>
                    {detail.sender.name}
                    {detail.sender.departmentName && (
                      <span className={s.metaDept}>{detail.sender.departmentName}</span>
                    )}
                  </p>
                  <p className={s.metaLine}>
                    <span className={s.metaLabel}>받는 사람</span>
                    {recipientsOf("TO").map((r) => r.person.name).join(", ")}
                  </p>
                  {recipientsOf("CC").length > 0 && (
                    <p className={s.metaLine}>
                      <span className={s.metaLabel}>참조</span>
                      {recipientsOf("CC").map((r) => r.person.name).join(", ")}
                    </p>
                  )}
                </div>
                <span className={s.metaTime}>{formatDateTime(detail.createdAt)}</span>
              </div>

              {attachments.length > 0 && (
                <div className={s.attachments}>
                  <p className={s.attachTitle}>
                    <Paperclip size={13} />
                    첨부파일 {attachments.length}개
                  </p>
                  <WSFileList
                    files={attachments.map((f) => ({
                      name: f.originalName ?? "",
                      size: f.fileSize,
                      url: f.filePath,
                    }))}
                    onDownload={handleDownload}
                  />
                </div>
              )}

              <div className={s.content}>{detail.content}</div>

              {/* 내가 보낸 메일이면 수신자별 읽음 여부를 보여준다 */}
              {detail.isSentByMe && (
                <div className={s.readStatus}>
                  <p className={s.readStatusTitle}>
                    수신 확인 {detail.recipients.filter((r) => r.readAt).length}/
                    {detail.recipients.length}
                  </p>
                  <div className={s.readChips}>
                    {detail.recipients.map((r) => (
                      <span
                        key={r.person.id}
                        className={`${s.readChip} ${r.readAt ? s.readChipDone : ""}`}
                        title={r.readAt ? `${formatDateTime(r.readAt)} 읽음` : "아직 읽지 않음"}
                      >
                        {r.readAt && <Check size={12} />}
                        {r.person.name}
                        {r.type === "CC" && <span className={s.readChipCc}>참조</span>}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </>
        ) : (
          <>
            <div className={s.mainHeader}>
              <div className={s.titleGroup}>
                <h2 className={s.title}>{boxLabel}</h2>
                <span className={s.counts}>
                  {box === "inbox" && `안읽음 ${unreadCount} / `}전체 {list.totalElements}
                </span>
              </div>
              <div className={s.tools}>
                <div className={s.searchBox}>
                  <WSSearchInput
                    value={keyword}
                    placeholder="메일 검색"
                    onChange={(value) => {
                      setKeyword(value);
                      setPage(0);
                    }}
                  />
                </div>
                {box === "inbox" && (
                  <button
                    type="button"
                    className={`${s.filterChip} ${unreadOnly ? s.filterChipOn : ""}`}
                    onClick={() => {
                      setUnreadOnly(!unreadOnly);
                      setPage(0);
                    }}
                  >
                    안 읽음
                  </button>
                )}
              </div>
            </div>

            <div className={`${s.rowGrid} ${s.tableHead}`}>
              <span />
              <span>{box === "inbox" ? "보낸 사람" : "받는 사람"}</span>
              <span>제목</span>
              <span className={s.headDate}>날짜</span>
            </div>

            <div className={s.list}>
              {listError ? (
                <div className={s.listError}>
                  <p>메일을 불러오지 못했습니다.</p>
                  <WSButton
                    label="다시 시도"
                    size="sm"
                    variant="secondary"
                    onClick={loadList}
                  />
                </div>
              ) : list.content.length === 0 ? (
                <WSEmptyState icon={<Mail size={30} />} title="메일이 없습니다" />
              ) : (
                list.content.map((mail) => (
                  <button
                    key={mail.id}
                    type="button"
                    className={`${s.rowGrid} ${s.row} ${mail.isRead ? "" : s.rowUnread}`}
                    onClick={() => setSearchParams({ id: mail.id })}
                  >
                    <span className={s.cellMark}>
                      {!mail.isRead && <span className={s.unreadDot} title="안 읽음" />}
                    </span>
                    <span className={s.cellFrom}>
                      {box === "inbox" ? mail.sender.name : mail.recipientSummary}
                    </span>
                    <span className={s.cellSubject}>
                      <span className={s.subjectText}>{mail.subject}</span>
                      {mail.hasAttachment && (
                        <Paperclip size={13} className={s.clip} aria-label="첨부파일 있음" />
                      )}
                    </span>
                    <span className={s.cellDate}>{formatListDate(mail.createdAt)}</span>
                  </button>
                ))
              )}
            </div>

            {!listError && list.totalPages > 1 && (
              <div className={s.pager}>
                <button
                  type="button"
                  className={s.pagerBtn}
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                  aria-label="이전 페이지"
                >
                  <ChevronLeft size={15} />
                </button>
                <span>
                  {page + 1} / {list.totalPages}
                </span>
                <button
                  type="button"
                  className={s.pagerBtn}
                  disabled={page + 1 >= list.totalPages}
                  onClick={() => setPage(page + 1)}
                  aria-label="다음 페이지"
                >
                  <ChevronRight size={15} />
                </button>
              </div>
            )}
          </>
        )}
      </section>

      <MailComposeModal
        isOpen={!!draft}
        onClose={() => setDraft(null)}
        employees={employees}
        draft={draft}
        onSend={handleSend}
      />
    </div>
  );
}
