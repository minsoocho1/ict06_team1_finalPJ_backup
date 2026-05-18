#
#  @FileName : roadmap_repository.py
#  @Description : AI 로드맵/콘텐츠 조회 및 저장 전용 DB 모듈
#  @Author : 김다솜
#  @Date : 2026. 05. 12
#  @Modification_History
#  @
#  @ 수정일자        수정자          수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.12    김다솜        database.py 분리를 위한 로드맵/콘텐츠 조회 모듈 추가
#

from repositories.db_connection import get_connection


def fetch_employee_info(emp_no):
    """
    로드맵 추천용 사원 기본 정보/소속 정보 조회
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
    기존 로드맵 카테고리별 아이템 구조 조회
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
    AI 생성 로드맵 ROADMAP/ROAD_ITEM 저장
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
    AI 프롬프트용 온보딩 콘텐츠 목록 텍스트 변환
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
    학습 상세/AI 문제 생성용 콘텐츠 상세 정보 조회
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
