import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Download, Edit, ChevronDown, Pencil } from "lucide-react";
import {
  WSCard,
  WSAvatar,
  WSButton,
} from "../../../components/common/CommonWidgets";
import { WSFileList } from "../../../components/common/FormComponents";
import s from "./BoardDetailPage.module.css";
import {
  getPostById,
  deletePost,
  getMyInfo,
  getPosts,
} from "../services/boardApi";
import useAuthContext from "../../../store/AuthContext";
import useFileUpload from "../../../hooks/useFileUpload";
import { getFile, saveFile, deleteFile } from "../../file/services/fileApi";

const MOCK_ATTACHMENTS = [
  { id: "a1", name: "03_제내_수정.xlsx", size: "1.2 MB", type: "XLSX" },
];

export default function BoardDetail() {
  const { accessToken } = useAuthContext();
  const { boardId, postId } = useParams();
  const [allPosts, setAllPosts] = useState([]);
  const [post, setPost] = useState(null);
  const [me, setMe] = useState(null);
  const [attachments, setAttachments] = useState(MOCK_ATTACHMENTS);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  // 파일 선언
  const {
    files,
    setFiles,
    isDragging,
    setIsDragging,
    uploadedFile,
    addFiles,
    removeFiles,
    clearFiles,
  } = useFileUpload(accessToken, "POST", postId);

  useEffect(() => {
    if (!accessToken) return;
    setIsLoading(true); // 로딩 시작
    // api 모두 실행
    Promise.all([
      getPostById(boardId, postId, accessToken),
      getPosts(boardId, accessToken),
      getMyInfo(accessToken),
    ])
      .then(([postData, allPostsData, myData]) => {
        setPost(postData);
        setAllPosts(allPostsData);
        setMe(myData);
        setIsLoading(false); // api 호출 종료 후에 로딩 끝날 수 있도록
      })
      .catch((err) => {
        console.error("데이터 불러오기 실패: ", err);
        setIsLoading(false);
      });

    // 파일 데이터 불러오기
    getFile(accessToken, "POST", postId).then((data) => {
      const fileList = Array.isArray(data.data) ? data.data : [];
      // console.log(fileList);
      setFiles(
        fileList.map((f) => ({
          file: {
            name: f.originalName,
            size: f.fileSize,
          },
          url: f.filePath,
          refType: f.refType,
          refId: f.refId,
        })),
      );
    });
  }, [boardId, postId, accessToken]);
  console.log("post:", post);

  // 파일 다운로드
  const handleDownload = async (file, idx) => {
    const response = await fetch(file.url);
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = file.name;
    a.click();

    URL.revokeObjectURL(url);
  };

  // 현재 글의 위치 찾기
  const postIndex = allPosts.findIndex((p) => p.id === Number(postId));
  // 이전글
  const prevPost = postIndex > 0 ? allPosts[postIndex - 1] : null;
  // 다음글
  const nextPost =
    postIndex < allPosts.length - 1 ? allPosts[postIndex + 1] : null;

  if (isLoading) {
    return null;
  }

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
            <WSFileList
              files={files.map(({ file }) => file)}
              onDownload={handleDownload}
            />
          </WSCard>
          {me && post && (me.id === post.authorId || me.role === "ADMIN") && (
            <>
              <div className={s.actionsCol}>
                <WSButton
                  label="수정"
                  icon={<Pencil size={16} />}
                  variant="secondary"
                  onClick={() => navigate(`/board/edit/${boardId}/${postId}`)}
                  className={s.draftBtn}
                />
                <button
                  onClick={async () => {
                    if (confirm("게시글을 삭제하시겠습니까?")) {
                      await deletePost(boardId, postId, accessToken);
                      navigate("/board");
                    }
                  }}
                  className={s.cancelBtn}
                >
                  삭제하기
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {nextPost && (
        <button
          onClick={() => navigate(`/board/${boardId}/${nextPost.id}`)}
          className={s.nextBtn}
        >
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>다음글</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{nextPost.title}</span>
          </div>
          <span className={s.nextDate}>
            {new Date(nextPost.createdAt).toLocaleDateString("ko-KR")}
          </span>
        </button>
      )}
      {prevPost && (
        <button
          onClick={() => navigate(`/board/${boardId}/${prevPost.id}`)}
          className={s.nextBtn}
        >
          <div className={s.nextLeft}>
            <span className={s.nextLabel}>이전글</span>
            <ChevronDown size={14} className={s.nextArrow} />
            <span className={s.nextTitle}>{prevPost.title}</span>
          </div>
          <span className={s.nextDate}>
            {new Date(prevPost.createdAt).toLocaleDateString("ko-KR")}
          </span>
        </button>
      )}
    </div>
  );
}
