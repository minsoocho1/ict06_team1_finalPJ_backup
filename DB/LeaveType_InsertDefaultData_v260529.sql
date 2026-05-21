INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '연차', 1.0, true, true, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '연차');

INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '오전반차', 0.5, true, true, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '오전반차');

INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '오후반차', 0.5, true, true, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '오후반차');

INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '조퇴', 0.1, true, true, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '조퇴');

INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '병가', 1.0, true, false, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '병가');

INSERT INTO leave_type (type_name, min_unit, is_paid, is_annual_deduct, is_active, created_at, updated_at)
SELECT '경조사', 1.0, true, false, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE type_name = '경조사');
