-- 부서에 상위부서(parent_id)를 추가해 다단계 조직도를 지원한다. 상위 부서가 삭제되면 하위는 최상위로 승격된다.
-- 주의: spring.flyway.enabled=false 라 자동 적용되지 않는다. 이 엔티티 변경이 담긴 백엔드를 배포하기 전에 DB에서 먼저 직접 실행할 것.
ALTER TABLE department
    ADD COLUMN parent_id BIGINT REFERENCES department(id) ON DELETE SET NULL;

CREATE INDEX idx_department_parent ON department(parent_id);
