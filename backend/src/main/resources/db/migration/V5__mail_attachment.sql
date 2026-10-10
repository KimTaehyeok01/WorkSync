-- 메일 첨부파일: 파일 첨부 위치 유형(file_attachment.ref_type)에 MAIL 추가
-- 주의: 이 프로젝트는 Flyway가 비활성화(spring.flyway.enabled: false) 상태라 이 파일은 자동 적용되지 않는다.
-- 사용자가 Supabase SQL Editor 등으로 아래 SQL을 DB에 직접 실행해야 한다.
-- 여러 번 실행해도 안전하다(이미 MAIL 이 있으면 아무 작업도 하지 않는다). 데이터는 지우지 않는다.
-- 서버를 배포(재시작)하기 전에 먼저 실행할 것.
--
-- 실제 DB 확인 결과: ref_type 은 문자열 컬럼이고 CHECK 제약(file_attachment_ref_type_check)이 허용 값을 제한한다.
-- 그래서 이 제약에 MAIL 이 없으면 메일 첨부 저장(refType MAIL)이 거부된다. (메일 목록 조회는 영향 없음)
--
-- 1) 컬럼이 Postgres enum 타입인 환경이면 그 타입에 MAIL 값을 추가한다(문자열 컬럼이면 아무것도 하지 않는다).
DO $$
DECLARE
    col_schema text;
    col_type   text;
BEGIN
    SELECT c.udt_schema, c.udt_name
      INTO col_schema, col_type
      FROM information_schema.columns c
     WHERE c.table_schema = 'public'
       AND c.table_name = 'file_attachment'
       AND c.column_name = 'ref_type';

    IF col_type IS NOT NULL AND EXISTS (
        SELECT 1
          FROM pg_type t
          JOIN pg_namespace n ON n.oid = t.typnamespace
         WHERE t.typname = col_type
           AND n.nspname = col_schema
           AND t.typtype = 'e'
    ) THEN
        EXECUTE format('ALTER TYPE %I.%I ADD VALUE IF NOT EXISTS %L', col_schema, col_type, 'MAIL');
    END IF;
END $$;

-- 2) CHECK 제약이 있으면 "기존에 허용하던 값 전부 + MAIL" 로 다시 만든다.
--    제약 정의에서 기존 허용 값을 그대로 읽어 오므로 기존 데이터는 모두 계속 통과한다.
--    제약 삭제와 재생성이 한 트랜잭션(이 DO 블록) 안에서 일어나므로 도중에 오류가 나면 원래대로 되돌아간다.
--    제약이 없거나 이미 MAIL 이 들어 있으면 아무 작업도 하지 않는다.
DO $$
DECLARE
    con_def text;
    vals    text[];
BEGIN
    SELECT pg_get_constraintdef(oid)
      INTO con_def
      FROM pg_constraint
     WHERE conrelid = 'public.file_attachment'::regclass
       AND conname = 'file_attachment_ref_type_check';

    IF con_def IS NULL OR con_def LIKE '%''MAIL''%' THEN
        RETURN;
    END IF;

    SELECT array_agg(DISTINCT x)
      INTO vals
      FROM (SELECT (regexp_matches(con_def, '''([^'']+)''', 'g'))[1] AS x) s;

    IF vals IS NULL THEN
        RAISE EXCEPTION 'file_attachment_ref_type_check 에서 기존 허용 값을 읽지 못했습니다: %', con_def;
    END IF;

    vals := vals || ARRAY['MAIL'];

    ALTER TABLE public.file_attachment DROP CONSTRAINT file_attachment_ref_type_check;
    EXECUTE format(
        'ALTER TABLE public.file_attachment ADD CONSTRAINT file_attachment_ref_type_check CHECK ((ref_type)::text = ANY (%L::text[]))',
        vals
    );
END $$;

-- 적용 확인용 쿼리 (별도로 실행 — 허용 값 목록에 MAIL 이 보여야 한다):
--   SELECT pg_get_constraintdef(oid)
--   FROM pg_constraint
--   WHERE conrelid = 'public.file_attachment'::regclass AND conname = 'file_attachment_ref_type_check';
