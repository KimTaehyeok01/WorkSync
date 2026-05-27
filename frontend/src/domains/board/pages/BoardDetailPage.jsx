import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Download, Edit, ChevronDown, Pencil } from "lucide-react";
import {
  WSCard,
  WSAvatar,
  WSButton,
} from "../../../components/common/CommonWidgets";
import s from "./BoardDetailPage.module.css";
import { getPostById } from "../services/boardApi";

const MOCK_ATTACHMENTS = [
  { id: "a1", name: "03_제내_수정.xlsx", size: "1.2 MB", type: "XLSX" },
];

export default function BoardDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [attachments, setAttachments] = useState(MOCK_ATTACHMENTS);

  useEffect(() => {
    const token = localStorage.getItem("refreshToken");
    getPostById(2, id, token).then((data) => {
      setPost(data);
    });
  }, [id]);

  // 다음 글
  // const nextPost =
  //   postIndex < BOARD_POSTS.length - 1 ? BOARD_POSTS[postIndex + 1] : null;

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
  function handleDeleteAttachment(attachmentId) {
    if (confirm("첨부파일을 삭제하시겠습니까?")) {
      setAttachments((prev) => prev.filter((a) => a.id !== attachmentId));
    }
  }

  return (
    <div className={s.root}>
      <div className={s.header}>
        <div className={s.headerLeft}>
          <button onClick={() => navigate("/board")} className={s.backBtn}>
            <ArrowLeft size={16} />
          </button>
          <div>
            <h1 className={s.pageTitle}>{post.boardName || "게시판"}</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={s.colMain}>
          <div className={s.contentCard}>
            <h1 className={s.title}>{post.title}</h1>
            <div className={s.metaRow}>
              <WSAvatar src={null} name={post.authorName} size={32} />
              <div>
                <span className={s.metaName}>{post.authorName}</span>
                <span className={s.metaDate}>
                  {post.createdAt?.slice(0, 10)}
                </span>
              </div>
            </div>
            <div className={s.content}>{post.content}</div>
          </div>
        </div>

        <div className={s.colSide}>
          <WSCard title="첨부 파일" subtitle="1개 파일 업로드">
            <div className={s.attachRow}>
              <div className={s.attachLeft}>
                <div className={s.attachIcon}>XLSX</div>
                <div>
                  <p className={s.attachName}>Q3_예산_요청.xlsx</p>
                  <p className={s.attachSize}>1.2 MB</p>
                </div>
              </div>
              <button className={s.attachDl}>
                <Download size={18} />
              </button>
            </div>
          </WSCard>

          <div className={s.actionsCol}>
            <WSButton
              label="수정"
              icon={<Pencil size={16} />}
              variant="secondary"
              onClick={() => navigate(`/board/edit/${id}`)}
              className={s.draftBtn}
            />
            <button
              onClick={() => {
                if (confirm("업무를 삭제하시겠습니까?")) navigate("/boad");
              }}
              className={s.cancelBtn}
            >
              삭제하기
            </button>
          </div>
        </div>
      </div>

      {/* 다음 글 */}
      {/* {nextPost && (
        <button
          onClick={() => navigate(`/board/${nextPost.id}`)}
          className={s.nextBtn}
        >
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>다음글</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{nextPost.title}</span>
          </div>
          <span className={s.nextDate}>{nextPost.createdAt}</span>
        </button>
      )} */}
    </div>
  );
}
