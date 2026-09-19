-- 전자결재 회수 상태(WITHDRAWN) 추가를 위한 enum 값 확장
-- 주의: 이 프로젝트는 Flyway가 비활성화(spring.flyway.enabled: false) 상태라 이 파일은 자동 적용되지 않는다.
-- 사용자가 psql 등으로 아래 SQL을 DB에 직접 실행해야 한다.

ALTER TYPE approval_doc_status ADD VALUE IF NOT EXISTS 'WITHDRAWN';
