-- 캘린더 일정(schedule)과 자원 예약(resource, reservation) 테이블 추가
-- 주의: 이 프로젝트는 Flyway가 비활성화(spring.flyway.enabled: false) 상태라 이 파일은 자동 적용되지 않는다.
-- 사용자가 Supabase SQL Editor 등으로 아래 SQL을 DB에 직접 실행해야 한다.

CREATE TYPE schedule_scope_enum    AS ENUM ('PERSONAL', 'DEPARTMENT', 'COMPANY');
CREATE TYPE resource_category_enum AS ENUM ('MEETING_ROOM', 'VEHICLE', 'EQUIPMENT');

-- 19. schedule (캘린더 일정)
CREATE TABLE schedule (
    id            BIGSERIAL           PRIMARY KEY,
    owner_id      BIGINT              NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    department_id BIGINT              REFERENCES department(id) ON DELETE SET NULL,
    title         VARCHAR(100)        NOT NULL,
    description   TEXT,
    location      VARCHAR(100),
    start_at      TIMESTAMP           NOT NULL,
    end_at        TIMESTAMP           NOT NULL,
    all_day       BOOLEAN             NOT NULL DEFAULT FALSE,
    scope         schedule_scope_enum NOT NULL DEFAULT 'PERSONAL',
    created_at    TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP           NOT NULL DEFAULT NOW(),
    CHECK (end_at >= start_at)
);
CREATE INDEX idx_schedule_owner      ON schedule(owner_id);
CREATE INDEX idx_schedule_department ON schedule(department_id);
CREATE INDEX idx_schedule_period     ON schedule(start_at, end_at);

-- 20. resource (예약 대상 자원: 회의실·차량·비품)
CREATE TABLE resource (
    id          BIGSERIAL              PRIMARY KEY,
    name        VARCHAR(50)            NOT NULL,
    category    resource_category_enum NOT NULL DEFAULT 'MEETING_ROOM',
    location    VARCHAR(100),
    capacity    INT,
    description VARCHAR(255),
    active      BOOLEAN                NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP              NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP              NOT NULL DEFAULT NOW()
);

-- 21. reservation (자원 예약)
CREATE TABLE reservation (
    id          BIGSERIAL    PRIMARY KEY,
    resource_id BIGINT       NOT NULL REFERENCES resource(id) ON DELETE CASCADE,
    employee_id BIGINT       NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    title       VARCHAR(100) NOT NULL,
    start_at    TIMESTAMP    NOT NULL,
    end_at      TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CHECK (end_at > start_at)
);
CREATE INDEX idx_reservation_resource_period ON reservation(resource_id, start_at, end_at);
CREATE INDEX idx_reservation_employee        ON reservation(employee_id);
