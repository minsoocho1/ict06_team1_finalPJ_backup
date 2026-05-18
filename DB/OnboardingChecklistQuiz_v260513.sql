-- 체크리스트, 퀴즈 보강 데이터
-- 작성자 : 김다솜
-- 작성일 : 2026-05-13
-- 설명 : 2026-05-13 추가 콘텐츠 기준 체크리스트와 퀴즈 보강

BEGIN;

------------------------------------------------------------
-- 1. CHECKLIST 보강
------------------------------------------------------------

-- 직접 체크형
INSERT INTO CHECKLIST
(title, category, description, is_mandatory, related_content_id, checklist_type, order_no)
SELECT
    v.title,
    v.category,
    v.description,
    v.is_mandatory,
    v.related_content_id::integer,
    v.checklist_type,
    v.order_no
FROM (
    VALUES
    ('입사 첫 주 업무 우선순위 확인', '전사', '첫 주에 우선 확인해야 할 학습/업무 순서를 점검합니다.', true, NULL, 'USER', 41),
    ('팀 협업 채널 초대 여부 확인', '전사', '메신저, 협업툴, 메일 그룹 초대 여부를 확인합니다.', true, NULL, 'USER', 42),
    ('인사 서류 제출 상태 점검', '인사', '인사 서류 제출 및 보완 요청 여부를 확인합니다.', true, NULL, 'USER', 43),
    ('개발 로컬 환경 실행 점검', '개발', '개발 프로젝트가 로컬에서 정상 실행되는지 확인합니다.', true, NULL, 'USER', 44),
    ('디자인 파일 접근 권한 확인', '디자인', 'Figma 및 디자인 자산 접근 권한을 확인합니다.', true, NULL, 'USER', 45)
) AS v(title, category, description, is_mandatory, related_content_id, checklist_type, order_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM CHECKLIST c
    WHERE c.title = v.title
);
INSERT INTO CHECKLIST
(title, category, description, is_mandatory, related_content_id, checklist_type, order_no)
SELECT
    v.title,
    v.category,
    v.description,
    v.is_mandatory,
    c.content_id,
    v.checklist_type,
    v.order_no
FROM (
    VALUES
    ('회사 비전 및 핵심가치 소개 시청', '전사', '회사 비전과 핵심가치를 이해합니다.', true, '회사 비전 및 핵심가치 소개', 'SYSTEM', 46),
    ('사내 협업툴 기본 사용 가이드 확인', '전사', '기본 협업 도구 사용 방식을 숙지합니다.', true, '사내 협업툴 기본 사용 가이드', 'SYSTEM', 47),
    ('보안 사고 대응 절차 안내 확인', '전사', '보안 사고 대응 절차를 이해합니다.', true, '보안 사고 대응 절차 안내', 'SYSTEM', 48),
    ('업무 보고 커뮤니케이션 가이드 확인', '전사', '업무 보고 기본 원칙을 이해합니다.', false, '업무 보고 커뮤니케이션 가이드', 'SYSTEM', 49),
    ('입퇴사 서류 처리 실무 학습', '인사', '인사 운영 실무 문서 처리 절차를 학습합니다.', true, '입퇴사 서류 처리 실무', 'SYSTEM', 50),
    ('근태 정산 및 휴가 정책 실무 학습', '인사', '근태/휴가 운영 기준을 학습합니다.', false, '근태 정산 및 휴가 정책 실무', 'SYSTEM', 51),
    ('채용 인터뷰 운영 체크포인트 학습', '인사', '채용 운영 기본 흐름을 학습합니다.', false, '채용 인터뷰 운영 체크포인트', 'SYSTEM', 52),
    ('Spring Boot 예외 처리 실무 학습', '개발', '예외 처리 구조와 패턴을 학습합니다.', true, 'Spring Boot 예외 처리 실무', 'SYSTEM', 53),
    ('JPA 성능 최적화 체크리스트 확인', '개발', 'JPA 성능 이슈 점검 항목을 확인합니다.', true, 'JPA 성능 최적화 체크리스트', 'SYSTEM', 54),
    ('Redis 캐시 적용 패턴 학습', '개발', 'Redis 캐시 설계 패턴을 학습합니다.', false, 'Redis 캐시 적용 패턴', 'SYSTEM', 55),
    ('React 상태 관리 패턴 실습 학습', '개발', '상태 관리 패턴을 실습 중심으로 학습합니다.', true, 'React 상태 관리 패턴 실습', 'SYSTEM', 56),
    ('컴포넌트 설계와 재사용 가이드 확인', '개발', '컴포넌트 재사용 원칙을 확인합니다.', true, '컴포넌트 설계와 재사용 가이드', 'SYSTEM', 57),
    ('웹 접근성 체크포인트 확인', '개발', '프론트엔드 접근성 기본 항목을 확인합니다.', false, '웹 접근성 체크포인트', 'SYSTEM', 58),
    ('디자인 QA 리뷰 기준 확인', '디자인', '디자인 QA 리뷰 기준을 확인합니다.', true, '디자인 QA 리뷰 기준', 'SYSTEM', 59),
    ('Figma 컴포넌트 운영 규칙 학습', '디자인', '디자인 시스템 운영 규칙을 학습합니다.', true, 'Figma 컴포넌트 운영 규칙', 'SYSTEM', 60),
    ('사용자 흐름 설계 사례 읽기 학습', '디자인', 'UX 흐름 설계 사례를 읽고 이해합니다.', false, '사용자 흐름 설계 사례 읽기', 'SYSTEM', 61)
) AS v(title, category, description, is_mandatory, content_title, checklist_type, order_no)
JOIN ON_CONTENT c
  ON c.title = v.content_title
WHERE NOT EXISTS (
    SELECT 1
    FROM CHECKLIST ck
    WHERE ck.title = v.title
);

------------------------------------------------------------
-- 2. QUIZ_GENERATION_RULE 보강
------------------------------------------------------------

INSERT INTO QUIZ_GENERATION_RULE
(category_name, question_count, pass_score, difficulty, question_type, is_active)
SELECT *
FROM (
    VALUES
    ('전사', 5, 80, 'EASY', 'MULTIPLE_CHOICE', true),
    ('인사', 4, 80, 'MEDIUM', 'MULTIPLE_CHOICE', true),
    ('개발', 8, 80, 'MEDIUM', 'MULTIPLE_CHOICE', true),
    ('디자인', 4, 80, 'EASY', 'MULTIPLE_CHOICE', true)
) AS v(category_name, question_count, pass_score, difficulty, question_type, is_active)
WHERE NOT EXISTS (
    SELECT 1
    FROM QUIZ_GENERATION_RULE r
    WHERE r.category_name = v.category_name
);

------------------------------------------------------------
-- 3. QUIZ_QUESTION 보강
------------------------------------------------------------

-- 전사
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '전사', 'MULTIPLE_CHOICE',
    '회사 비전 및 핵심가치 소개 콘텐츠의 주요 목적은 무엇인가요?',
    '행사 일정만 공유하기 위해',
    '조직의 방향성과 일하는 기준을 이해시키기 위해',
    '출근 시간을 기록하기 위해',
    '평가 점수만 올리기 위해',
    2, null, null, null, 10,
    '비전과 핵심가치는 조직 이해와 업무 기준 정렬을 돕습니다.'
FROM ON_CONTENT c
WHERE c.title = '회사 비전 및 핵심가치 소개'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '회사 비전 및 핵심가치 소개 콘텐츠의 주요 목적은 무엇인가요?'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '전사', 'SHORT_ANSWER',
    '사내 협업툴 기본 사용 가이드를 먼저 확인해야 하는 이유를 설명하세요.',
    null, null, null, null, null,
    '사내 협업툴 사용법을 먼저 알아야 메신저, 결재, 일정, 공지 확인 등 기본 업무를 빠르게 수행할 수 있다.',
    '["협업툴","기본 업무","공지","일정","결재"]'::jsonb,
    '기본 업무 수행과 협업 적응 관점을 설명하면 좋음.',
    10,
    '협업툴은 입사 초기 기본 업무 적응에 직접 연결됩니다.'
FROM ON_CONTENT c
WHERE c.title = '사내 협업툴 기본 사용 가이드'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '사내 협업툴 기본 사용 가이드를 먼저 확인해야 하는 이유를 설명하세요.'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '전사', 'MULTIPLE_CHOICE',
    '보안 사고 대응 절차 안내에서 가장 중요한 행동 원칙은 무엇인가요?',
    '문제를 혼자 숨기고 해결한다',
    '이상 징후를 즉시 보고하고 절차에 따라 대응한다',
    '증거를 지운 뒤 보고한다',
    '관련 없는 부서에만 먼저 알린다',
    2, null, null, null, 10,
    '보안 사고는 즉시 보고와 절차 기반 대응이 핵심입니다.'
FROM ON_CONTENT c
WHERE c.title = '보안 사고 대응 절차 안내'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '보안 사고 대응 절차 안내에서 가장 중요한 행동 원칙은 무엇인가요?'
  );

-- 인사
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '인사', 'MULTIPLE_CHOICE',
    '입퇴사 서류 처리 실무에서 가장 중요한 것은 무엇인가요?',
    '서류를 모아두고 나중에 한꺼번에 처리한다',
    '정확한 문서 확인과 누락 없는 처리다',
    '구두 안내만으로 대체한다',
    '승인 없이 임의 수정한다',
    2, null, null, null, 10,
    '인사 서류는 정확성과 누락 방지가 핵심입니다.'
FROM ON_CONTENT c
WHERE c.title = '입퇴사 서류 처리 실무'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '입퇴사 서류 처리 실무에서 가장 중요한 것은 무엇인가요?'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '인사', 'SHORT_ANSWER',
    '근태 정산 및 휴가 정책 실무를 이해해야 하는 이유를 설명하세요.',
    null, null, null, null, null,
    '근태와 휴가 정책을 이해해야 근무 기록, 휴가 승인, 정산 기준을 정확히 적용할 수 있다.',
    '["근태","휴가","정산","기준","승인"]'::jsonb,
    '근무 기록과 휴가 운영 정확성 관점을 설명하면 좋음.',
    10,
    '근태/휴가 정책 이해는 인사 운영 정확성과 직접 연결됩니다.'
FROM ON_CONTENT c
WHERE c.title = '근태 정산 및 휴가 정책 실무'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '근태 정산 및 휴가 정책 실무를 이해해야 하는 이유를 설명하세요.'
  );

-- 개발
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '개발', 'MULTIPLE_CHOICE',
    'Spring Boot 예외 처리 실무에서 중요한 포인트는 무엇인가요?',
    '예외를 모두 무시한다',
    '일관된 응답 구조와 예외 분류를 유지한다',
    '오류를 클라이언트에 그대로 노출한다',
    '로그를 남기지 않는다',
    2, null, null, null, 10,
    '예외 처리는 일관된 응답과 분류가 중요합니다.'
FROM ON_CONTENT c
WHERE c.title = 'Spring Boot 예외 처리 실무'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = 'Spring Boot 예외 처리 실무에서 중요한 포인트는 무엇인가요?'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '개발', 'MULTIPLE_CHOICE',
    'JPA 성능 최적화 체크리스트에서 주의할 항목으로 적절한 것은 무엇인가요?',
    '무조건 즉시 로딩만 사용한다',
    'N+1 문제와 불필요한 조회를 점검한다',
    '모든 쿼리를 native로만 작성한다',
    '인덱스를 절대 사용하지 않는다',
    2, null, null, null, 10,
    'JPA 성능 최적화는 N+1, fetch 전략, 불필요한 조회 점검이 중요합니다.'
FROM ON_CONTENT c
WHERE c.title = 'JPA 성능 최적화 체크리스트'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = 'JPA 성능 최적화 체크리스트에서 주의할 항목으로 적절한 것은 무엇인가요?'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '개발', 'SHORT_ANSWER',
    'Redis 캐시 적용 패턴을 이해해야 하는 이유를 설명하세요.',
    null, null, null, null, null,
    'Redis 캐시 패턴을 이해하면 조회 성능을 높이고 반복 요청 부하를 줄이면서 적절한 캐시 무효화 전략을 설계할 수 있다.',
    '["Redis","캐시","성능","부하","무효화"]'::jsonb,
    '성능 개선과 캐시 무효화 관점을 설명하면 좋음.',
    10,
    '캐시 패턴 이해는 성능과 데이터 일관성 설계에 중요합니다.'
FROM ON_CONTENT c
WHERE c.title = 'Redis 캐시 적용 패턴'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = 'Redis 캐시 적용 패턴을 이해해야 하는 이유를 설명하세요.'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '개발', 'MULTIPLE_CHOICE',
    '웹 접근성 체크포인트에서 가장 바람직한 대응은 무엇인가요?',
    '시각 디자인만 우선한다',
    '키보드 접근성과 시맨틱 구조를 함께 점검한다',
    '스크린리더를 고려하지 않는다',
    '버튼 대신 div만 사용한다',
    2, null, null, null, 10,
    '접근성은 키보드 탐색, 시맨틱 구조, 보조기기 지원을 함께 봐야 합니다.'
FROM ON_CONTENT c
WHERE c.title = '웹 접근성 체크포인트'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '웹 접근성 체크포인트에서 가장 바람직한 대응은 무엇인가요?'
  );

-- 디자인
INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '디자인', 'MULTIPLE_CHOICE',
    '디자인 QA 리뷰 기준의 핵심 목적은 무엇인가요?',
    '파일명을 통일하기 위해',
    '구현 결과가 디자인 의도와 일치하는지 확인하기 위해',
    '배포를 늦추기 위해',
    '레이어 개수를 줄이기 위해',
    2, null, null, null, 10,
    '디자인 QA는 구현 품질과 디자인 일치 여부를 확인하는 과정입니다.'
FROM ON_CONTENT c
WHERE c.title = '디자인 QA 리뷰 기준'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = '디자인 QA 리뷰 기준의 핵심 목적은 무엇인가요?'
  );

INSERT INTO QUIZ_QUESTION
(content_id, category_name, question_type, question_text, option_1, option_2, option_3, option_4, answer_no, sample_answer, keyword_answer, rubric, score, explanation)
SELECT
    c.content_id, '디자인', 'SHORT_ANSWER',
    'Figma 컴포넌트 운영 규칙을 지켜야 하는 이유를 설명하세요.',
    null, null, null, null, null,
    'Figma 컴포넌트 운영 규칙을 지켜야 일관된 디자인 시스템을 유지하고 협업 시 재사용성과 변경 반영 효율을 높일 수 있다.',
    '["Figma","컴포넌트","일관성","재사용","협업"]'::jsonb,
    '일관성, 재사용성, 협업 효율 관점을 설명하면 좋음.',
    10,
    '운영 규칙은 디자인 시스템 품질과 협업 효율에 중요합니다.'
FROM ON_CONTENT c
WHERE c.title = 'Figma 컴포넌트 운영 규칙'
  AND NOT EXISTS (
      SELECT 1 FROM QUIZ_QUESTION q
      WHERE q.question_text = 'Figma 컴포넌트 운영 규칙을 지켜야 하는 이유를 설명하세요.'
  );

COMMIT;
