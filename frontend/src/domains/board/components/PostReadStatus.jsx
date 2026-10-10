// 게시글 읽음 현황 카드 — 열람 시 읽음을 기록하고, 읽은 수·상단 고정·읽은 사람(작성자·관리자)을 보여준다
import { useState, useEffect } from "react";
import { Pin, PinOff } from "lucide-react";
import { WSCard, WSButton } from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { formatDateTime } from "../../../utils/dateFormatter";
import { markPostRead, getPostStatus, setPostPinned } from "../services/postExtraApi";
import s from "./PostExtras.module.css";

export default function PostReadStatus({ accessToken, postId }) {
  const [status, setStatus] = useState(null);

  const load = async () => {
    const json = await getPostStatus(accessToken, postId);
    setStatus(isOk(json) ? json.data : null);
  };

  // 글을 열면 먼저 읽음을 기록한 뒤 현황을 조회한다 (내 읽음이 수에 포함되도록)
  useEffect(() => {
    if (!accessToken || !postId) return;
    markPostRead(accessToken, Number(postId)).then(load);
  }, [accessToken, postId]);

  const togglePin = async () => {
    const json = await setPostPinned(accessToken, Number(postId), !status.isPinned);
    if (isOk(json)) load();
  };

  if (!status) return null;

  return (
    <WSCard
      title="읽음 확인"
      subtitle={`${status.readCount}명이 읽었습니다`}
      action={
        status.isPinnable && (
          <WSButton
            label={status.isPinned ? "고정 해제" : "상단 고정"}
            size="sm"
            variant="secondary"
            icon={status.isPinned ? <PinOff size={13} /> : <Pin size={13} />}
            onClick={togglePin}
          />
        )
      }
    >
      {status.readers.length === 0 ? (
        <p className={s.muted}>
          {status.isPinned ? "상단에 고정된 글입니다." : "읽은 사람 목록은 작성자와 관리자에게만 보입니다."}
        </p>
      ) : (
        <div className={s.readers}>
          {status.readers.map((reader) => (
            <div key={reader.employeeId} className={s.reader}>
              <span className={s.readerName}>
                {reader.name}
                {reader.departmentName && (
                  <span className={s.readerDept}>{reader.departmentName}</span>
                )}
              </span>
              <span className={s.readerTime}>{formatDateTime(reader.readAt)}</span>
            </div>
          ))}
        </div>
      )}
    </WSCard>
  );
}
