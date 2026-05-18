-- 온보딩 콘텐츠/문서 시드 데이터
-- 작성자 : 김다솜
-- 작성일 : 2026-05-13
-- 설명 : VIDEO 유지 / PDF-RAG 연동 기준 ON_CONTENT, DOCUMENT, 부서 매핑 정리

BEGIN;

------------------------------------------------------------
-- 1. ON_CONTENT
------------------------------------------------------------
INSERT INTO ON_CONTENT
(title, type, category, sub_category, target_position, difficulty, estimated_time, tags, is_mandatory, path)
VALUES
-- 전사 공통
('회사 비전 및 핵심가치 소개', 'VIDEO', '전사', '오리엔테이션', '공통', 'EASY', 12,
 '["vision","culture","orientation"]'::jsonb, true, 'https://youtu.be/80kxM2YeRC4?si=CO0AhKkCqgeoF9aJ'),

('보안 사고 대응 절차 안내', 'PDF', '전사', '보안정책', '공통', 'MEDIUM', 20,
 '["security","incident","policy"]'::jsonb, true, 'https://drive.google.com/file/d/1G-xEQiEe7mhWxiONZPabP1pwbA-6yFFT/view?usp=drive_link'),

('업무 보고 커뮤니케이션 가이드', 'PDF', '전사', '업무기본', '공통', 'EASY', 15,
 '["report","communication","common"]'::jsonb, false, 'https://drive.google.com/file/d/1eXnj7frL_jY_8fyOez405D9-7bOpDqxy/view?usp=drive_link'),

('취업규칙', 'PDF', '전사', '업무기본', '공통', 'EASY', 15,
 '["attendance","hr","common"]'::jsonb, false, 'https://drive.google.com/file/d/1qW5Lebo-zJ5fiTVDOH6tEbK9dTcm46wr/view?usp=drive_link'),

-- 인사팀
('근로제도의 이해', 'PDF', '인사', '인사운영', '인사팀', 'MEDIUM', 25,
 '["hr","document","ops"]'::jsonb, true, 'https://drive.google.com/file/d/1gYjTh5KTmEipeZlR3pgxj1I3aVWbRzK6/view?usp=drive_link'),

('직무 중심 인사관리', 'PDF', '인사', '근태/휴가', '인사팀', 'MEDIUM', 20,
 '["attendance","vacation","hr"]'::jsonb, false, 'https://drive.google.com/file/d/1Ud_KX7DwhMVy-XeZLXQ2jT_pgPTmXS_s/view?usp=drive_link'),

('채용 인터뷰 운영 체크포인트', 'PDF', '인사', '채용', '인사팀', 'EASY', 18,
 '["recruiting","interview","hr"]'::jsonb, false, 'https://drive.google.com/file/d/1Cf7Gdflhhj6VDiJNcFVSTE4UX6H56dxG/view?usp=drive_link'),

-- 개발1팀(백엔드)
('Spring Boot 예외 처리 실무', 'PDF', '개발', '백엔드', '개발1팀', 'MEDIUM', 22,
 '["spring-boot","backend","exception"]'::jsonb, true, 'https://drive.google.com/file/d/12z3sfBH0b6_yrG-ZYN7TDWm6i0VJO1Cz/view?usp=drive_link'),

('표준프레임워크 적용 가이드', 'PDF', '개발', '백엔드', '개발1팀', 'MEDIUM', 25,
 '["framework","backend","standard"]'::jsonb, true, 'https://drive.google.com/file/d/13QupZReo0W-ssQ-NvOuwWzMOYYhOyG-v/view?usp=drive_link'),

('AWS 기술 개요', 'PDF', '개발', 'Redis', '개발1팀', 'MEDIUM', 20,
 '["AWS","server","backend"]'::jsonb, false, 'https://drive.google.com/file/d/16K7EeSqYRegE_CPukMtwMZEuG10dgajX/view?usp=drive_link'),

-- 개발2팀(프론트엔드)
('React 상태 관리 패턴 실습', 'VIDEO', '개발', '프론트엔드', '개발2팀', 'MEDIUM', 20,
 '["react","state","frontend"]'::jsonb, true, 'https://youtu.be/nkXIpGjVxWU?si=JB1EBRxoA32en-OK'),

('컴포넌트 설계와 재사용 가이드', 'PDF', '개발', '프론트엔드', '개발2팀', 'MEDIUM', 18,
 '["component","frontend","guide"]'::jsonb, true, 'https://drive.google.com/file/d/1r0vvT0Z2AKKz4VmPbBCfynFWwhTNNn2D/view?usp=drive_link'),

('웹 접근성 체크포인트', 'PDF', '개발', '프론트엔드', '개발2팀', 'EASY', 15,
 '["a11y","frontend","ui"]'::jsonb, false, 'https://drive.google.com/file/d/1N0XXrKcjYgLZXTdhsqD5VYHUSI4rduqT/view?usp=drive_link'),

-- 디자인팀
('디자인 QA 리뷰 기준', 'PDF', '디자인', 'QA', '디자인팀', 'EASY', 15,
 '["design","qa","review"]'::jsonb, true, 'https://drive.google.com/file/d/1F76q2n4TZl4-dWosDSXludHLfGiZOsLJ/view?usp=drive_link'),

('Figma 디자인시스템 구축', 'PDF', '디자인', '디자인시스템', '디자인팀', 'MEDIUM', 20,
 '["figma","design-system","design"]'::jsonb, true, 'https://drive.google.com/file/d/12zs2rKTFE0aCSeSXPPvigiyYxl6vmHo0/view?usp=drive_link'),

('사용자 흐름 설계', 'PDF', '디자인', 'UX리서치', '디자인팀', 'EASY', 18,
 '["ux","flow","design"]'::jsonb, false, 'https://drive.google.com/file/d/1F76q2n4TZl4-dWosDSXludHLfGiZOsLJ/view?usp=drive_link');

------------------------------------------------------------
-- 2. DOCUMENT
-- PDF 콘텐츠만 RAG 대상 문서로 등록
-- ON_CONTENT.title / ON_CONTENT.path 와 동일하게 유지
------------------------------------------------------------
INSERT INTO DOCUMENT
(title, file_path, summary_preview, dept_id, access_level, current_stage, created_by)
SELECT
    v.title,
    v.file_path,
    NULL,
    d.dept_id,
    v.access_level,
    'UPLOADED',
    NULL
FROM (
    VALUES
    ('보안 사고 대응 절차 안내', 'https://drive.google.com/file/d/1G-xEQiEe7mhWxiONZPabP1pwbA-6yFFT/view?usp=drive_link', NULL, 'PUBLIC'),
    ('업무 보고 커뮤니케이션 가이드', 'https://drive.google.com/file/d/1eXnj7frL_jY_8fyOez405D9-7bOpDqxy/view?usp=drive_link', NULL, 'PUBLIC'),
    ('취업규칙', 'https://drive.google.com/file/d/1qW5Lebo-zJ5fiTVDOH6tEbK9dTcm46wr/view?usp=drive_link', NULL, 'PUBLIC'),

    ('근로제도의 이해', 'https://drive.google.com/file/d/1gYjTh5KTmEipeZlR3pgxj1I3aVWbRzK6/view?usp=drive_link', '인사팀', 'DEPT'),
    ('직무 중심 인사관리', 'https://drive.google.com/file/d/1Ud_KX7DwhMVy-XeZLXQ2jT_pgPTmXS_s/view?usp=drive_link', '인사팀', 'DEPT'),
    ('채용 인터뷰 운영 체크포인트', 'https://drive.google.com/file/d/1Cf7Gdflhhj6VDiJNcFVSTE4UX6H56dxG/view?usp=drive_link', '인사팀', 'DEPT'),

    ('Spring Boot 예외 처리 실무', 'https://drive.google.com/file/d/12z3sfBH0b6_yrG-ZYN7TDWm6i0VJO1Cz/view?usp=drive_link', '개발1팀', 'DEPT'),
    ('표준프레임워크 적용 가이드', 'https://drive.google.com/file/d/13QupZReo0W-ssQ-NvOuwWzMOYYhOyG-v/view?usp=drive_link', '개발1팀', 'DEPT'),
    ('AWS 기술 개요', 'https://drive.google.com/file/d/16K7EeSqYRegE_CPukMtwMZEuG10dgajX/view?usp=drive_link', '개발1팀', 'DEPT'),

    ('컴포넌트 설계와 재사용 가이드', 'https://drive.google.com/file/d/1r0vvT0Z2AKKz4VmPbBCfynFWwhTNNn2D/view?usp=drive_link', '개발2팀', 'DEPT'),
    ('웹 접근성 체크포인트', 'https://drive.google.com/file/d/1N0XXrKcjYgLZXTdhsqD5VYHUSI4rduqT/view?usp=drive_link', '개발2팀', 'DEPT'),

    ('디자인 QA 리뷰 기준', 'https://drive.google.com/file/d/1F76q2n4TZl4-dWosDSXludHLfGiZOsLJ/view?usp=drive_link', '디자인팀', 'DEPT'),
    ('Figma 디자인시스템 구축', 'https://drive.google.com/file/d/12zs2rKTFE0aCSeSXPPvigiyYxl6vmHo0/view?usp=drive_link', '디자인팀', 'DEPT'),
    ('사용자 흐름 설계', 'https://drive.google.com/file/d/1F76q2n4TZl4-dWosDSXludHLfGiZOsLJ/view?usp=drive_link', '디자인팀', 'DEPT')
) AS v(title, file_path, dept_name, access_level)
LEFT JOIN department d ON d.dept_name = v.dept_name;

------------------------------------------------------------
-- 3. ON_CONTENT_TARGET_DEPARTMENT
------------------------------------------------------------
INSERT INTO ON_CONTENT_TARGET_DEPARTMENT (content_id, dept_id)
SELECT c.content_id, d.dept_id
FROM (
    VALUES
    ('근로제도의 이해', '인사팀'),
    ('직무 중심 인사관리', '인사팀'),
    ('채용 인터뷰 운영 체크포인트', '인사팀'),

    ('Spring Boot 예외 처리 실무', '개발1팀'),
    ('표준프레임워크 적용 가이드', '개발1팀'),
    ('AWS 기술 개요', '개발1팀'),

    ('React 상태 관리 패턴 실습', '개발2팀'),
    ('컴포넌트 설계와 재사용 가이드', '개발2팀'),
    ('웹 접근성 체크포인트', '개발2팀'),

    ('디자인 QA 리뷰 기준', '디자인팀'),
    ('Figma 디자인시스템 구축', '디자인팀'),
    ('사용자 흐름 설계', '디자인팀')
) AS m(content_title, dept_name)
JOIN ON_CONTENT c ON c.title = m.content_title
JOIN department d ON d.dept_name = m.dept_name;

COMMIT;
------------------------------------------------------------
-- 4. CHECKLIST
------------------------------------------------------------
INSERT INTO CHECKLIST
(title, category, description, is_mandatory, related_content_id, checklist_type, order_no)
VALUES
-- 직접 체크형
('사내 메신저 프로필 설정 완료', '전사', '메신저 프로필과 기본 정보를 등록합니다.', true, CAST(NULL AS INTEGER), 'USER', 1),
('첫 주 온보딩 일정 확인', '전사', '첫 주 학습 및 업무 일정을 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 2),
('비상 연락망 정보 확인', '전사', '비상 연락망과 개인 연락처를 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 3),
('팀 협업 채널 초대 여부 확인', '전사', '메일, 메신저, 협업툴 초대 여부를 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 4),
('업무용 계정 로그인 점검', '전사', '필수 업무 계정 로그인 가능 여부를 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 5),

-- 전사 공통
('회사 비전 및 핵심가치 소개 시청', '전사', '회사 비전과 핵심가치를 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '회사 비전 및 핵심가치 소개'), 'SYSTEM', 6),

('보안 사고 대응 절차 안내 확인', '전사', '보안 사고 대응 절차를 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '보안 사고 대응 절차 안내'), 'SYSTEM', 7),

('업무 보고 커뮤니케이션 가이드 확인', '전사', '업무 보고 기본 원칙을 이해합니다.', false,
 (SELECT content_id FROM ON_CONTENT WHERE title = '업무 보고 커뮤니케이션 가이드'), 'SYSTEM', 8),

('취업규칙 확인', '전사', '취업규칙의 주요 내용을 확인합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '취업규칙'), 'SYSTEM', 9),

-- 인사
('근로제도의 이해 학습', '인사', '근로제도 기본 구조를 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '근로제도의 이해'), 'SYSTEM', 10),

('직무 중심 인사관리 학습', '인사', '직무 기반 인사관리 개념을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '직무 중심 인사관리'), 'SYSTEM', 11),

('채용 인터뷰 운영 체크포인트 확인', '인사', '채용 인터뷰 운영 포인트를 확인합니다.', false,
 (SELECT content_id FROM ON_CONTENT WHERE title = '채용 인터뷰 운영 체크포인트'), 'SYSTEM', 12),

('인사 서류 제출 상태 점검', '인사', '인사 관련 제출 서류와 보완 여부를 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 13),

-- 개발
('Spring Boot 예외 처리 실무 학습', '개발', '예외 처리 구조와 패턴을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = 'Spring Boot 예외 처리 실무'), 'SYSTEM', 14),

('표준프레임워크 적용 가이드 확인', '개발', '표준프레임워크 적용 방식을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '표준프레임워크 적용 가이드'), 'SYSTEM', 15),

('AWS 기술 개요 학습', '개발', 'AWS 기초 개념과 서비스 구성을 이해합니다.', false,
 (SELECT content_id FROM ON_CONTENT WHERE title = 'AWS 기술 개요'), 'SYSTEM', 16),

('React 상태 관리 패턴 실습 시청', '개발', 'React 상태 관리 패턴을 학습합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = 'React 상태 관리 패턴 실습'), 'SYSTEM', 17),

('컴포넌트 설계와 재사용 가이드 확인', '개발', '컴포넌트 재사용 원칙을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '컴포넌트 설계와 재사용 가이드'), 'SYSTEM', 18),

('웹 접근성 체크포인트 확인', '개발', '접근성 기본 항목을 점검합니다.', false,
 (SELECT content_id FROM ON_CONTENT WHERE title = '웹 접근성 체크포인트'), 'SYSTEM', 19),

('개발 로컬 환경 실행 점검', '개발', '프로젝트가 로컬에서 정상 실행되는지 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 20),

-- 디자인
('디자인 QA 리뷰 기준 확인', '디자인', '디자인 QA 리뷰 기준을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = '디자인 QA 리뷰 기준'), 'SYSTEM', 21),

('Figma 디자인시스템 구축 학습', '디자인', '디자인시스템 구축 방식을 이해합니다.', true,
 (SELECT content_id FROM ON_CONTENT WHERE title = 'Figma 디자인시스템 구축'), 'SYSTEM', 22),

('사용자 흐름 설계 학습', '디자인', '사용자 흐름 설계 사례를 이해합니다.', false,
 (SELECT content_id FROM ON_CONTENT WHERE title = '사용자 흐름 설계'), 'SYSTEM', 23),

('디자인 파일 접근 권한 확인', '디자인', 'Figma 및 디자인 자산 접근 권한을 확인합니다.', true, CAST(NULL AS INTEGER), 'USER', 24);

------------------------------------------------------------
-- 5. QUIZ_GENERATION_RULE
------------------------------------------------------------
INSERT INTO QUIZ_GENERATION_RULE
(category_name, question_count, pass_score, difficulty, question_type, is_active)
VALUES
('전사', 4, 80, 'EASY', 'MULTIPLE_CHOICE', true),
('인사', 4, 80, 'MEDIUM', 'MULTIPLE_CHOICE', true),
('개발', 6, 80, 'MEDIUM', 'MULTIPLE_CHOICE', true),
('디자인', 4, 80, 'EASY', 'MULTIPLE_CHOICE', true);

------------------------------------------------------------
-- 6. QUIZ_QUESTION
------------------------------------------------------------

-- 전사
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
VALUES
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '회사 비전 및 핵심가치 소개'),
 '전사', 'MULTIPLE_CHOICE',
 '회사 비전 및 핵심가치 소개 콘텐츠의 주요 목적은 무엇인가요?',
 '행사 일정만 공유하기 위해',
 '조직의 방향성과 일하는 기준을 이해시키기 위해',
 '평가 점수만 높이기 위해',
 '출근 시간을 기록하기 위해',
 2, null, null, null, 10,
 '비전과 핵심가치는 조직 이해와 업무 기준 정렬을 돕습니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '보안 사고 대응 절차 안내'),
 '전사', 'MULTIPLE_CHOICE',
 '보안 사고 대응에서 가장 중요한 행동은 무엇인가요?',
 '문제를 혼자 숨기고 해결한다',
 '이상 징후를 즉시 보고하고 절차에 따라 대응한다',
 '증거를 먼저 삭제한다',
 '관련 없는 팀에만 먼저 알린다',
 2, null, null, null, 10,
 '보안 사고는 즉시 보고와 절차 기반 대응이 핵심입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '업무 보고 커뮤니케이션 가이드'),
 '전사', 'SHORT_ANSWER',
 '업무 보고 커뮤니케이션 가이드를 먼저 학습해야 하는 이유를 설명하세요.',
 null, null, null, null, null,
 '업무 보고 가이드는 보고 형식과 전달 기준을 맞춰 협업 효율과 의사소통 정확도를 높이는 데 도움이 된다.',
 '["업무 보고","커뮤니케이션","협업","전달","기준"]'::jsonb,
 '보고 기준과 협업 효율 관점을 설명하면 좋음.',
 10,
 '업무 보고 기준을 맞추는 것은 협업 효율에 중요합니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '취업규칙'),
 '전사', 'MULTIPLE_CHOICE',
 '취업규칙 확인의 가장 큰 목적은 무엇인가요?',
 '랜덤 규정을 암기하기 위해',
 '근무 관련 기본 규정과 원칙을 이해하기 위해',
 '동영상 시청 시간을 늘리기 위해',
 '평가 문제 수를 늘리기 위해',
 2, null, null, null, 10,
 '취업규칙은 근무 관련 기본 규정과 원칙 이해를 돕습니다.'
);

-- 인사
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
VALUES
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '근로제도의 이해'),
 '인사', 'MULTIPLE_CHOICE',
 '근로제도를 이해해야 하는 이유로 가장 적절한 것은 무엇인가요?',
 '근무 기준과 제도를 정확히 운영하기 위해',
 '디자인 리뷰를 하기 위해',
 '서버 배포를 하기 위해',
 'UI만 점검하기 위해',
 1, null, null, null, 10,
 '근로제도 이해는 인사 운영의 기본입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '직무 중심 인사관리'),
 '인사', 'MULTIPLE_CHOICE',
 '직무 중심 인사관리의 핵심은 무엇인가요?',
 '개인 취향대로 업무를 나누는 것',
 '직무 기준으로 역할과 평가를 정렬하는 것',
 '휴가만 관리하는 것',
 '랜덤하게 조직을 재배치하는 것',
 2, null, null, null, 10,
 '직무 중심 인사관리는 역할과 평가 기준 정렬이 핵심입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '채용 인터뷰 운영 체크포인트'),
 '인사', 'SHORT_ANSWER',
 '채용 인터뷰 운영 체크포인트를 확인해야 하는 이유를 설명하세요.',
 null, null, null, null, null,
 '채용 인터뷰 운영 체크포인트를 확인하면 면접 진행 기준을 통일하고 지원자 경험과 평가 품질을 높일 수 있다.',
 '["채용","인터뷰","기준","지원자 경험","평가 품질"]'::jsonb,
 '인터뷰 기준 통일과 평가 품질 관점을 설명하면 좋음.',
 10,
 '인터뷰 운영 기준 정리는 채용 품질과 일관성을 높입니다.'
);

-- 개발
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
VALUES
(
 (SELECT content_id FROM ON_CONTENT WHERE title = 'Spring Boot 예외 처리 실무'),
 '개발', 'MULTIPLE_CHOICE',
 'Spring Boot 예외 처리에서 중요한 포인트는 무엇인가요?',
 '예외를 모두 무시한다',
 '일관된 응답 구조와 예외 분류를 유지한다',
 '오류를 그대로 노출한다',
 '로그를 남기지 않는다',
 2, null, null, null, 10,
 '예외 처리는 일관된 응답과 분류가 중요합니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '표준프레임워크 적용 가이드'),
 '개발', 'MULTIPLE_CHOICE',
 '표준프레임워크 적용 가이드 학습 목적은 무엇인가요?',
 '프로젝트 구조와 적용 기준을 이해하기 위해',
 '디자인 시안을 만들기 위해',
 '휴가 정책을 정리하기 위해',
 '영상 편집을 하기 위해',
 1, null, null, null, 10,
 '표준프레임워크는 적용 기준과 구조 이해가 핵심입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = 'AWS 기술 개요'),
 '개발', 'MULTIPLE_CHOICE',
 'AWS 기술 개요 학습의 핵심 목적은 무엇인가요?',
 '클라우드 기본 서비스 구성을 이해하기 위해',
 '근태를 정산하기 위해',
 '채용 인터뷰를 운영하기 위해',
 '디자인 검수를 하기 위해',
 1, null, null, null, 10,
 'AWS 기술 개요는 클라우드 서비스 이해를 돕습니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = 'React 상태 관리 패턴 실습'),
 '개발', 'SHORT_ANSWER',
 'React 상태 관리 패턴을 학습해야 하는 이유를 설명하세요.',
 null, null, null, null, null,
 '상태 관리 패턴을 이해해야 데이터 변화에 따라 화면을 안정적으로 갱신하고 컴포넌트 간 상태 흐름을 관리할 수 있다.',
 '["React","상태 관리","화면 갱신","컴포넌트","흐름"]'::jsonb,
 '상태 변화와 컴포넌트 흐름 관점을 설명하면 좋음.',
 10,
 '상태 관리 패턴은 프론트엔드 구조 안정성에 중요합니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '컴포넌트 설계와 재사용 가이드'),
 '개발', 'MULTIPLE_CHOICE',
 '컴포넌트 재사용 설계의 장점은 무엇인가요?',
 '코드를 반복해서 더 많이 작성할 수 있다',
 '일관성과 유지보수성을 높일 수 있다',
 '스타일만 랜덤하게 바꿀 수 있다',
 '라우팅을 제거할 수 있다',
 2, null, null, null, 10,
 '재사용 가능한 컴포넌트는 일관성과 유지보수성을 높입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '웹 접근성 체크포인트'),
 '개발', 'MULTIPLE_CHOICE',
 '웹 접근성 점검에서 바람직한 대응은 무엇인가요?',
 '시각 디자인만 우선한다',
 '키보드 접근성과 시맨틱 구조를 함께 점검한다',
 '스크린리더를 고려하지 않는다',
 '버튼 대신 div만 사용한다',
 2, null, null, null, 10,
 '접근성은 키보드 탐색과 시맨틱 구조를 함께 봐야 합니다.'
);

-- 디자인
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
VALUES
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '디자인 QA 리뷰 기준'),
 '디자인', 'MULTIPLE_CHOICE',
 '디자인 QA 리뷰 기준의 핵심 목적은 무엇인가요?',
 '화면 일관성과 품질을 점검하기 위해',
 '급여를 정산하기 위해',
 '채용 인터뷰를 진행하기 위해',
 '서버 설정을 하기 위해',
 1, null, null, null, 10,
 '디자인 QA는 품질과 일관성 점검이 핵심입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = 'Figma 디자인시스템 구축'),
 '디자인', 'MULTIPLE_CHOICE',
 '디자인시스템 구축의 장점으로 가장 적절한 것은 무엇인가요?',
 '매번 새로 그리는 시간이 늘어난다',
 '일관성과 재사용성을 높인다',
 '협업을 줄인다',
 '문서 없이 작업하게 한다',
 2, null, null, null, 10,
 '디자인시스템은 일관성과 재사용성을 높입니다.'
),
(
 (SELECT content_id FROM ON_CONTENT WHERE title = '사용자 흐름 설계'),
 '디자인', 'SHORT_ANSWER',
 '사용자 흐름 설계를 학습해야 하는 이유를 설명하세요.',
 null, null, null, null, null,
 '사용자 흐름 설계를 이해하면 서비스 이용 경로를 자연스럽게 설계하고 사용자의 과업 수행 경험을 개선할 수 있다.',
 '["사용자 흐름","경로","과업","경험","설계"]'::jsonb,
 '이용 경로와 사용자 경험 개선 관점을 설명하면 좋음.',
 10,
 '사용자 흐름 설계는 UX 품질 향상에 중요합니다.'
);