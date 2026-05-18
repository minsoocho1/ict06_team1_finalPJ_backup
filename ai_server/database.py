# 
#  @FileName : database.py
#  @Description : AI 서버용 DB 조회/저장 모듈
#                 - AI 로드맵 추천용 데이터 조회 및 저장
#                 - 온보딩 콘텐츠/상세 정보 조회
#                 - 관리자 대시보드 통계 및 최근 활동 조회
#  @Author : 김다솜
#  @Date : 2026. 04. 28
#  @Modification_History
#  @
#  @ 수정일자        수정자          수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.04.27    김다솜         최초 생성 및 AI 로드맵 추천용 DB 조회 함수 구현
#  @ 2026.04.28    김다솜         교육 그룹 기반 로드맵 저장 구조 반영 및 ON_CONTENT 조회 확장
#  @ 2026.05.11    김다솜         관리자 대시보드 실제 통계 데이터 연동
#  @ 2026.05.12    김다솜         평가/AI 로그 기반 KPI 정리 및 최근 시스템 활동 분석 추가
#

from datetime import datetime

import pandas as pd
from repositories.db_connection import get_connection


def fetch_employee_info(emp_no):
    """
    로드맵 추천에 필요한 사원 기본 정보와 소속 정보 조회
    """
    conn = get_connection()
    if not conn:
        return None

    try:
        query = """
            SELECT
                e.name,
                d.dept_name,
                p.position_name,
                e.dept_id,
                e.position_id
            FROM employee e
            JOIN department d ON e.dept_id = d.dept_id
            JOIN position p ON e.position_id = p.position_id
            WHERE e.emp_no = :emp_no
        """
        result = conn.run(query, emp_no=emp_no)

        if not result:
            return None

        row = result[0]
        return {
            "name": row[0],
            "dept_name": row[1],
            "position_name": row[2],
            "dept_id": row[3],
            "position_id": row[4],
        }
    except Exception as e:
        print(f"사원 정보 조회 실패: {e}")
        return None


def fetch_existing_roadmap(emp_no):
    """
    이미 생성된 로드맵이 있으면 카테고리별 아이템 구조로 묶어서 반환
    """
    conn = get_connection()
    if not conn:
        return None

    try:
        query = """
            SELECT
                i.category_name,
                i.item_id,
                i.item_title,
                i.content_id,
                COALESCE(p.status, 'NOT_STARTED') AS status,
                COALESCE(p.rate, 0) AS rate,
                i.order_no
            FROM ROADMAP r
            JOIN ROAD_ITEM i ON r.roadmap_id = i.roadmap_id
            LEFT JOIN ROAD_PROGRESS p
                ON p.item_id = i.item_id
               AND p.emp_no = :emp_no
            WHERE r.emp_no = :emp_no
            ORDER BY i.order_no
        """
        result = conn.run(query, emp_no=emp_no)

        if not result:
            return None

        grouped = {}
        for row in result:
            category_name = row[0]
            grouped.setdefault(category_name, []).append(
                {
                    "item_id": row[1],
                    "item_title": row[2],
                    "content_id": row[3],
                    "status": row[4],
                    "rate": float(row[5] or 0),
                }
            )

        return [
            {"category_name": category, "items": items}
            for category, items in grouped.items()
        ]
    except Exception as e:
        print(f"기존 로드맵 조회 실패: {e}")
        return None


def save_ai_roadmap(dept_id, position_id, title, roadmap_items, emp_no):
    """
    AI가 생성한 로드맵을 ROADMAP, ROAD_ITEM 테이블에 저장
    """
    conn = get_connection()
    if not conn:
        return False

    try:
        sql_roadmap = """
            INSERT INTO ROADMAP (
                title, dept_id, position_id, emp_no,
                generated_type, version, is_completed, created_at, updated_at
            )
            VALUES (
                :title, :dept_id, :position_id, :emp_no,
                'AI', 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            RETURNING roadmap_id
        """

        res = conn.run(
            sql_roadmap,
            title=title,
            dept_id=dept_id,
            position_id=position_id,
            emp_no=emp_no,
        )
        roadmap_id = res[0][0]

        sql_item = """
            INSERT INTO ROAD_ITEM (
                roadmap_id, content_id, item_title, category_name, order_no
            )
            VALUES (
                :roadmap_id, :content_id, :item_title, :category_name, :order_no
            )
        """

        order_no = 1
        for group in roadmap_items:
            category_name = group["category_name"]
            for item in group["items"]:
                conn.run(
                    sql_item,
                    roadmap_id=roadmap_id,
                    content_id=item["content_id"],
                    item_title=item["item_title"],
                    category_name=category_name,
                    order_no=order_no,
                )
                order_no += 1

        return True
    except Exception as e:
        print(f"로드맵 저장 실패: {e}")
        return False


def get_on_content_list():
    """
    AI 프롬프트에 넣을 온보딩 콘텐츠 목록을 텍스트 형태로 정리해 반환한다.
    """
    conn = get_connection()
    if not conn:
        return ""

    try:
        query = """
            SELECT
                content_id, title, type, category, sub_category,
                target_position, difficulty, estimated_time, tags, is_mandatory
            FROM ON_CONTENT
            ORDER BY content_id
        """

        results = conn.run(query)
        content_items = []
        for row in results:
            content_items.append(
                f"ID:{row[0]} | "
                f"제목:{row[1]} | "
                f"유형:{row[2]} | "
                f"분류:{row[3]} | "
                f"세부분류:{row[4]} | "
                f"대상직무:{row[5]} | "
                f"난이도:{row[6]} | "
                f"예상학습시간:{row[7]}분 | "
                f"태그:{row[8]} | "
                f"필수여부:{row[9]}"
            )
        return "\n".join(content_items)
    except Exception as e:
        print(f"콘텐츠 목록 조회 실패: {e}")
        return ""


def fetch_content_detail(content_id):
    """
    학습 상세 화면과 AI 문제 생성에 필요한 콘텐츠 상세 정보 반환
    """
    conn = get_connection()
    if not conn:
        return None

    try:
        query = """
            SELECT
                content_id, title, type, category, sub_category,
                target_position, difficulty, estimated_time, tags, is_mandatory, path
            FROM ON_CONTENT
            WHERE content_id = :content_id
        """
        result = conn.run(query, content_id=int(content_id))

        if not result:
            return None

        row = result[0]
        return {
            "content_id": row[0],
            "title": row[1],
            "type": row[2],
            "category": row[3],
            "sub_category": row[4],
            "target_position": row[5],
            "difficulty": row[6],
            "estimated_time": row[7],
            "tags": row[8],
            "is_mandatory": row[9],
            "path": row[10],
        }
    except Exception as e:
        print(f"콘텐츠 상세 조회 실패: {e}")
        return None


def analyze_onboarding_stats():
    """
    부서별 온보딩 완료율을 계산해 차트용 데이터로 반환
    """
    conn = get_connection()
    if not conn:
        return []

    try:
        query = """
            SELECT
                d.dept_name,
                COALESCE(COUNT(ri.item_id), 0) as total_items,
                COALESCE(SUM(CASE WHEN rp.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) as completed_items
            FROM department d
            LEFT JOIN employee e ON d.dept_id = e.dept_id
            LEFT JOIN ROADMAP r ON e.emp_no = r.emp_no
            LEFT JOIN ROAD_ITEM ri ON r.roadmap_id = ri.roadmap_id
            LEFT JOIN ROAD_PROGRESS rp ON ri.item_id = rp.item_id AND e.emp_no = rp.emp_no
            GROUP BY d.dept_id, d.dept_name
            ORDER BY d.dept_id
        """
        results = conn.run(query)

        stats = []
        for dept_name, total_items, completed_items in results:
            rate = 0.0 if not total_items else round((completed_items / total_items) * 100, 1)
            stats.append({"dept_name": dept_name, "completion_rate": rate})
        return stats
    except Exception as e:
        print(f"온보딩 통계 분석 실패: {e}")
        return []


def analyze_quiz_performance():
    """
    부서별 평균 퀴즈 점수를 계산해 차트용 데이터로 반환한다.
    """
    conn = get_connection()
    if not conn:
        return []

    try:
        query = """
            SELECT
                d.dept_name,
                COALESCE(AVG(sub.total_score), 0) as avg_score
            FROM department d
            LEFT JOIN employee e ON d.dept_id = e.dept_id
            LEFT JOIN (
                SELECT
                    qr.emp_no,
                    qq.category_name,
                    qr.submitted_at,
                    SUM(COALESCE(qr.score, 0)) as total_score
                FROM QUIZ_RESULT qr
                JOIN QUIZ_QUESTION qq ON qr.question_id = qq.question_id
                WHERE qr.submitted_at IS NOT NULL
                GROUP BY qr.emp_no, qq.category_name, qr.submitted_at
            ) sub ON e.emp_no = sub.emp_no
            GROUP BY d.dept_id, d.dept_name
            ORDER BY d.dept_id
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
    """
    AI_LOG 기준으로 AI 기능 사용 유형별 건수를 집계한다.
    """
    conn = get_connection()
    if not conn:
        return {}

    try:
        query = """
            SELECT
                type,
                COUNT(*) as usage_count
            FROM AI_LOG
            WHERE success = TRUE
            GROUP BY type
        """
        results = conn.run(query)
        return {row[0]: int(row[1]) for row in results}
    except Exception as e:
        print(f"AI 사용 통계 분석 실패: {e}")
        return {}


def analyze_rag_status():
    """
    문서 처리 단계별 건수를 집계해 RAG 처리 현황 차트 데이터로 반환한다.
    """
    conn = get_connection()
    if not conn:
        return []

    try:
        query = """
            SELECT
                current_stage,
                COUNT(*) as count
            FROM DOCUMENT
            GROUP BY current_stage
        """
        results = conn.run(query)
        df = pd.DataFrame(results, columns=["stage", "count"])
        return df.to_dict(orient="records")
    except Exception as e:
        print(f"RAG 상태 분석 실패: {e}")
        return []


def analyze_dashboard_kpis():
    """
    관리자 홈 KPI 카드에 필요한 요약 수치를 계산한다.
    """
    conn = get_connection()
    if not conn:
        return {
            "totalOnboarding": 0,
            "avgProgress": 0.0,
            "evaluationSubmissions": 0,
            "aiActivities": 0,
        }

    try:
        res_total = conn.run("SELECT COUNT(DISTINCT emp_no) FROM ROADMAP")
        total_onboarding = int(res_total[0][0]) if res_total else 0

        query_avg = """
            SELECT
                CAST(COUNT(rp.item_id) AS FLOAT) / NULLIF(COUNT(ri.item_id), 0) * 100 as avg_rate
            FROM ROAD_ITEM ri
            LEFT JOIN ROAD_PROGRESS rp
              ON ri.item_id = rp.item_id
             AND rp.status = 'COMPLETED'
        """
        res_avg = conn.run(query_avg)
        avg_progress = round(float(res_avg[0][0] or 0), 1) if res_avg else 0.0

        query_eval = """
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT qr.emp_no, qq.category_name, qr.submitted_at
                FROM QUIZ_RESULT qr
                JOIN QUIZ_QUESTION qq ON qr.question_id = qq.question_id
                WHERE qr.submitted_at IS NOT NULL
            ) submitted
        """
        res_eval = conn.run(query_eval)
        evaluation_submissions = int(res_eval[0][0]) if res_eval else 0

        query_ai = """
            SELECT COUNT(*)
            FROM AI_LOG
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
    """
    최근 활동 시간을 상대 시간 형식으로 변환한다.
    """
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
    """
    문서 처리, 평가 제출, AI 사용 로그를 합쳐 최근 시스템 활동 목록을 반환한다.
    """
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
                COALESCE(l.ended_at, l.updated_at, l.created_at) as event_time
            FROM DOCUMENT_PROCESS_LOG l
            JOIN DOCUMENT d ON l.doc_id = d.doc_id
            WHERE COALESCE(l.ended_at, l.updated_at, l.created_at) IS NOT NULL
            ORDER BY event_time DESC
            LIMIT 5
        """)

        for title, stage, status, event_time in document_rows:
            stage_label = {"CHUNK": "청크", "EMBED": "벡터", "REFLECT": "반영"}.get(stage, "문서")
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
                FROM QUIZ_RESULT
                WHERE submitted_at IS NOT NULL
            ) qr
            JOIN QUIZ_QUESTION qq ON qr.question_id = qq.question_id
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
            FROM AI_LOG a
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


if __name__ == "__main__":
    connection = get_connection()
    if connection:
        print("DB 연결 성공")
    else:
        print("DB 연결 실패")
