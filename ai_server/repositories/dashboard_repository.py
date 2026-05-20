#
#  @FileName : dashboard_repository.py
#  @Description : 관리자 대시보드 통계/최근 활동 전용 DB 모듈
#  @Author : 김다솜
#  @Date : 2026. 05. 13
#  @Modification_History
#  @
#  @ 수정일자        수정자         수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.12    김다솜         database.py 분리를 위한 대시보드 통계 모듈 추가
#  @ 2026.05.13    김다솜         팀별/본부별 온보딩 완료율, 퀴즈 점수 토글용 집계 구조 정리
# 

from datetime import datetime

import pandas as pd

from repositories.db_connection import get_connection


def _employee_base_where():
    # 대시보드 집계 대상 제외 조건(퇴사자, 삭제된 계정, 관리자 권한) 정의
    return """
        e.is_deleted = 'N'
        AND e.status <> '퇴사'
        AND r.role_name <> '관리자'
    """


def _group_expr(level: str) -> str:
    # 레벨 설정(division/team)에 따른 그룹화 대상 칼럼 결정
    if level == "division":
        return "COALESCE(parent.dept_name, d.dept_name)"
    return "d.dept_name"


def analyze_onboarding_stats(level: str = "team"):
    # 부서 레벨별 온보딩 교육 항목 완료율 집계 데이터 산출 로직 (level: team/division)
    conn = get_connection()
    if not conn:
        return []

    try:
        group_expr = _group_expr(level)
        # 팀 레벨 선택 시 parent_dept_id가 존재하는 하위 부서 데이터만 필터링
        level_filter = "AND d.parent_dept_id IS NOT NULL" if level == "team" else ""
        query = f"""
            SELECT
                {group_expr} AS group_name,
                COALESCE(COUNT(ri.item_id), 0) AS total_items,
                COALESCE(SUM(CASE WHEN rp.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_items
            FROM employee e
            JOIN department d ON e.dept_id = d.dept_id
            LEFT JOIN department parent ON d.parent_dept_id = parent.dept_id
            JOIN role r ON e.role_id = r.role_id
            LEFT JOIN roadmap rm ON e.emp_no = rm.emp_no
            LEFT JOIN road_item ri ON rm.roadmap_id = ri.roadmap_id
            LEFT JOIN road_progress rp ON ri.item_id = rp.item_id AND e.emp_no = rp.emp_no
            WHERE {_employee_base_where()}
            {level_filter}
            GROUP BY {group_expr}
            ORDER BY group_name
        """
        results = conn.run(query)
        stats = []
        for group_name, total_items, completed_items in results:
            rate = 0.0 if not total_items else round((completed_items / total_items) * 100, 1)
            stats.append({"dept_name": group_name, "completion_rate": rate})
        return stats
    except Exception as e:
        print(f"온보딩 통계 분석 실패: {e}")
        return []


def analyze_quiz_performance(level: str = "team"):
    # 부서 레벨별 카테고리 평가 평균 점수 산출 로직 (level: team/division)
    conn = get_connection()
    if not conn:
        return []

    try:
        group_expr = _group_expr(level)
        # 팀 레벨 선택 시 하위 부서 데이터 추출을 위한 조건절 생성
        level_filter = "AND d.parent_dept_id IS NOT NULL" if level == "team" else ""
        query = f"""
            SELECT
                {group_expr} AS group_name,
                COALESCE(AVG(sub.total_score), 0) AS avg_score
            FROM employee e
            JOIN department d ON e.dept_id = d.dept_id
            LEFT JOIN department parent ON d.parent_dept_id = parent.dept_id
            JOIN role r ON e.role_id = r.role_id
            LEFT JOIN (
                SELECT
                    qr.emp_no,
                    qq.category_name,
                    qr.submitted_at,
                    SUM(COALESCE(qr.score, 0)) AS total_score
                FROM quiz_result qr
                JOIN quiz_question qq ON qr.question_id = qq.question_id
                WHERE qr.submitted_at IS NOT NULL
                GROUP BY qr.emp_no, qq.category_name, qr.submitted_at
            ) sub ON e.emp_no = sub.emp_no
            WHERE {_employee_base_where()}
            {level_filter}
            GROUP BY {group_expr}
            ORDER BY group_name
        """
        results = conn.run(query)
        return [
            {"dept_name": row[0], "avg_score": round(float(row[1] or 0), 1)}
            for row in results
        ]
    except Exception as e:
        print(f"퀴즈 통계 분석 실패: {e}")
        return []


def analyze_ai_usage_trend():
    # AI 로그 기반 성공한 기능 유형별 사용 횟수 합계 산출
    conn = get_connection()
    if not conn:
        return {}

    try:
        # AI 로그 기반 성공한 기능별 사용 횟수 합계 산출
        query = """
            SELECT
                type,
                COUNT(*) AS usage_count
            FROM ai_log
            WHERE success = TRUE
            GROUP BY type
        """
        results = conn.run(query)
        return {row[0]: int(row[1]) for row in results}
    except Exception as e:
        print(f"AI 사용 통계 분석 실패: {e}")
        return {}


def analyze_rag_status():
    # 문서 테이블 기반 현재 처리 단계별(UPLOADED, CHUNKING 등) 문서 수 집계
    conn = get_connection()
    if not conn:
        return []

    try:
        # 현재 단계별 문서 수 집계 쿼리 수행
        query = """
            SELECT
                current_stage,
                COUNT(*) AS count
            FROM document
            GROUP BY current_stage
        """
        results = conn.run(query)
        df = pd.DataFrame(results, columns=["stage", "count"])
        return df.to_dict(orient="records")
    except Exception as e:
        print(f"RAG 상태 분석 실패: {e}")
        return []


def analyze_evaluation_status():
    # 전체 퀴즈 결과 요약 및 80% 득점 기준 통과/미통과 분포 분석
    conn = get_connection()
    if not conn:
        return []

    try:
        # 80% 기준 통과 여부 판별 및 분포 집계 쿼리 수행
        query = """
            WITH evaluation_summary AS (
                SELECT
                    qr.emp_no,
                    qq.category_name,
                    qr.submitted_at,
                    SUM(COALESCE(qr.score, 0)) AS total_score,
                    SUM(COALESCE(qq.score, 0)) AS max_score,
                    COALESCE(qgr.pass_score, 80) as pass_score
                FROM quiz_result qr
                JOIN quiz_question qq ON qr.question_id = qq.question_id
                LEFT JOIN quiz_generation_rule qgr ON qq.category_name = qgr.category_name AND qgr.is_active = TRUE
                WHERE qr.submitted_at IS NOT NULL
                GROUP BY qr.emp_no, qq.category_name, qr.submitted_at, qgr.pass_score
            )
            SELECT
                CASE
                    WHEN max_score > 0 AND total_score >= max_score * (pass_score / 100.0) THEN '통과'
                    ELSE '미통과'
                END AS result_status,
                COUNT(*) AS result_count
            FROM evaluation_summary
            GROUP BY result_status
            ORDER BY result_status
        """
        results = conn.run(query)
        return [
            {"status": row[0], "count": int(row[1])}
            for row in results
        ]
    except Exception as e:
        print(f"평가 결과 분포 분석 실패: {e}")
        return []


def analyze_category_performance_stats(level: str = "team"):
    # 카테고리별 평균 점수 및 합격 기준점(80%) 비교 데이터 산출 로직 (level: team/division)
    conn = get_connection()
    if not conn:
        return []

    try:
        # 팀 레벨 선택 시 하위 부서 필터링 조건 생성
        level_filter = "AND d.parent_dept_id IS NOT NULL" if level == "team" else ""
        query = f"""
            WITH submission_scores AS (
                SELECT 
                    qr.emp_no, 
                    qq.category_name, 
                    qr.submitted_at,
                    SUM(COALESCE(qr.score, 0)) AS total_score,
                    SUM(COALESCE(qq.score, 0)) AS max_score,
                    COALESCE(qgr.pass_score, 80) as pass_score
                FROM quiz_result qr
                JOIN quiz_question qq ON qr.question_id = qq.question_id
                LEFT JOIN quiz_generation_rule qgr ON qq.category_name = qgr.category_name AND qgr.is_active = TRUE
                JOIN employee e ON qr.emp_no = e.emp_no
                JOIN department d ON e.dept_id = d.dept_id
                JOIN role r ON e.role_id = r.role_id
                WHERE qr.submitted_at IS NOT NULL AND {_employee_base_where()} {level_filter}
                GROUP BY qr.emp_no, qq.category_name, qr.submitted_at, qgr.pass_score
            )
            SELECT 
                category_name,
                ROUND(AVG(CASE WHEN max_score > 0 THEN (total_score::FLOAT / max_score * 100) ELSE 0 END)::numeric, 1) as avg_score,
                AVG(pass_score) as baseline
            FROM submission_scores
            GROUP BY category_name
            ORDER BY category_name
        """
        results = conn.run(query)
        return [{"category_name": row[0], "avg_score": float(row[1]), "baseline": float(row[2])} for row in results]
    except Exception as e:
        print(f"카테고리 성능 분석 실패: {e}")
        return []


def analyze_category_pass_rate_stats(level: str = "team"):
    # 카테고리별 전체 평가 응시 건수 대비 통과(80% 이상 득점) 비율 집계 로직 (level: team/division)
    conn = get_connection()
    if not conn:
        return []

    try:
        # 부서 레벨 선택에 따른 필터링 조건 반영
        level_filter = "AND d.parent_dept_id IS NOT NULL" if level == "team" else ""
        query = f"""
            WITH submission_stats AS (
                SELECT 
                    qr.emp_no, 
                    qq.category_name, 
                    qr.submitted_at,
                    SUM(COALESCE(qr.score, 0)) AS total_score,
                    SUM(COALESCE(qq.score, 0)) AS max_score,
                    COALESCE(qgr.pass_score, 80) as pass_score
                FROM quiz_result qr
                JOIN quiz_question qq ON qr.question_id = qq.question_id
                LEFT JOIN quiz_generation_rule qgr ON qq.category_name = qgr.category_name AND qgr.is_active = TRUE
                JOIN employee e ON qr.emp_no = e.emp_no
                JOIN department d ON e.dept_id = d.dept_id
                JOIN role r ON e.role_id = r.role_id
                WHERE qr.submitted_at IS NOT NULL AND {_employee_base_where()} {level_filter}
                GROUP BY qr.emp_no, qq.category_name, qr.submitted_at, qgr.pass_score
            )
            SELECT 
                category_name,
                ROUND((COUNT(CASE WHEN max_score > 0 AND total_score >= max_score * (pass_score / 100.0) THEN 1 END)::FLOAT / COUNT(*)) * 100, 1) as pass_rate
            FROM submission_stats
            GROUP BY category_name
            ORDER BY category_name
        """
        results = conn.run(query)
        return [{"category_name": row[0], "pass_rate": float(row[1])} for row in results]
    except Exception as e:
        print(f"카테고리 통과율 분석 실패: {e}")
        return []


def analyze_low_understanding_questions(level: str = "team", limit=5):
    # 문항별 정답률(이해도) 기준 하위 구간 문항 추출 및 분석 로직 (level: team/division)
    conn = get_connection()
    if not conn:
        return []

    try:
        # 하위 팀 필터링 반영을 위한 조건절 생성
        level_filter = "AND d.parent_dept_id IS NOT NULL" if level == "team" else ""
        query = f"""
            SELECT 
                qq.question_text,
                qq.category_name,
                ROUND(AVG(CASE 
                    WHEN qr.is_correct = TRUE THEN 1 
                    WHEN qr.is_correct IS NULL AND qq.score > 0 AND (qr.score::FLOAT / qq.score) >= (COALESCE(qgr.pass_score, 80) / 100.0) THEN 1
                    ELSE 0 
                END) * 100, 1) as understanding_rate
            FROM quiz_result qr
            JOIN quiz_question qq ON qr.question_id = qq.question_id
            LEFT JOIN quiz_generation_rule qgr ON qq.category_name = qgr.category_name AND qgr.is_active = TRUE
            JOIN employee e ON qr.emp_no = e.emp_no
            JOIN department d ON e.dept_id = d.dept_id
            JOIN role r ON e.role_id = r.role_id
            WHERE qr.submitted_at IS NOT NULL AND {_employee_base_where()} {level_filter}
            GROUP BY qq.question_id, qq.question_text, qq.category_name, qgr.pass_score
            ORDER BY understanding_rate ASC
            LIMIT {limit}
        """
        results = conn.run(query)
        return [{"question_text": row[0], "category_name": row[1], "understanding_rate": float(row[2])} for row in results]
    except Exception as e:
        print(f"문항 이해도 분석 실패: {e}")
        return []


def analyze_dashboard_kpis():
    # 관리자 홈 KPI 카드(총 인원, 평균 진행률, 평가 제출 수, AI 활동 수) 요약 수치 집계 로직
    conn = get_connection()
    if not conn:
        return {
            "totalOnboarding": 0,
            "avgProgress": 0.0,
            "evaluationSubmissions": 0,
            "aiActivities": 0,
        }

    try:
        query_total = f"""
            SELECT COUNT(*)
            FROM employee e
            JOIN role r ON e.role_id = r.role_id
            WHERE {_employee_base_where()}
        """
        res_total = conn.run(query_total)
        total_onboarding = int(res_total[0][0]) if res_total else 0

        query_avg = f"""
            SELECT COALESCE(AVG(progress_rate), 0)
            FROM (
                SELECT
                    e.emp_no,
                    CASE
                        WHEN COUNT(ri.item_id) = 0 THEN 0
                        ELSE SUM(CASE WHEN rp.status = 'COMPLETED' THEN 1 ELSE 0 END)::FLOAT
                             / COUNT(ri.item_id) * 100
                    END AS progress_rate
                FROM employee e
                JOIN role r ON e.role_id = r.role_id
                LEFT JOIN roadmap rm ON e.emp_no = rm.emp_no
                LEFT JOIN road_item ri ON rm.roadmap_id = ri.roadmap_id
                LEFT JOIN road_progress rp ON ri.item_id = rp.item_id AND rp.emp_no = e.emp_no
                WHERE {_employee_base_where()}
                GROUP BY e.emp_no
            ) progress_base
        """
        res_avg = conn.run(query_avg)
        avg_progress = round(float(res_avg[0][0] or 0), 1) if res_avg else 0.0

        query_eval = """
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT qr.emp_no, qq.category_name, qr.submitted_at
                FROM quiz_result qr
                JOIN quiz_question qq ON qr.question_id = qq.question_id
                WHERE qr.submitted_at IS NOT NULL
            ) submitted
        """
        res_eval = conn.run(query_eval)
        evaluation_submissions = int(res_eval[0][0]) if res_eval else 0

        query_ai = """
            SELECT COUNT(*)
            FROM ai_log
            WHERE success = TRUE
        """
        res_ai = conn.run(query_ai)
        ai_activities = int(res_ai[0][0]) if res_ai else 0

        return {
            "totalOnboarding": total_onboarding,
            "avgProgress": avg_progress,
            "evaluationSubmissions": evaluation_submissions,
            "aiActivities": ai_activities,
        }
    except Exception as e:
        print(f"KPI 분석 실패: {e}")
        return {
            "totalOnboarding": 0,
            "avgProgress": 0.0,
            "evaluationSubmissions": 0,
            "aiActivities": 0,
        }


def _format_activity_time(event_time):
    # 활동 발생 시간과 현재 시간의 차이를 계산하여 상대 시간 문자열(방금 전, n분 전 등)로 변환 처리
    if event_time is None:
        return "방금 전"

    now = datetime.now()
    delta = now - event_time
    seconds = int(delta.total_seconds())

    if seconds < 60:
        return "방금 전"
    if seconds < 3600:
        return f"{seconds // 60}분 전"
    if seconds < 86400:
        return f"{seconds // 3600}시간 전"
    if seconds < 172800:
        return "어제"
    return event_time.strftime("%m-%d %H:%M")


def analyze_recent_activities(limit=5):
    # RAG 처리, 평가 제출, AI 비서 활용 등 최근 시스템 주요 활동 목록 통합 정렬 및 반환 로직
    conn = get_connection()
    if not conn:
        return []

    activities = []

    try:
        document_rows = conn.run("""
            SELECT
                d.title,
                l.stage,
                l.status,
                COALESCE(l.ended_at, l.updated_at, l.created_at) AS event_time
            FROM document_process_log l
            JOIN document d ON l.doc_id = d.doc_id
            WHERE COALESCE(l.ended_at, l.updated_at, l.created_at) IS NOT NULL
            ORDER BY event_time DESC
            LIMIT 5
        """)

        for title, stage, status, event_time in document_rows:
            stage_label = {"CHUNK": "청크", "EMBED": "벡터", "PUBLISH": "게시"}.get(stage, "문서")
            status_label = "완료" if status == "SUCCESS" else "실패"
            activities.append(
                {
                    "event_time": event_time,
                    "time_label": _format_activity_time(event_time),
                    "title": "문서/RAG",
                    "message": f"{title} 문서의 {stage_label} 처리 {status_label}",
                    "url": "/admin/onboarding/documents",
                    "borderClass": "border-success" if status == "SUCCESS" else "border-danger",
                    "textClass": "text-success" if status == "SUCCESS" else "text-danger",
                }
            )

        quiz_rows = conn.run("""
            SELECT
                e.name,
                qq.category_name,
                qr.submitted_at
            FROM (
                SELECT DISTINCT emp_no, question_id, submitted_at
                FROM quiz_result
                WHERE submitted_at IS NOT NULL
            ) qr
            JOIN quiz_question qq ON qr.question_id = qq.question_id
            JOIN employee e ON qr.emp_no = e.emp_no
            ORDER BY qr.submitted_at DESC
            LIMIT 5
        """)

        for name, category_name, event_time in quiz_rows:
            activities.append(
                {
                    "event_time": event_time,
                    "time_label": _format_activity_time(event_time),
                    "title": "온보딩 평가",
                    "message": f"{name}님이 {category_name} 카테고리 평가를 제출했습니다.",
                    "url": "/admin/evaluation/main",
                    "borderClass": "border-primary",
                    "textClass": "text-primary",
                }
            )

        ai_rows = conn.run("""
            SELECT
                COALESCE(e.name, '사용자'),
                a.type,
                a.created_at
            FROM ai_log a
            LEFT JOIN employee e ON a.emp_no = e.emp_no
            WHERE a.created_at IS NOT NULL
            ORDER BY a.created_at DESC
            LIMIT 5
        """)

        for name, ai_type, event_time in ai_rows:
            activities.append(
                {
                    "event_time": event_time,
                    "time_label": _format_activity_time(event_time),
                    "title": "AI 비서",
                    "message": f"{name}님이 {ai_type} 기능을 사용했습니다.",
                    "url": "/admin/home",
                    "borderClass": "border-warning",
                    "textClass": "text-warning",
                }
            )

        activities.sort(key=lambda item: item["event_time"] or datetime.min, reverse=True)

        recent = []
        for item in activities[:limit]:
            item.pop("event_time", None)
            recent.append(item)
        return recent
    except Exception as e:
        print(f"최근 활동 분석 실패: {e}")
        return []
