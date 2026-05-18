-- 전자결재 결재 서식-결재선 서식 관계 변경 롤백 SQL
-- 목적:
--   APP_FORM.line_template_id 구조를 적용하기 전 상태로 되돌립니다.
--   즉, APP_LINE_TEMPLATE.form_id 컬럼을 다시 만들고 APP_FORM.line_template_id를 제거합니다.
--
-- 주의:
--   새 구조에서는 하나의 결재선 서식을 여러 결재 서식이 공유할 수 있습니다.
--   하지만 기존 구조(APP_LINE_TEMPLATE.form_id)는 결재선 서식 1개당 결재 서식 1개만 저장할 수 있습니다.
--   따라서 같은 line_template_id를 여러 app_form이 사용 중이면 가장 작은 form_id 1건만 복구됩니다.
--   운영 DB에 적용하기 전 아래 중복 확인 쿼리 결과를 반드시 확인하세요.

-- 1. 하나의 결재선 서식을 여러 결재 서식이 공유 중인지 확인합니다.
SELECT
    line_template_id,
    COUNT(*) AS connected_form_count,
    STRING_AGG(form_id::TEXT, ', ' ORDER BY form_id) AS connected_form_ids
FROM app_form
WHERE line_template_id IS NOT NULL
GROUP BY line_template_id
HAVING COUNT(*) > 1;

-- 2. 기존 구조에서 사용하던 APP_LINE_TEMPLATE.form_id 컬럼을 다시 추가합니다.
ALTER TABLE app_line_template
    ADD COLUMN IF NOT EXISTS form_id INTEGER;

-- 3. APP_FORM.line_template_id 값을 APP_LINE_TEMPLATE.form_id로 이관합니다.
--    여러 결재 서식이 같은 결재선 서식을 사용하는 경우 MIN(form_id) 1건만 복구합니다.
UPDATE app_line_template t
SET form_id = migrated.form_id
FROM (
    SELECT
        line_template_id AS template_id,
        MIN(form_id) AS form_id
    FROM app_form
    WHERE line_template_id IS NOT NULL
    GROUP BY line_template_id
) migrated
WHERE t.template_id = migrated.template_id;

-- 4. 기존 구조의 FK를 복구합니다.
--    이미 같은 이름의 FK가 있다면 먼저 제거한 뒤 다시 생성합니다.
ALTER TABLE app_line_template
    DROP CONSTRAINT IF EXISTS fk_app_line_template_form;

ALTER TABLE app_line_template
    ADD CONSTRAINT fk_app_line_template_form
        FOREIGN KEY (form_id)
        REFERENCES app_form(form_id);

-- 5. 새 구조에서 추가한 FK와 컬럼을 제거합니다.
ALTER TABLE app_form
    DROP CONSTRAINT IF EXISTS fk_app_form_line_template;

ALTER TABLE app_form
    DROP COLUMN IF EXISTS line_template_id;

-- 6. app_form의 is_default 컬럼을 제겋바니다.
ALTER TABLE app_form
    DROP COLUMN IF EXISTS is_default;