/*
 * 전자결재 기본 서식 template 보강 SQL
 *
 * 목적:
 * - 아직 template 값이 정해지지 않은 기본 전자결재 서식에 화면 렌더링용 JSON을 입력합니다.
 * - React 작성 화면은 app_form.template JSON의 fields 배열을 기준으로 입력 필드를 생성합니다.
 *
 * 주의:
 * - fileRequired=false는 첨부파일 입력 영역을 화면에 표시하지 않는다는 의미입니다.
 * - 현재 작성 화면에서 파일 첨부는 선택 첨부가 아니라 필수 첨부 방식이므로,
 *   아래 네 서식은 일반 입력 항목 위주로 구성하고 첨부파일은 사용하지 않도록 설정했습니다.
 */

-- 1. 교육 신청
UPDATE app_form
SET
    template = $${
      "title": "교육 신청",
      "fields": [
        {
          "id": "education_name",
          "type": "text",
          "label": "교육명",
          "placeholder": "",
          "description": "수강하려는 교육 또는 세미나명을 입력하세요.",
          "options": []
        },
        {
          "id": "education_institution",
          "type": "text",
          "label": "교육 기관",
          "placeholder": "",
          "description": "교육을 진행하는 기관명 또는 주관사를 입력하세요.",
          "options": []
        },
        {
          "id": "education_start_date",
          "type": "date",
          "label": "교육 시작일",
          "placeholder": "",
          "description": "교육이 시작되는 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "education_end_date",
          "type": "date",
          "label": "교육 종료일",
          "placeholder": "",
          "description": "교육이 종료되는 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "education_method",
          "type": "select",
          "label": "교육 방식",
          "placeholder": "",
          "description": "교육 참여 방식을 선택하세요.",
          "options": ["온라인", "오프라인", "혼합"]
        },
        {
          "id": "education_cost",
          "type": "amount",
          "label": "교육 비용",
          "placeholder": "0",
          "description": "회사 지원 또는 정산이 필요한 교육 비용을 입력하세요.",
          "options": []
        },
        {
          "id": "education_purpose",
          "type": "text",
          "label": "교육 목적",
          "placeholder": "",
          "description": "교육을 신청하는 업무상 목적을 입력하세요.",
          "options": []
        },
        {
          "id": "expected_effect",
          "type": "text",
          "label": "기대 효과",
          "placeholder": "",
          "description": "교육 수강 후 업무에 어떻게 활용할 수 있는지 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }$$,
    is_default = true
WHERE form_name = '교육 신청';

-- 2. 비용 계획 신청
UPDATE app_form
SET
    template = $${
      "title": "비용 계획 신청",
      "fields": [
        {
          "id": "expense_category",
          "type": "select",
          "label": "비용 구분",
          "placeholder": "",
          "description": "계획 중인 비용 유형을 선택하세요.",
          "options": ["식비", "교통비", "숙박비", "소모품", "교육비", "접대비", "기타"]
        },
        {
          "id": "expense_purpose",
          "type": "text",
          "label": "사용 목적",
          "placeholder": "",
          "description": "비용을 사용하려는 업무 목적을 입력하세요.",
          "options": []
        },
        {
          "id": "planned_start_date",
          "type": "date",
          "label": "사용 시작 예정일",
          "placeholder": "",
          "description": "비용 사용이 시작될 예정일을 선택하세요.",
          "options": []
        },
        {
          "id": "planned_end_date",
          "type": "date",
          "label": "사용 종료 예정일",
          "placeholder": "",
          "description": "비용 사용이 종료될 예정일을 선택하세요.",
          "options": []
        },
        {
          "id": "vendor_name",
          "type": "text",
          "label": "거래처/사용처",
          "placeholder": "",
          "description": "예상 거래처 또는 비용 사용처를 입력하세요.",
          "options": []
        },
        {
          "id": "planned_amount",
          "type": "amount",
          "label": "예상 금액",
          "placeholder": "0",
          "description": "사용 예정 금액을 입력하세요.",
          "options": []
        },
        {
          "id": "expense_plan_detail",
          "type": "text",
          "label": "비용 계획 내용",
          "placeholder": "",
          "description": "비용 사용 계획을 구체적으로 입력하세요.",
          "options": []
        },
        {
          "id": "expense_plan_reason",
          "type": "text",
          "label": "신청 사유",
          "placeholder": "",
          "description": "비용 계획 승인이 필요한 사유를 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }$$,
    is_default = true
WHERE form_name = '비용 계획 신청';

-- 3. 사내 활동 공유
UPDATE app_form
SET
    template = $${
      "title": "사내 활동 공유",
      "fields": [
        {
          "id": "activity_type",
          "type": "select",
          "label": "활동 구분",
          "placeholder": "",
          "description": "공유하려는 활동 유형을 선택하세요.",
          "options": ["회의", "워크숍", "교육", "프로젝트", "동호회", "행사", "기타"]
        },
        {
          "id": "activity_title",
          "type": "text",
          "label": "활동 제목",
          "placeholder": "",
          "description": "공유할 활동의 제목을 입력하세요.",
          "options": []
        },
        {
          "id": "activity_date",
          "type": "date",
          "label": "활동일",
          "placeholder": "",
          "description": "활동이 진행된 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "participants",
          "type": "text",
          "label": "참여자",
          "placeholder": "예: 개발1팀 전원, 홍길동 외 3명",
          "description": "활동에 참여한 대상자를 입력하세요.",
          "options": []
        },
        {
          "id": "activity_summary",
          "type": "text",
          "label": "활동 요약",
          "placeholder": "",
          "description": "활동의 주요 내용을 간략히 입력하세요.",
          "options": []
        },
        {
          "id": "share_content",
          "type": "text",
          "label": "공유 내용",
          "placeholder": "",
          "description": "구성원에게 공유하고 싶은 내용이나 결과를 입력하세요.",
          "options": []
        },
        {
          "id": "follow_up",
          "type": "text",
          "label": "후속 조치",
          "placeholder": "",
          "description": "추가 진행 사항이나 요청할 후속 조치가 있으면 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }$$,
    is_default = true
WHERE form_name = '사내 활동 공유';

-- 4. 증명서 발급 신청
UPDATE app_form
SET
    template = $${
      "title": "증명서 발급 신청",
      "fields": [
        {
          "id": "certificate_type",
          "type": "select",
          "label": "증명서 종류",
          "placeholder": "",
          "description": "발급받을 증명서 종류를 선택하세요.",
          "options": ["재직증명서", "경력증명서", "소득증명서", "원천징수영수증", "기타"]
        },
        {
          "id": "certificate_purpose",
          "type": "select",
          "label": "발급 목적",
          "placeholder": "",
          "description": "증명서 발급 목적을 선택하세요.",
          "options": ["금융기관 제출", "관공서 제출", "이직/경력 증빙", "비자/해외 제출", "개인 보관", "기타"]
        },
        {
          "id": "submit_to",
          "type": "text",
          "label": "제출처",
          "placeholder": "",
          "description": "증명서를 제출할 기관 또는 회사명을 입력하세요.",
          "options": []
        },
        {
          "id": "issue_count",
          "type": "number",
          "label": "발급 부수",
          "placeholder": "1",
          "description": "필요한 발급 부수를 입력하세요.",
          "options": []
        },
        {
          "id": "request_due_date",
          "type": "date",
          "label": "희망 수령일",
          "placeholder": "",
          "description": "증명서를 수령하고 싶은 날짜를 선택하세요.",
          "options": []
        },
        {
          "id": "language_type",
          "type": "select",
          "label": "발급 언어",
          "placeholder": "",
          "description": "증명서 발급 언어를 선택하세요.",
          "options": ["국문", "영문"]
        },
        {
          "id": "request_reason",
          "type": "text",
          "label": "신청 사유",
          "placeholder": "",
          "description": "증명서 발급이 필요한 구체적인 사유를 입력하세요.",
          "options": []
        },
        {
          "id": "request_note",
          "type": "text",
          "label": "비고",
          "placeholder": "",
          "description": "추가 요청 사항이 있으면 입력하세요.",
          "options": []
        }
      ],
      "fileRequired": false
    }$$,
    is_default = true
WHERE form_name = '증명서 발급 신청';
