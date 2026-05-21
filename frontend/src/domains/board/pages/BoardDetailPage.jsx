import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Download, Edit, ChevronDown } from "lucide-react";
import { BOARD_POSTS } from "../../../constants/mockData";
import { WSCard, WSAvatar } from "../../../components/common/CommonWidgets";
import s from "./BoardDetailPage.module.css";

const CATEGORY_LABELS = {
  notice: "공지 사항",
  dept: "부서 게시판",
  free: "자유 게시판",
};

const MOCK_ATTACHMENTS = [
  { id: "a1", name: "03_제내_수정.xlsx", size: "1.2 MB", type: "XLSX" },
];

export default function BoardDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const postIndex = BOARD_POSTS.findIndex((p) => p.id === id);
  const post = BOARD_POSTS[postIndex];

  const [attachments, setAttachments] = useState(MOCK_ATTACHMENTS);

  if (!post) {
    return (
      <div className={s.notFound}>
        <p className={s.notFoundTitle}>게시글을 찾을 수 없습니다</p>
        <p className={s.notFoundDesc}>삭제되었거나 잘못된 주소입니다.</p>
        <button onClick={() => navigate("/board")} className={s.notFoundBtn}>
          <ArrowLeft size={14} /> 게시판으로 돌아가기
        </button>
      </div>
    );
  }

  const nextPost = postIndex < BOARD_POSTS.length - 1 ? BOARD_POSTS[postIndex + 1] : null;

  function handleDeleteAttachment(attachmentId) {
    if (confirm("첨부파일을 삭제하시겠습니까?")) {
      setAttachments((prev) => prev.filter((a) => a.id !== attachmentId));
    }
  }

  return (
    <div className={s.root}>
      <button onClick={() => navigate("/board")} className={s.backBtn}>
        <ArrowLeft size={18} />
        {CATEGORY_LABELS[post.category] || "게시판"}
      </button>

      <div className={s.layout}>
        <div className={s.colMain}>
          <div className={s.contentCard}>
            <h1 className={s.title}>{post.title}</h1>
            <div className={s.metaRow}>
              <WSAvatar src={post.author.avatar} name={post.author.name} size={32} />
              <div>
                <span className={s.metaName}>{post.author.name}</span>
                <span className={s.metaDate}>{post.createdAt}</span>
              </div>
            </div>
            <div className={s.content}>{post.content}</div>
          </div>
        </div>

        <div className={s.colSide}>
          <WSCard>
            <h3 className={s.sideTitle}>첨부 파일</h3>
            <p className={s.sideSub}>{attachments.length}개 파일 첨부됨</p>

            <div className={s.fileList}>
              {attachments.map((file) => (
                <div key={file.id} className={s.fileRow}>
                  <div className={s.fileIcon}>XLSX</div>
                  <div className={s.fileBody}>
                    <p className={s.fileName}>{file.name}</p>
                    <p className={s.fileSize}>{file.size}</p>
                  </div>
                  <button className={s.fileDl} title="다운로드">
                    <Download size={16} />
                  </button>
                </div>
              ))}
            </div>

            <div className={s.actionsCol}>
              <button className={s.editBtn}>
                <Edit size={14} /> 수정
              </button>
              <button
                onClick={() => {
                  if (attachments.length > 0) handleDeleteAttachment(attachments[0].id);
                }}
                className={s.delBtn}
              >
                삭제하기
              </button>
            </div>
          </WSCard>
        </div>
      </div>

      {nextPost && (
        <button onClick={() => navigate(`/board/${nextPost.id}`)} className={s.nextBtn}>
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>다음글</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{nextPost.title}</span>
          </div>
          <span className={s.nextDate}>{nextPost.createdAt}</span>
        </button>
      )}
    </div>
  );
}
