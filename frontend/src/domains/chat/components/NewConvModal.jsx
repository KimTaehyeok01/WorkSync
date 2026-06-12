import { useState, useEffect } from "react";
import { X, MessageSquare } from "lucide-react";
import { WSAvatar } from "../../../components/common/CommonWidgets";
import { getEmployee, createChatRoom } from "../services/chatApi";
import { statusColor, JOB_GRADE } from "./chatUtil";
import useAuthContext from "../../../store/AuthContext";
import s from "../pages/ChatPage.module.css";

// + 버튼 클릭 : 새 대화 시작 모달창
export function NewConvModal({ onClose, onCreate, my }) {
  const { accessToken } = useAuthContext();
  const [roomType, setRoomType] = useState("DIRECT");
  const [memberIds, setMemberIds] = useState([]);
  const [name, setName] = useState("");
  const [employee, setEmployee] = useState([]);

  // 전사 직원 불러오기
  useEffect(() => {
    if (!accessToken) return;
    getEmployee(accessToken).then((data) => {
      setEmployee(Array.isArray(data.data) ? data.data : []);
    });
  }, [accessToken]);

  //
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

  const otherMembers = employee.filter((m) => m.id !== my.id);

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
                      <WSAvatar src={m.profileImage} name={m.name} size={36} />
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
