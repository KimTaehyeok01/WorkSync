import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Download, Pencil } from "lucide-react";
import { KANBAN_TASKS } from "../../../constants/mockData";
import {
  WSAvatar,
  WSButton,
  WSCard,
  WSProgress,
} from "../../../components/common/CommonWidgets";
import s from "./TaskDetailPage.module.css";

export default function TaskDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const allTasks = [
    ...KANBAN_TASKS.todo.map((t) => ({ ...t, status: "todo" })),
    ...KANBAN_TASKS.inProgress.map((t) => ({ ...t, status: "inProgress" })),
    ...KANBAN_TASKS.review.map((t) => ({ ...t, status: "review" })),
    ...KANBAN_TASKS.done.map((t) => ({ ...t, status: "done" })),
  ];

  const task = allTasks.find((t) => t.id === id);

  if (!task) {
    return (
      <div className={s.notFound}>
        <p>업무를 찾을 수 없습니다</p>
      </div>
    );
  }

  return (
    <div>
      <div className={s.header}>
        <button onClick={() => navigate("/tasks")} className={s.backBtn}>
          <ArrowLeft size={16} />
        </button>
        <div>
          <div className={s.titleRow}>
            <h1 className={s.pageTitle}>업무 상세</h1>
          </div>
        </div>
      </div>

      <div className={s.layout}>
        <div className={s.colMain}>
          <WSCard>
            <div className={s.infoGrid}>
              <div className={s.ColWrapper}>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>프로젝트명</p>
                  <p className={s.infoValue}>그룹웨어 시스템 구축</p>
                </div>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>기간</p>
                  <p className={s.infoValue}>
                    {task.startDate} ~ {task.endDate}
                  </p>
                </div>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>진행률</p>
                  <div className={s.infoValue}>
                    <WSProgress value={task.progress} />
                  </div>
                </div>
                <div className={s.infoCol}>
                  <p className={s.infoLabel}>담당자</p>
                  <div className={s.infoValue}>
                    <div className={s.assigneeValue}>
                      <WSAvatar
                        src={task.assignee.avatar}
                        name={task.assignee.name}
                        size={24}
                      />
                      <span>
                        {task.assignee.name} ({task.assignee.dept},{" "}
                        {task.assignee.role})
                      </span>
                    </div>
                  </div>
                  <p className={s.infoLabel}>작성자</p>
                  <div className={s.infoValue}>
                    <div className={s.assigneeValue}>
                      <WSAvatar
                        src={task.assignee.avatar}
                        name={task.assignee.name}
                        size={24}
                      />
                      <span>
                        {task.assignee.name} ({task.assignee.dept},{" "}
                        {task.assignee.role})
                      </span>
                    </div>
                  </div>
                  <p className={s.infoLabel}>작성일</p>
                  <p className={s.infoValue}>2026-05-01</p>
                </div>
              </div>
              <div className={s.details}>
                <div className={s.detailLines}>
                  <p>1. 금일 수행 업무</p>
                  <p>1) 전자결재 화면 UI 개발</p>
                  <p>- 결재선 지정 모달 구현</p>
                  <p>- 승인/반려 버튼 상태 처리</p>
                  <p>- 모바일 반응형 레이아웃 수정</p>
                  <p>2) API 연동</p>
                  <p>- 결재 목록 조회 API 연결</p>
                  <p>- Axios 인터셉터 토큰 처리 추가</p>
                  <p>- 오류 응답 공통 핸들링 적용</p>
                  <p>3) DB 설계 검토</p>
                  <p>- 문서 데이터 승인상태 컬럼 추가</p>
                  <p>- 결재 이력 테이블 인덱스 추가 검토</p>
                  <p>2. 이슈 사항</p>
                  <p>- 결재선 데이터 구조와 프론트 상태값 불일치 발생</p>
                  <p>- Safari 브라우저에서 모달 스크롤 오류 확인</p>
                  <p>- QA 환경 API 응답 속도 지연 현상 존재</p>
                </div>
              </div>
            </div>
          </WSCard>
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
              onClick={() => navigate(`/tasks/edit/${id}`)}
              className={s.draftBtn}
            />
            <button
              onClick={() => {
                if (confirm("업무를 삭제하시겠습니까?")) navigate("/tasks");
              }}
              className={s.cancelBtn}
            >
              삭제하기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
