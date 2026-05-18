-- 결재 서식-결재선 서식 관계 변경
-- 하나의 결재선 서식을 여러 결재 서식에서 재사용할 수 있도록 관계 방향을 변경
-- 결재 서식에 결재선 서식을 fk로 추가
ALTER TABLE app_form
    ADD COLUMN line_template_id INTEGER;

ALTER TABLE app_form
    ADD CONSTRAINT fk_app_form_line_template
        FOREIGN KEY (line_template_id)
        REFERENCES app_line_template(template_id);

-- 기존 APP_LINE_TEMPLATE.form_id에 데이터가 있다면, 컬럼 제거 전에 다음처럼 이관할 수 있습니다.
UPDATE app_form f
SET line_template_id = t.template_id
FROM app_line_template t
WHERE t.form_id = f.form_id;

-- 이관 확인 후 기존 FK와 컬럼을 제거합니다.
-- 실제 FK 이름은 DB에서 확인 후 변경하세요.
ALTER TABLE app_line_template
    DROP CONSTRAINT IF EXISTS fkk84oo4y8i0iw594uirf4mt5sh;

ALTER TABLE app_line_template
    DROP COLUMN IF EXISTS form_id;

-------------------------------------------------------------
-- app_form 기본 제공 서식 보호 플래그
ALTER TABLE app_form
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;

-- 시연 기본 서식으로 보호할 서식은 아래 예시처럼 필요 시 지정합니다.
-- UPDATE app_form
--    SET is_default = TRUE
--  WHERE form_name IN ('연차 신청서', '지출결의서');
