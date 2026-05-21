-- * @author : 송영은
-- * description : 관리자용 전자결재에 필요한 sql 
-- * ========================================
-- * DATE         AUTHOR      NOTE
-- * 2026-05-01   송영은       최초 생성
-- * 2026-05-14   송영은		 기본 결재 서식 넣기, approval, app_line 최신 엔티티 파일에 맞게 표 구조 변경 
-- * 2026-05-15   송영은		 테스트 결재 상신 데이터 삭제

----------------------------------------------------------------
-- 테스트 결재 상신 데이터 삭제
delete from app_line; 
delete from app_file;
delete from approval; 

----------------------------------------------------------------
-- app_form 데이터 삭제
delete from app_form 
 where form_id <= 14; 

-- app_line 최신 엔티티 파일에 맞게 표 구조 변경 -------------------------
BEGIN;

-- 1. 혹시 기존 데이터가 approver_no에만 들어간 경우를 대비해 값 이관
ALTER TABLE app_line
    ALTER COLUMN approver_id DROP NOT NULL;

UPDATE app_line
SET approver_id = approver_no
WHERE approver_id IS NULL
  AND approver_no IS NOT NULL;

-- 2. 이관 후에도 결재자 값이 없는 데이터가 있으면 중단
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM app_line
        WHERE approver_id IS NULL
    ) THEN
        RAISE EXCEPTION 'app_line.approver_id가 NULL인 데이터가 있어 마이그레이션을 중단합니다.';
    END IF;
END $$;

-- 3. approver_no 또는 approver_id에 걸린 기존 FK 제약조건 제거
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT DISTINCT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = t.oid
        WHERE t.relname = 'app_line'
          AND c.contype = 'f'
          AND a.attnum = ANY (c.conkey)
          AND a.attname IN ('approver_no', 'approver_id')
    LOOP
        EXECUTE format('ALTER TABLE app_line DROP CONSTRAINT IF EXISTS %I', constraint_name);
    END LOOP;
END $$;

-- 4. 엔티티에서 더 이상 사용하지 않는 중복 컬럼 제거
ALTER TABLE app_line
    DROP COLUMN IF EXISTS approver_no;

-- 5. approver_id를 엔티티와 맞는 결재자 FK 컬럼으로 확정
ALTER TABLE app_line
    ALTER COLUMN approver_id TYPE varchar(20),
    ALTER COLUMN approver_id SET NOT NULL;

ALTER TABLE app_line
    ADD CONSTRAINT fk_app_line_approver
    FOREIGN KEY (approver_id)
    REFERENCES employee(emp_no);

COMMIT;

-----------------------------------------------------------------------
-- approval 최신 entity에 맞게 현재 DB 수정
BEGIN;

-- 1. 기존 데이터가 *_id 컬럼에만 들어간 경우를 대비해 값 이관
UPDATE approval
SET writer_no = writer_id
WHERE writer_no IS NULL
  AND writer_id IS NOT NULL;

UPDATE approval
SET current_approver_no = current_approver_id
WHERE current_approver_no IS NULL
  AND current_approver_id IS NOT NULL;

-- 2. writer_no는 엔티티에서 nullable=false 이므로 NULL 데이터가 있으면 중단
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM approval
        WHERE writer_no IS NULL
    ) THEN
        RAISE EXCEPTION 'approval.writer_no가 NULL인 데이터가 있어 마이그레이션을 중단합니다.';
    END IF;
END $$;

-- 3. 중복 컬럼 또는 유지 컬럼에 걸린 기존 FK 제약조건 제거
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT DISTINCT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = t.oid
        WHERE t.relname = 'approval'
          AND c.contype = 'f'
          AND a.attnum = ANY (c.conkey)
          AND a.attname IN (
              'writer_id',
              'writer_no',
              'current_approver_id',
              'current_approver_no'
          )
    LOOP
        EXECUTE format('ALTER TABLE approval DROP CONSTRAINT IF EXISTS %I', constraint_name);
    END LOOP;
END $$;

-- 4. 최신 엔티티에서 사용하지 않는 중복 컬럼 제거
ALTER TABLE approval
    DROP COLUMN IF EXISTS writer_id,
    DROP COLUMN IF EXISTS current_approver_id;

-- 5. 최신 엔티티 기준 FK 컬럼 타입과 NULL 허용 여부 정리
ALTER TABLE approval
    ALTER COLUMN writer_no TYPE varchar(20),
    ALTER COLUMN writer_no SET NOT NULL,
    ALTER COLUMN current_approver_no TYPE varchar(20),
    ALTER COLUMN current_approver_no DROP NOT NULL;

-- 6. 최신 엔티티 기준 FK 제약조건 재생성
ALTER TABLE approval
    ADD CONSTRAINT fk_approval_writer
    FOREIGN KEY (writer_no)
    REFERENCES employee(emp_no);

ALTER TABLE approval
    ADD CONSTRAINT fk_approval_current_approver
    FOREIGN KEY (current_approver_no)
    REFERENCES employee(emp_no);

-- 7. ApprovalStatus enum에 추가된 CANCELED 상태를 DB 체크 제약조건에도 반영
--    이 쿼리가 없으면 상신 취소 시 status='CANCELED' 업데이트가 approval_status_check에 막힙니다.
ALTER TABLE approval
    DROP CONSTRAINT IF EXISTS approval_status_check;

ALTER TABLE approval
    ADD CONSTRAINT approval_status_check
    CHECK (status IN ('DRAFT', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELED'));

COMMIT;




------------------------------------------------------------------------
-- ⭐⭐ 근태 관련 기본 결재 서식 넣기 : 아래 서식은 근태와 연동되어야 하므로 별도로 데이터 생성 필요.
---- 1. 근무 계획 신청 서식
UPDATE app_form
SET
    template = '{
      "title": "근무 계획 신청",
      "fields": [
        {
          "id": "work_plan_date",
          "type": "date",
          "label": "근무 예정일",
          "placeholder": "",
          "description": "근무 계획을 신청할 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "planned_start_time",
          "type": "time",
          "label": "근무 시작 예정 시간",
          "placeholder": "",
          "description": "예정된 근무 시작 시간을 입력하세요.",
          "options": []
        },
        {
          "id": "planned_end_time",
          "type": "time",
          "label": "근무 종료 예정 시간",
          "placeholder": "",
          "description": "예정된 근무 종료 시간을 입력하세요.",
          "options": []
        },
        {
          "id": "work_plan_type",
          "type": "select",
          "label": "근무 형태",
          "placeholder": "",
          "description": "신청하려는 근무 형태를 선택하세요.",
          "options": ["재택근무", "외근", "연장근무"]
        },
        {
          "id": "work_plan_reason",
          "type": "text",
          "label": "신청 사유",
          "placeholder": "",
          "description": "근무 계획 신청 사유를 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }',
    is_default = true
WHERE form_name = '근무 계획 신청';

---- 2. 부재 일정
UPDATE app_form
SET
    template = '{
      "title": "부재 일정",
      "fields": [
        {
          "id": "absence_type",
          "type": "select",
          "label": "부재 유형",
          "placeholder": "",
          "description": "신청하려는 부재 유형을 선택하세요.",
          "options": ["연차", "오전반차", "오후반차", "조퇴", "외출", "병가", "경조사"]
        },
        {
          "id": "absence_start_date",
          "type": "date",
          "label": "부재 시작일",
          "placeholder": "",
          "description": "부재가 시작되는 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "absence_end_date",
          "type": "date",
          "label": "부재 종료일",
          "placeholder": "",
          "description": "부재가 종료되는 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "absence_start_time",
          "type": "time",
          "label": "부재 시작 시간",
          "placeholder": "",
          "description": "반차, 조퇴, 외출처럼 시간이 필요한 경우 입력하세요.",
          "options": []
        },
        {
          "id": "absence_end_time",
          "type": "time",
          "label": "부재 종료 시간",
          "placeholder": "",
          "description": "복귀 예정 시간 또는 부재 종료 시간을 입력하세요.",
          "options": []
        },
        {
          "id": "absence_reason",
          "type": "text",
          "label": "부재 사유",
          "placeholder": "",
          "description": "부재 사유를 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }',
    is_default = true
WHERE form_name = '부재 일정';

-- 3. 근무 결과 신청
UPDATE app_form
SET
    template = '{
      "title": "근무 결과 신청",
      "fields": [
        {
          "id": "work_result_date",
          "type": "date",
          "label": "근무일",
          "placeholder": "",
          "description": "근무 결과를 등록할 날짜를 선택하세요."
        },
        {
          "id": "actual_start_time",
          "type": "time",
          "label": "실제 근무 시작 시간",
          "placeholder": "",
          "description": "실제 근무를 시작한 시간을 입력하세요."
        },
        {
          "id": "actual_end_time",
          "type": "time",
          "label": "실제 근무 종료 시간",
          "placeholder": "",
          "description": "실제 근무를 종료한 시간을 입력하세요."
        },
        {
          "id": "break_minutes",
          "type": "number",
          "label": "휴게 시간(분)",
          "placeholder": "0",
          "description": "휴게 시간을 분 단위로 입력하세요."
        },
        {
          "id": "work_result_content",
          "type": "text",
          "label": "근무 결과 내용",
          "placeholder": "",
          "description": "수행한 업무 내용을 입력하세요."
        },
		{
          "id": "work_plan_type",
          "type": "select",
          "label": "근무 형태",
          "placeholder": "",
          "description": "신청하려는 근무 형태를 선택하세요.",
          "options": ["재택근무", "외근", "연장근무"]
        },
        {
          "id": "work_result_reason",
          "type": "text",
          "label": "신청 사유",
          "placeholder": "",
          "description": "근무 결과 신청 또는 정정 사유를 입력하세요."
        }
      ],
      "fileRequired": false
    }',
    is_default = true
WHERE form_name = '근무 결과 신청';

-- 반영 확인용
SELECT form_id, form_name, is_default, template
FROM app_form
WHERE form_name IN ('근무 계획 신청', '부재 일정', '근무 결과 신청')
ORDER BY form_id;

--- vOld: 셀렉트 박스 적용 전---------
---- 1. 근무 계획 신청 서식
UPDATE app_form
SET
    template = '{
      "title": "근무계획 신청",
      "fields": [
        {
          "id": "work_plan_date",
          "type": "date",
          "label": "근무 예정일",
          "placeholder": "",
          "description": "근무 계획을 신청할 날짜를 선택하세요."
        },
        {
          "id": "planned_start_time",
          "type": "time",
          "label": "근무 시작 예정 시간",
          "placeholder": "",
          "description": "예정된 근무 시작 시간을 입력하세요."
        },
        {
          "id": "planned_end_time",
          "type": "time",
          "label": "근무 종료 예정 시간",
          "placeholder": "",
          "description": "예정된 근무 종료 시간을 입력하세요."
        },
        {
          "id": "work_plan_type",
          "type": "text",
          "label": "근무 형태",
          "placeholder": "예: 정상근무, 재택근무, 외근, 연장근무",
          "description": "신청하려는 근무 형태를 입력하세요."
        },
        {
          "id": "work_plan_reason",
          "type": "text",
          "label": "신청 사유",
          "placeholder": "",
          "description": "근무 계획 신청 사유를 입력하세요."
        }
      ],
      "fileRequired": false
    }',
    is_default = true
WHERE form_name = '근무 계획 신청';

---- 2. 부재 일정
UPDATE app_form
SET
    template = '{
      "title": "부재 일정",
      "fields": [
        {
          "id": "absence_type",
          "type": "text",
          "label": "부재 유형",
          "placeholder": "예: 연차, 오전반차, 오후반차, 조퇴, 외출, 병가",
          "description": "신청하려는 부재 유형을 입력하세요."
        },
        {
          "id": "absence_start_date",
          "type": "date",
          "label": "부재 시작일",
          "placeholder": "",
          "description": "부재가 시작되는 날짜를 선택하세요."
        },
        {
          "id": "absence_end_date",
          "type": "date",
          "label": "부재 종료일",
          "placeholder": "",
          "description": "부재가 종료되는 날짜를 선택하세요."
        },
        {
          "id": "absence_start_time",
          "type": "time",
          "label": "부재 시작 시간",
          "placeholder": "",
          "description": "반차, 조퇴, 외출처럼 시간이 필요한 경우 입력하세요."
        },
        {
          "id": "absence_end_time",
          "type": "time",
          "label": "부재 종료 시간",
          "placeholder": "",
          "description": "복귀 예정 시간 또는 부재 종료 시간을 입력하세요."
        },
        {
          "id": "absence_reason",
          "type": "text",
          "label": "부재 사유",
          "placeholder": "",
          "description": "부재 사유를 입력하세요."
        }
      ],
      "fileRequired": false
    }',
    is_default = true
WHERE form_name = '부재 일정';



-- (주의) 아래에 작성된 쿼리들은 실행 전에 충분히 고민할 것. 연관 테이블 삭제 위험-----------
-- 비상 시에 테이블 아예 삭제
drop table app_form cascade;  
drop table attendance; 

-- 테스트 용으로 만든 근로자 데이터 삭제 
delete from employee 
 where emp_no = '20260001';

select * from pg_settings
where name = 'max_connections';

SELECT * FROM pg_stat_activity;

-- 현재 connection 상태 확인
SELECT count(*) FROM pg_stat_activity;

SELECT state, count(*)
FROM pg_stat_activity
GROUP BY state;

-- 오래된 세션 종료 
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
AND now() - state_change > interval '5 minutes';
