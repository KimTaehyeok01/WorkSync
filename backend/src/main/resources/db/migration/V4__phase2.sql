-- 2차 확장: 사내 메일, 알림 유형·수신 설정, 게시판 고정·읽음·댓글, 반복 일정·참석자, 인사 발령 이력
-- 주의: 이 프로젝트는 Flyway가 비활성화(spring.flyway.enabled: false) 상태라 이 파일은 자동 적용되지 않는다.
-- 사용자가 Supabase SQL Editor 등으로 아래 SQL을 DB에 직접 실행해야 한다.
-- 모든 구문은 여러 번 실행해도 안전하다(IF NOT EXISTS).
-- 이 브랜치의 서버를 띄우기 전에 반드시 먼저 적용할 것 — 알림 발송이 notification_setting 을 조회한다.

-- 알림 유형 추가
ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'MAIL';
ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'SCHEDULE';
ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'RESERVATION';
ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'BOARD';

-- 22. mail (사내 메일 본문 — 보낸 사람 기준 1건)
CREATE TABLE IF NOT EXISTS mail (
    id             BIGSERIAL    PRIMARY KEY,
    sender_id      BIGINT       NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    subject        VARCHAR(200) NOT NULL,
    content        TEXT         NOT NULL,
    sender_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mail_sender ON mail(sender_id, created_at DESC);

-- 23. mail_recipient (수신자별 상태 — 받는 사람 TO / 참조 CC)
CREATE TABLE IF NOT EXISTS mail_recipient (
    id             BIGSERIAL   PRIMARY KEY,
    mail_id        BIGINT      NOT NULL REFERENCES mail(id) ON DELETE CASCADE,
    recipient_id   BIGINT      NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    recipient_type VARCHAR(10) NOT NULL CHECK (recipient_type IN ('TO', 'CC')),
    read_at        TIMESTAMP,
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (mail_id, recipient_id)
);
CREATE INDEX IF NOT EXISTS idx_mail_recipient_inbox ON mail_recipient(recipient_id, deleted, created_at DESC);

-- 24. notification_setting (알림 수신 설정 — 행이 없으면 수신)
CREATE TABLE IF NOT EXISTS notification_setting (
    id          BIGSERIAL   PRIMARY KEY,
    employee_id BIGINT      NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (employee_id, type)
);

-- 25. post_pin (게시글 상단 고정)
CREATE TABLE IF NOT EXISTS post_pin (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL UNIQUE REFERENCES post(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 26. post_read (게시글 읽음 확인)
CREATE TABLE IF NOT EXISTS post_read (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT    NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    employee_id BIGINT    NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, employee_id)
);

-- 27. post_comment (게시글 댓글)
CREATE TABLE IF NOT EXISTS post_comment (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    author_id  BIGINT    NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_post_comment_post ON post_comment(post_id, created_at);

-- schedule 반복 설정 (NONE / DAILY / WEEKLY / MONTHLY, repeat_until 까지 반복)
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS repeat_type  VARCHAR(10) NOT NULL DEFAULT 'NONE';
ALTER TABLE schedule ADD COLUMN IF NOT EXISTS repeat_until DATE;

-- 28. schedule_attendee (일정 참석자)
CREATE TABLE IF NOT EXISTS schedule_attendee (
    id          BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT    NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
    employee_id BIGINT    NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (schedule_id, employee_id)
);
CREATE INDEX IF NOT EXISTS idx_schedule_attendee_employee ON schedule_attendee(employee_id);

-- 29. employee_history (인사 발령 이력)
CREATE TABLE IF NOT EXISTS employee_history (
    id           BIGSERIAL    PRIMARY KEY,
    employee_id  BIGINT       NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    change_type  VARCHAR(20)  NOT NULL,
    before_value VARCHAR(100),
    after_value  VARCHAR(100),
    actor_id     BIGINT,
    actor_name   VARCHAR(50),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_employee_history_employee ON employee_history(employee_id, created_at DESC);

-- Supabase 공개 API(anon 키)로 직접 접근하지 못하도록 RLS 활성화 (백엔드는 소유자 계정이라 영향 없음)
ALTER TABLE mail                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE mail_recipient       ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_setting ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_pin             ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_read            ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_comment         ENABLE ROW LEVEL SECURITY;
ALTER TABLE schedule_attendee    ENABLE ROW LEVEL SECURITY;
ALTER TABLE employee_history     ENABLE ROW LEVEL SECURITY;
