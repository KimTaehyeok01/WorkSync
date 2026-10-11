// 여러 화면에서 함께 쓰는 표시용 라벨 모음

export const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

export const JOB_GRADE_LABEL = {
  STAFF: "사원",
  SENIOR: "주임",
  ASSISTANT_MANAGER: "대리",
  MANAGER: "과장",
  GENERAL_MANAGER: "부장",
  DIRECTOR: "이사",
  VICE_PRESIDENT: "부사장",
  PRESIDENT: "사장",
  CEO: "대표이사",
};

export const ROLE_LABEL = {
  USER: "일반",
  ADMIN: "관리자",
};

// badge 값은 WSBadge 의 status 로 넘긴다
export const ATTENDANCE_STATUS = {
  NORMAL: { label: "정상", badge: "approved" },
  LATE: { label: "지각", badge: "pending" },
  EARLY_LEAVE: { label: "조퇴", badge: "pending" },
  ABSENT: { label: "결근", badge: "rejected" },
};

export const LEAVE_TYPE_LABEL = {
  ANNUAL: "연차",
  HALF: "반차",
  SICK: "병가",
  FAMILY: "경조사",
  OTHER: "기타",
};

export const LEAVE_STATUS = {
  PENDING: { label: "결재 대기", badge: "pending" },
  APPROVED: { label: "승인", badge: "approved" },
  REJECTED: { label: "반려", badge: "rejected" },
};

export const TASK_STATUS_LABEL = {
  TODO: "대기중",
  IN_PROGRESS: "진행중",
  DONE: "완료",
};

export const APPROVAL_STATUS_LABEL = {
  IN_PROGRESS: "진행중",
  APPROVED: "승인",
  REJECTED: "반려",
  WITHDRAWN: "회수",
};

export const NOTIFICATION_TYPE_LABEL = {
  APPROVAL: "전자결재",
  TASK: "업무",
  MESSAGE: "메신저",
  MAIL: "메일",
  SCHEDULE: "일정",
  RESERVATION: "예약",
  BOARD: "게시판",
};
