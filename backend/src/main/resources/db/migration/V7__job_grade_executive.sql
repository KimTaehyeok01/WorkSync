-- 직급 enum 에 부사장·사장을 추가한다 (순서: DIRECTOR < VICE_PRESIDENT < PRESIDENT < CEO).
-- 주의: spring.flyway.enabled=false 라 자동 적용되지 않는다. 백엔드 배포 전에 DB에서 직접 실행하고,
-- 새 enum 값은 이 문장을 커밋한 뒤에야 사용할 수 있으므로 데이터 갱신 SQL 과 같은 트랜잭션에서 실행하지 말 것.
ALTER TYPE job_grade_type ADD VALUE IF NOT EXISTS 'VICE_PRESIDENT' BEFORE 'CEO';
ALTER TYPE job_grade_type ADD VALUE IF NOT EXISTS 'PRESIDENT' BEFORE 'CEO';
