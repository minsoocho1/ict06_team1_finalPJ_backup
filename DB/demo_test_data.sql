-- 1. 조직 및 기본 데이터 (부서, 직급)
INSERT INTO department (dept_id, dept_name) VALUES (10, '개발본부'), (20, '인사팀');
INSERT INTO position (position_id, position_name) VALUES (1, '사원'), (2, '팀장');

-- 2. 테스트 사원 생성 (신규 입사자: 김민수)
-- 비밀번호는 '1234'의 해싱값 가정 (TempPassword 실행 결과값 사용 권장)
INSERT INTO employee (emp_no, name, dept_id, position_id, status, hire_date, account_no, password) 
VALUES ('20240001', '김민수', 10, 1, '재직', CURRENT_DATE, '110-123-456789', '$2a$10$7696Kyz6pE8Y.KInW5C9Ge0gScl.BCz/S7.6vG3z9FvV5Vj8vV9.a');

-- 3. 온보딩 교육 콘텐츠 생성
INSERT INTO on_content (content_id, title, type, category, is_mandatory, target_position, difficulty, path)
VALUES 
(101, '사내 보안 규정 가이드', 'PDF', '공통', true, '공통', 'EASY', '/storage/docs/security_guide.pdf'),
(102, '백엔드 개발 표준 가이드', 'LINK', '기술', false, '개발', 'NORMAL', 'https://wiki.company.com/dev-standard'),
(103, '그룹웨어 사용법 교육', 'VIDEO', '공통', true, '공통', 'EASY', 'https://youtube.com/example_link');

-- 4. 온보딩 문서(RAG용) 정보 등록
INSERT INTO document (doc_id, title, file_path, current_stage, created_at, dept_id, access_level)
VALUES (501, '사내 보안 규정 가이드', '/storage/docs/security_guide.pdf', 'PUBLISHED', CURRENT_TIMESTAMP, 20, 'PUBLIC');

-- 5. 로드맵 생성
INSERT INTO roadmap (roadmap_id, emp_no, title, generated_type, is_completed, version)
VALUES (2001, '20240001', '김민수 Onboarding Roadmap', 'MANUAL', false, 1);

-- 6. 로드맵 아이템 생성 (학습 일정 포함)
-- RID 태그 매칭을 위해 ID를 명시적으로 지정
INSERT INTO road_item (item_id, roadmap_id, content_id, item_title, category_name, order_no, start_date, due_date)
VALUES 
(1001, 2001, 101, '사내 보안 규정 가이드', 'Mandatory Training', 1, CURRENT_DATE - 2, CURRENT_DATE),
(1002, 2001, 102, '백엔드 개발 표준 가이드', 'Job Training', 2, CURRENT_DATE, CURRENT_DATE + 3),
(1003, 2001, 103, '그룹웨어 사용법 교육', 'Mandatory Training', 3, CURRENT_DATE + 3, CURRENT_DATE + 5);

-- 7. 학습 진행도(Progress) 초기화
-- 1번 항목: 완료(COMPLETED), 2번 항목: 진행중(IN_PROGRESS), 3번 항목: 시작전(NOT_STARTED)
INSERT INTO road_progress (emp_no, item_id, status, rate, updated_at)
VALUES 
('20240001', 1001, 'COMPLETED', 100.00, CURRENT_TIMESTAMP),
('20240001', 1002, 'IN_PROGRESS', 45.00, CURRENT_TIMESTAMP),
('20240001', 1003, 'NOT_STARTED', 0.00, CURRENT_TIMESTAMP);

-- 8. 체크리스트 데이터 생성
-- USER 타입: 기본 필수 항목, SYSTEM 타입: 학습 연동 항목
INSERT INTO checklist (checklist_id, title, category, checklist_type, is_mandatory, related_content_id, order_no)
VALUES 
(401, '인사팀 방문 및 사원증 수령', '행정', 'USER', true, NULL, 1),
(402, '사내 보안 규정 숙지 여부 확인', '보안', 'SYSTEM', true, 101, 2),
(403, '개발 환경 셋업 완료', '기술', 'USER', false, NULL, 3);

-- 9. 체크리스트 진행도 초기화
-- 402번은 학습 완료(1001번 아이템)와 연동되어 자동 완료된 상태
INSERT INTO checklist_progress (emp_no, checklist_id, status, completed_at)
VALUES 
('20240001', 401, 'COMPLETED', CURRENT_TIMESTAMP - 1),
('20240001', 402, 'COMPLETED', CURRENT_TIMESTAMP - 1),
('20240001', 403, 'NOT_STARTED', NULL);

-- 10. 캘린더 일정(Schedule) 연동
-- RoadmapServiceImpl의 syncToCalendar 로직을 반영한 데이터
INSERT INTO schedule (title, content, start_time, end_time, creator_no, category, is_public, dept_id)
VALUES 
('[온보딩] 사내 보안 규정 가이드', '보안 규정을 숙지하세요.\n\n[RID:1001]', (CURRENT_DATE - 2)::timestamp, CURRENT_DATE::timestamp + interval '23 hours 59 minutes', '20240001', 'ONBOARDING', false, 10),
('[온보딩] 백엔드 개발 표준 가이드', '표준 가이드를 학습하세요.\n\n[RID:1002]', CURRENT_DATE::timestamp, (CURRENT_DATE + 3)::timestamp + interval '23 hours 59 minutes', '20240001', 'ONBOARDING', false, 10),
('[온보딩] 그룹웨어 사용법 교육', '그룹웨어 사용법 영상 시청\n\n[RID:1003]', (CURRENT_DATE + 3)::timestamp, (CURRENT_DATE + 5)::timestamp + interval '23 hours 59 minutes', '20240001', 'ONBOARDING', false, 10);

-- 11. 인공지능 퀴즈 및 평가 결과 (시연용 데이터)
INSERT INTO evaluation_question (question_id, content_id, category_name, question_text, answer_text, explanation, score)
VALUES (601, 101, '보안', '사내 망에서 외부 저장매체 사용은 원칙적으로 금지입니까?', 'O', '[AI자동생성] 보안 규정 제3조에 의거함', 10);

INSERT INTO quiz_result (result_id, emp_no, question_id, user_answer, is_correct, score, created_at)
VALUES (701, '20240001', 601, 'O', true, 10, CURRENT_TIMESTAMP);

-- 시나리오 확인 쿼리 (정상 입력 확인용)
-- SELECT e.name, r.title, i.item_title, p.status, p.rate 
-- FROM employee e 
-- JOIN roadmap r ON e.emp_no = r.emp_no 
-- JOIN road_item i ON r.roadmap_id = i.roadmap_id 
-- JOIN road_progress p ON i.item_id = p.item_id 
-- WHERE e.emp_no = '20240001';