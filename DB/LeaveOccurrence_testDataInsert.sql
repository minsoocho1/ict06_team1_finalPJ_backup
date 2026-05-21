-- 최신 엔티티와 안 맞는 DB 구조 변경
-- 1. 현재 Entity가 사용하는 emp_no 컬럼 보장
ALTER TABLE leave_occurrence
    ADD COLUMN IF NOT EXISTS emp_no VARCHAR(20);

-- 2. 기존 데이터가 emp_id에만 있다면 employee.emp_id 기준으로 emp_no 백필
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'leave_occurrence'
          AND column_name = 'emp_id'
    ) THEN
        UPDATE leave_occurrence lo
        SET emp_no = e.emp_no
        FROM employee e
        WHERE lo.emp_no IS NULL
          AND lo.emp_id IS NOT NULL
          AND lo.emp_id = e.emp_id;
    END IF;
END $$;

-- 3. emp_no를 최신 Entity 기준 필수 컬럼으로 설정
ALTER TABLE leave_occurrence
    ALTER COLUMN emp_no SET NOT NULL;

-- 4. 예전 구조의 emp_id 컬럼 제거
ALTER TABLE leave_occurrence
    DROP COLUMN IF EXISTS emp_id CASCADE;

-- 5. emp_no -> employee(emp_no) FK 재생성
ALTER TABLE leave_occurrence
    DROP CONSTRAINT IF EXISTS fk_leave_occurrence_employee;

ALTER TABLE leave_occurrence
    ADD CONSTRAINT fk_leave_occurrence_employee
        FOREIGN KEY (emp_no)
        REFERENCES employee(emp_no);

-- 테스트용 연차 부여 쿼리
INSERT INTO leave_occurrence (
    emp_no,
    type_id,
    target_year,
    occur_date,
    occur_days,
    used_days,
    remain_days,
    expiry_date,
    reason,
    created_at,
    updated_at
)
SELECT
    '20269997',
    lt.type_id,
    2026,
    CURRENT_DATE,
    50.0,
    0.0,
    50.0,
    DATE '2026-12-31',
    '전자결재 근태 연동 테스트용 임시 연차 부여',
    NOW(),
    NOW()
FROM leave_type lt
WHERE lt.type_name = '연차';
