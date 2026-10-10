// 게시글 댓글 목록과 작성 입력
import { useState, useEffect } from "react";
import { Trash2 } from "lucide-react";
import { WSAvatar, WSButton } from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import { formatDateTime } from "../../../utils/dateFormatter";
import { getComments, createComment, deleteComment } from "../services/postExtraApi";
import s from "./PostExtras.module.css";

const MAX_LENGTH = 1000;

export default function PostComments({ accessToken, postId }) {
  const [comments, setComments] = useState([]);
  const [content, setContent] = useState("");
  const [error, setError] = useState("");

  const load = async () => {
    const json = await getComments(accessToken, postId);
    setComments(Array.isArray(json?.data) ? json.data : []);
  };

  useEffect(() => {
    if (!accessToken || !postId) return;
    load();
  }, [accessToken, postId]);

  const handleCreate = async () => {
    const json = await createComment(accessToken, Number(postId), content.trim());
    if (!isOk(json)) {
      setError(json?.message ?? "댓글을 등록하지 못했습니다.");
      return;
    }
    setContent("");
    setError("");
    load();
  };

  const handleDelete = async (id) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    const json = await deleteComment(accessToken, id);
    if (isOk(json)) load();
  };

  return (
    <div className={s.comments}>
      <h3 className={s.sectionTitle}>댓글 {comments.length}</h3>

      {comments.map((comment) => (
        <div key={comment.id} className={s.comment}>
          <WSAvatar src={comment.authorProfileImage} name={comment.authorName} size={30} />
          <div className={s.commentBody}>
            <p className={s.commentMeta}>
              <span className={s.commentAuthor}>{comment.authorName}</span>
              {formatDateTime(comment.createdAt)}
            </p>
            <p className={s.commentContent}>{comment.content}</p>
          </div>
          {comment.isDeletable && (
            <button
              type="button"
              className={s.iconBtn}
              onClick={() => handleDelete(comment.id)}
              aria-label="댓글 삭제"
            >
              <Trash2 size={14} />
            </button>
          )}
        </div>
      ))}

      <div className={s.commentForm}>
        <textarea
          className={s.commentInput}
          value={content}
          rows={2}
          maxLength={MAX_LENGTH}
          placeholder="댓글을 입력하세요"
          onChange={(e) => setContent(e.target.value)}
        />
        <WSButton
          label="등록"
          size="sm"
          disabled={content.trim().length === 0}
          onClick={handleCreate}
        />
      </div>
      {error && <p className={s.error}>{error}</p>}
    </div>
  );
}
