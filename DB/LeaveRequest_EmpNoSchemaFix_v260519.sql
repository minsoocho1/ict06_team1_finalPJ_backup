/*
 * [결재-근태 연동용]: LEAVE_REQUEST 테이블의 사원 참조 컬럼을 현재 Entity 구조(emp_no)와 맞추기 위한 보정 SQL입니다.
 *
 * 오류 원인:
 * - LeaveRequestEntity는 employee를 emp_no 컬럼으로 저장합니다.
 * - 일부 개발 DB에는 예전 구조의 emp_id 컬럼이 NOT NULL로 남아 있어 Hibernate insert 시 emp_id가 NULL이 되어 실패합니다.
 *
 * 적용 시점:
 * - 부재 일정 결재 최종 승인 시 "leave_request.emp_id null violates not-null constraint" 오류가 발생하는 DB에 적용합니다.
 */

-- 1. 현재 Entity가 사용하는 emp_no 컬럼이 없는 오래된 DB를 대비해 컬럼을 보장합니다.
ALTER TABLE leave_request
    ADD COLUMN IF NOT EXISTS emp_no VARCHAR(20);

-- 2. 기존 데이터가 emp_id에만 남아 있다면 employee.emp_id를 통해 emp_no를 백필합니다.
--    emp_id 컬럼이 이미 제거된 DB에서도 스크립트가 실패하지 않도록 존재 여부를 확인합니다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'leave_request'
          AND column_name = 'emp_id'
    ) THEN
        UPDATE leave_request lr
        SET emp_no = e.emp_no
        FROM employee e
        WHERE lr.emp_no IS NULL
          AND lr.emp_id IS NOT NULL
          AND lr.emp_id = e.emp_id;
    END IF;
END $$;

-- 3. emp_no가 채워진 뒤 현재 Entity 구조에 맞게 필수 컬럼으로 지정합니다.
ALTER TABLE leave_request
    ALTER COLUMN emp_no SET NOT NULL;

-- 4. 예전 구조의 emp_id 컬럼이 남아 있으면 더 이상 사용하지 않으므로 제거합니다.
--    연결된 예전 FK/NOT NULL 제약도 함께 정리되도록 CASCADE를 사용합니다.
ALTER TABLE leave_request
    DROP COLUMN IF EXISTS emp_id CASCADE;

-- 5. emp_no FK가 없는 DB를 위해 FK를 다시 보장합니다.
--    이미 같은 이름의 제약이 있으면 먼저 제거한 뒤 재생성합니다.
ALTER TABLE leave_request
    DROP CONSTRAINT IF EXISTS fk_leave_request_employee;

ALTER TABLE leave_request
    ADD CONSTRAINT fk_leave_request_employee
        FOREIGN KEY (emp_no)
        REFERENCES employee(emp_no);
