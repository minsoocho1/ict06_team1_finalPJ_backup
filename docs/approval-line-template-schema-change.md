# 결재 서식-결재선 서식 관계 변경 메모

## 변경 목표

하나의 결재선 서식을 여러 결재 서식에서 재사용할 수 있도록 관계 방향을 변경합니다.

기존 구조:

```text
APP_LINE_TEMPLATE.form_id -> APP_FORM.form_id
```

변경 구조:

```text
APP_FORM.line_template_id -> APP_LINE_TEMPLATE.template_id
```

## 관계

```text
APP_LINE_TEMPLATE 1 ─── N APP_FORM
```

예시:

| 전자결재 서식 | 연결 결재선 |
| --- | --- |
| 부재 일정 | 근태 관리 결재선 |
| 근무 계획 신청 | 근태 관리 결재선 |
| 근무 결과 신청 | 근태 관리 결재선 |
| 비용 정산 신청 | 비용 집행 결재선 |
| 비용 계획 신청 | 비용 집행 결재선 |

## DDL 예시

운영 DB에 적용할 때는 실제 제약조건 이름을 확인한 뒤 실행해야 합니다.

```sql
ALTER TABLE app_form
    ADD COLUMN line_template_id INTEGER;

ALTER TABLE app_form
    ADD CONSTRAINT fk_app_form_line_template
        FOREIGN KEY (line_template_id)
        REFERENCES app_line_template(template_id);
```

기존 `APP_LINE_TEMPLATE.form_id`에 데이터가 있다면, 컬럼 제거 전에 다음처럼 이관할 수 있습니다.

```sql
UPDATE app_form f
SET line_template_id = t.template_id
FROM app_line_template t
WHERE t.form_id = f.form_id;
```

이관 확인 후 기존 FK와 컬럼을 제거합니다.

```sql
-- 실제 FK 이름은 DB에서 확인 후 변경하세요.
ALTER TABLE app_line_template
    DROP CONSTRAINT IF EXISTS fk_app_line_template_form;

ALTER TABLE app_line_template
    DROP COLUMN IF EXISTS form_id;
```

## 코드 반영 내용

- `AppFormEntity`에 `lineTemplate` 필드를 추가했습니다.
- `AppLineTemplateEntity`에서 `form` 필드를 제거했습니다.
- 결재 서식 목록은 `form.getLineTemplate()` 기준으로 연결 결재선을 표시합니다.
- 결재선 서식 목록은 해당 결재선 서식을 사용하는 결재 서식명을 쉼표로 묶어 표시합니다.
- 결재선 서식 적용 API는 이제 `APP_FORM.line_template_id`를 수정합니다.
- 사용 중인 결재선 서식은 삭제할 수 없도록 서비스에서 검증합니다.

