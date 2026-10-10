// 사내 메일 페이지 — 왼쪽 메일함 목록, 오른쪽 본문의 2단 구성
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
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import {
  WSAvatar,
  WSButton,
  WSEmptyState,
  WSSearchInput,
} from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { formatDateTime, formatRelative } from "../../../utils/dateFormatter";
import {
  getMyInfo,
  getEmployees,
  getMails,
  getMail,
  sendMail,
  deleteMail,
} from "../services/mailApi";
import MailComposeModal from "../components/MailComposeModal";
import s from "./MailPage.module.css";

const BOXES = [
  { key: "inbox", label: "받은편지함", icon: Inbox },
  { key: "sent", label: "보낸편지함", icon: Send },
];

// 답장·전달 본문에 붙일 원문 인용
function quote(mail) {
  return `\n\n----- 원본 메일 -----\n보낸 사람: ${mail.sender.name}\n보낸 날짜: ${formatDateTime(mail.createdAt)}\n제목: ${mail.subject}\n\n${mail.content}`;
}

function withPrefix(prefix, subject) {
  return subject.startsWith(prefix) ? subject : `${prefix} ${subject}`;
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
  const [list, setList] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [detail, setDetail] = useState(null);
  // draft: null(닫힘) | 작성 모달 초깃값
  const [draft, setDraft] = useState(null);

  const loadList = async () => {
    const json = await getMails(accessToken, box, { page, unreadOnly, keyword });
    setList(json?.data ?? { content: [], totalPages: 0, totalElements: 0 });
  };

  useEffect(() => {
    if (!accessToken) return;
    getMyInfo(accessToken).then((json) => setMe(json?.data ?? null));
    getEmployees(accessToken).then((json) =>
      setEmployees(Array.isArray(json?.data) ? json.data : []),
    );
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    loadList();
  }, [accessToken, box, page, unreadOnly, keyword]);

  // 주소의 id 가 바뀌면 그 메일을 연다 (열면 서버에서 읽음 처리)
  useEffect(() => {
    if (!accessToken) return;
    if (!selectedId) {
      setDetail(null);
      return;
    }
    getMail(accessToken, selectedId).then((json) => {
      setDetail(isOk(json) ? json.data : null);
      // 목록의 안 읽음 표시를 바로 지운다
      setList((prev) => ({
        ...prev,
        content: prev.content.map((mail) =>
          String(mail.id) === String(selectedId) ? { ...mail, isRead: true } : mail,
        ),
      }));
    });
  }, [accessToken, selectedId]);

  const changeBox = (next) => {
    setBox(next);
    setPage(0);
    setSearchParams({});
  };

  const handleSend = async (mail) => {
    const json = await sendMail(accessToken, mail);
    if (isOk(json)) {
      setDraft(null);
      loadList();
    }
    return json;
  };

  const handleDelete = async () => {
    if (!confirm("이 메일을 내 메일함에서 삭제하시겠습니까?")) return;
    // 내가 보낸 메일을 보낸편지함에서 보고 있으면 SENT, 그 외에는 받은 메일로 지운다
    const targetBox = box === "sent" && detail.isSentByMe ? "SENT" : "INBOX";
    const json = await deleteMail(accessToken, detail.id, targetBox);
    if (!isOk(json)) {
      alert(json?.message ?? "삭제에 실패했습니다.");
      return;
    }
    setSearchParams({});
    loadList();
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

  return (
    <div className={s.root}>
      {/* ── 왼쪽: 메일함 ── */}
      <aside className={`${s.listPane} ${detail ? s.listPaneHidden : ""}`}>
        <div className={s.listHeader}>
          <div className={s.boxTabs}>
            {BOXES.map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                type="button"
                className={`${s.boxTab} ${box === key ? s.boxTabActive : ""}`}
                onClick={() => changeBox(key)}
              >
                <Icon size={15} />
                {label}
              </button>
            ))}
          </div>
          <WSButton
            label="메일 쓰기"
            size="sm"
            icon={<PenSquare size={14} />}
            onClick={() => setDraft({})}
          />
        </div>

        <div className={s.listTools}>
          <WSSearchInput
            value={keyword}
            placeholder="제목 검색"
            onChange={(value) => {
              setKeyword(value);
              setPage(0);
            }}
          />
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

        <div className={s.list}>
          {list.content.length === 0 ? (
            <WSEmptyState icon={<Mail size={30} />} title="메일이 없습니다" />
          ) : (
            list.content.map((mail) => (
              <button
                key={mail.id}
                type="button"
                className={`${s.item} ${String(mail.id) === String(selectedId) ? s.itemActive : ""} ${mail.isRead ? "" : s.itemUnread}`}
                onClick={() => setSearchParams({ id: mail.id })}
              >
                <WSAvatar
                  src={box === "inbox" ? mail.sender.profileImage : null}
                  name={box === "inbox" ? mail.sender.name : mail.recipientSummary}
                  size={34}
                />
                <span className={s.itemBody}>
                  <span className={s.itemTop}>
                    <span className={s.itemName}>
                      {box === "inbox" ? mail.sender.name : mail.recipientSummary}
                    </span>
                    <span className={s.itemTime}>{formatRelative(mail.createdAt)}</span>
                  </span>
                  <span className={s.itemSubject}>{mail.subject}</span>
                  <span className={s.itemPreview}>{mail.preview}</span>
                </span>
                {!mail.isRead && <span className={s.unreadDot} />}
              </button>
            ))
          )}
        </div>

        {list.totalPages > 1 && (
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
      </aside>

      {/* ── 오른쪽: 본문 ── */}
      <section className={`${s.detailPane} ${detail ? "" : s.detailPaneEmpty}`}>
        {!detail ? (
          <WSEmptyState
            icon={<Mail size={34} />}
            title="메일을 선택하세요"
            description="왼쪽 목록에서 메일을 고르면 내용이 여기에 표시됩니다."
          />
        ) : (
          <>
            <div className={s.detailActions}>
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
