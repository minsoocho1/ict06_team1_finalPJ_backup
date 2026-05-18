-- 온보딩 콘텐츠 추가 데이터
-- 작성자: 김다솜
-- 작성일: 2026-05-13
-- 설명: 공통/직군별 로드맵 차별화를 위한 콘텐츠 추가

BEGIN;

INSERT INTO ON_CONTENT
(title, type, category, sub_category, target_position, difficulty, estimated_time, tags, is_mandatory, path)
VALUES
-- 전사 공통
('회사 비전 및 핵심가치 소개', 'VIDEO', '전사', '온보딩', '공통', 'EASY', 12,
 '["vision","culture","onboarding"]'::jsonb, true, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'),
('사내 협업툴 기본 사용 가이드', 'PDF', '전사', '시스템', '공통', 'EASY', 15,
 '["groupware","manual","common"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_GROUPWARE_GUIDE/view'),
('보안 사고 대응 절차 안내', 'PDF', '전사', '보안', '공통', 'MEDIUM', 20,
 '["security","incident","common"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_SECURITY_RESPONSE/view'),
('업무 보고 커뮤니케이션 가이드', 'LINK', '전사', '업무기초', '공통', 'EASY', 15,
 '["report","communication","common"]'::jsonb, false, 'https://www.atlassian.com/ko/work-management/project-management/project-status-reports'),

-- 인사팀
('입퇴사 서류 처리 실무', 'PDF', '인사', '인사운영', '인사팀', 'MEDIUM', 25,
 '["hr","document","ops"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_HR_DOCS/view'),
('근태 정산 및 휴가 정책 실무', 'VIDEO', '인사', '근태', '인사팀', 'MEDIUM', 20,
 '["attendance","vacation","hr"]'::jsonb, false, 'https://www.youtube.com/watch?v=5o0f9W9lF6A'),
('채용 인터뷰 운영 체크포인트', 'LINK', '인사', '채용', '인사팀', 'EASY', 18,
 '["recruiting","interview","hr"]'::jsonb, false, 'https://www.aihr.com/blog/recruitment-process/'),

-- 개발1팀(백엔드)
('Spring Boot 예외 처리 실무', 'VIDEO', '개발', '백엔드', '개발1팀', 'MEDIUM', 22,
 '["spring-boot","backend","exception"]'::jsonb, true, 'https://www.youtube.com/watch?v=9SGDpanrc8U'),
('JPA 성능 최적화 체크리스트', 'PDF', '개발', '백엔드', '개발1팀', 'MEDIUM', 25,
 '["jpa","backend","performance"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_JPA_PERFORMANCE/view'),
('Redis 캐시 적용 패턴', 'LINK', '개발', 'Redis', '개발1팀', 'MEDIUM', 20,
 '["redis","cache","backend"]'::jsonb, false, 'https://redis.io/learn/howtos/solutions/caching-architecture'),

-- 개발2팀(프론트엔드)
('React 상태 관리 패턴 실습', 'VIDEO', '개발', '프론트엔드', '개발2팀', 'MEDIUM', 20,
 '["react","state","frontend"]'::jsonb, true, 'https://www.youtube.com/watch?v=O6P86uwfdR0'),
('컴포넌트 설계와 재사용 가이드', 'PDF', '개발', '프론트엔드', '개발2팀', 'MEDIUM', 18,
 '["component","frontend","guide"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_COMPONENT_GUIDE/view'),
('웹 접근성 체크포인트', 'LINK', '개발', '프론트엔드', '개발2팀', 'EASY', 15,
 '["a11y","frontend","ui"]'::jsonb, false, 'https://developer.mozilla.org/ko/docs/Learn/Accessibility'),

-- 디자인팀
('디자인 QA 리뷰 기준', 'PDF', '디자인', 'QA', '디자인팀', 'EASY', 15,
 '["design","qa","review"]'::jsonb, true, 'https://drive.google.com/file/d/REPLACE_DESIGN_QA/view'),
('Figma 컴포넌트 운영 규칙', 'VIDEO', '디자인', '디자인시스템', '디자인팀', 'MEDIUM', 20,
 '["figma","design-system","design"]'::jsonb, true, 'https://www.youtube.com/watch?v=wc5krC28ynQ'),
('사용자 흐름 설계 사례 읽기', 'LINK', '디자인', 'UX리서치', '디자인팀', 'EASY', 18,
 '["ux","flow","design"]'::jsonb, false, 'https://www.nngroup.com/articles/user-journey-mapping/');

COMMIT;
