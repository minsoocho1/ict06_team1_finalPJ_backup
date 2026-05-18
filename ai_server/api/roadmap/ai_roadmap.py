#
#  @FileName : ai_roadmap.py
#  @Description : AI 온보딩 로드맵 API Router
#                 - 사원별 맞춤 로드맵 생성 및 조회
#                 - Gemini API 기반 로드맵 추천 (llama-3.3-70b-versatile)
#                 - 콘텐츠 상세 조회
#  @Author : 김다솜
#  @Date : 2026. 04. 27
#  @Modification_History
#  @
#  @ 수정일         수정자        수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.04.27    김다솜        최초 생성 및 Gemini 기반 로드맵 추천 API 구현
#  @ 2026.05.04    김다솜        main.py에서 라우터로 분리
#  @ 2026.05.06    김다솜        GEMINI_API_KEY 함수 내부로 이동 (load_dotenv 타이밍 문제 해결),
#                               Gemini → Groq API로 교체 (llama-3.3-70b-versatile),
#

import os
import json
from groq import Groq
from fastapi import APIRouter
from database import (
    fetch_employee_info,
    fetch_existing_roadmap,
    save_ai_roadmap,
    get_on_content_list,
    fetch_content_detail
)

router = APIRouter()

# True: AI 호출 없이 mock 데이터 반환 / False: Gemini API 실제 호출
USE_MOCK = False

# 플랜B: AI API 호출 실패 또는 개발 테스트용 임시 로드맵 데이터
# 0. 임시 데이터 정의
mock_roadmap = [
    {
        "category_name": "필수이수교육",
        "items": [
            {"item_id": 1, "item_title": "2026 신입사원 필독: 조직문화 및 비전 교육", "content_id": 1, "status": "COMPLETED", "rate": 100},
            {"item_id": 2, "item_title": "근태 관리 및 전자결재 시스템 사용 가이드", "content_id": 2, "status": "IN_PROGRESS", "rate": 40},
            {"item_id": 3, "item_title": "개인정보 보호 및 정보보안 기본 교육", "content_id": 3, "status": "NOT_STARTED", "rate": 0},
            {"item_id": 4, "item_title": "직장 내 성희롱 예방 교육", "content_id": 4, "status": "NOT_STARTED", "rate": 0},
        ],
    },
    {
        "category_name": "직무교육 (백엔드)",
        "items": [
            {"item_id": 5, "item_title": "Java Spring Boot 프로젝트 구조 이해", "content_id": 12, "status": "IN_PROGRESS", "rate": 80},
            {"item_id": 6, "item_title": "Spring Security와 JWT 인증 흐름", "content_id": 13, "status": "NOT_STARTED", "rate": 0},
            {"item_id": 7, "item_title": "JPA 연관관계 매핑 실무", "content_id": 14, "status": "NOT_STARTED", "rate": 0},
            {"item_id": 8, "item_title": "트랜잭션과 동시성 처리 기초", "content_id": 15, "status": "NOT_STARTED", "rate": 0},
        ],
    },
    {
        "category_name": "직무교육 (프론트엔드)",
        "items": [
            {"item_id": 9, "item_title": "React 컴포넌트 구조와 Props 관리", "content_id": 19, "status": "IN_PROGRESS", "rate": 50},
            {"item_id": 10, "item_title": "React Router 기반 페이지 라우팅", "content_id": 20, "status": "IN_PROGRESS", "rate": 20},
            {"item_id": 11, "item_title": "Axios와 API 통신 패턴", "content_id": 21, "status": "NOT_STARTED", "rate": 0},
            {"item_id": 12, "item_title": "상태 관리와 Context API 활용", "content_id": 22, "status": "NOT_STARTED", "rate": 0},
        ],
    },
]

@router.get("/roadmap/{emp_no}")
def get_roadmap(emp_no: str):
    GROQ_API_KEY = os.getenv("GROQ_API_KEY")
    client = Groq(api_key=GROQ_API_KEY)

    try:
        # 1. 사원 정보 조회
        emp_info = fetch_employee_info(emp_no)
        print(f"👉 입력받은 사번: {emp_no} (타입: {type(emp_no)})")
        print(f"👉 DB 결과값: {emp_info} (타입: {type(emp_info)})")
        if not emp_info:
            return {"error": "사원 정보를 찾을 수 없습니다."}

        # 2-1. 기존 로드맵 있으면 AI 호출 없이 바로 반환(중복 생성 방지)
        existing_data = fetch_existing_roadmap(emp_no)
        if existing_data:
            print(f"✅ [캐시] 기존 로드맵 반환 (사번: {emp_no}, 항목 수: {sum(len(g['items']) for g in existing_data)}개)")
            return {
                "name": emp_info.get("name"),
                "recommended_roadmap": existing_data,
                "is_mock": False,
                "is_cached": True
            }
            
        # 2-2. 개발 모드: AI 호출 없이 mock 데이터 반환
        if USE_MOCK:
            print("⚠️ 개발 모드: AI 호출 생략, mock 로드맵 사용")
            return {
                "name": emp_info.get("name"),
                "recommended_roadmap": mock_roadmap,
                "is_mock": True
            }

        # 3. AI 프롬프트 구성
        emp_name = emp_info.get('name', '사용자')
        dept_name = emp_info.get('dept_name', '해당 부서')
        pos_name = emp_info.get('position_name', '사원')
        
        print(f"🤖 [AI 생성] Groq 로드맵 생성 시작 (사번: {emp_no})")
        content_list_str = get_on_content_list()

        prompt_text = f"""
사원 {emp_name} (부서: {dept_name}, 직급: {pos_name})을 위한 맞춤형 온보딩 로드맵을 작성해줘.

사내 강의 목록 (형식: ID|제목|분류|세부분류|필수여부|난이도):
{content_list_str}

지시사항:
1. 필수(T) 콘텐츠는 반드시 '필수이수교육' 카테고리에 포함할 것.
2. 부서({dept_name})와 직무에 적합한 강의를 골라 직무교육에 배치할 것.
3. 난이도가 높거나 전문적인 강의는 '심화교육'에 배치할 것.
4. 전체 로드맵은 5~8개의 강의로 구성하고, 반드시 제공된 ID(content_id)만 사용할 것.
5. 카테고리명은 반드시 아래 5개를 그대로 사용할 것. 절대 합치거나 변경하지 말 것:
   - 필수이수교육
   - 직무교육 (백엔드)
   - 직무교육 (프론트엔드)
   - 심화교육
   - AI 활용 교육

반드시 아래 키를 가진 JSON 객체 형식으로만 출력해. 다른 텍스트 없이 JSON만:
{
  "recommended_roadmap": [
    {"category_name": "필수이수교육", "items": [{"item_title": "강의제목", "content_id": 1}]},
    {"category_name": "직무교육 (백엔드)", "items": [{"item_title": "강의제목", "content_id": 2}]},
    {"category_name": "직무교육 (프론트엔드)", "items": [{"item_title": "강의제목", "content_id": 3}]},
    {"category_name": "심화교육", "items": [{"item_title": "강의제목", "content_id": 4}]},
    {"category_name": "AI 활용 교육", "items": [{"item_title": "강의제목", "content_id": 5}]}
  ]
}
"""

        # API 호출
        print(f"\n ===== AI 요청 시작(사번: {emp_no}) =====")
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "당신은 온보딩 로드맵 설계 전문가입니다. 반드시 'recommended_roadmap' 키를 가진 JSON 객체 형식으로 답변하세요."
                },
                {
                    "role": "user",
                    "content": prompt_text
                }
            ],
            temperature=0.2,
            response_format={"type": "json_object"}
        )

        # 6. AI 응답 파싱
        ai_text = response.choices[0].message.content.strip()
        
        if ai_text.startswith("```"):
            ai_text = ai_text.split("```")[1]
            if ai_text.startswith("json"):
                ai_text = ai_text[4:]
        ai_text = ai_text.strip()
        
        # JSON 객체에서 리스트 추출
        result_json = json.loads(ai_text)
        if isinstance(result_json, dict) and "recommended_roadmap" in result_json:
            roadmap_items = result_json["recommended_roadmap"]
        else:
            roadmap_items = result_json # fallback
        
        print(f"🤖 AI 생성 로드맵 (항목 수: {len(roadmap_items)}개 카테고리)")
            
        # 7. DB에 최종 저장 및 결과 반환
        save_success = save_ai_roadmap(
            dept_id=emp_info.get("dept_id"),
            position_id=emp_info.get("position_id"),
            title=f"{emp_info.get('name')} 님 맞춤 로드맵",
            roadmap_items=roadmap_items,
            emp_no=emp_no
        )
        
        if save_success:
            print(f"💾 [저장 완료] 사번: {emp_no}, 항목 수: {sum(len(g['items']) for g in roadmap_items)}개")
        else:
            print(f"⚠️ [저장 실패] 사번: {emp_no}")
    
        return {
            "name": emp_info.get("name"),
            "recommended_roadmap": roadmap_items,
            "saved": save_success,
            **({"warning": "DB STORAGE FAILED"} if not save_success else {})
        }
    
    except Exception as e:
        # AI 호출 실패 시 mock 데이터로 fallback (React 화면 정상 렌더링 보장)
        import traceback
        print("🚨 [상세 에러 발생 위치 및 내용] 🚨")
        traceback.print_exc()
        return {
            "name": emp_info.get("name") if 'emp_info' in locals() else "사용자",
            "recommended_roadmap": mock_roadmap,
            "is_mock": True,
            "error_msg": str(e)
        }

# =========================
# 콘텐츠 상세 조회 API
# 
# 로드맵 화면에서 선택한 콘텐츠(content_id) 기반, ON_CONTENT 테이블에서 상세 정보 조회-반환
# LearningDetail.js에서 콘텐츠 타입(VIDEO/PDF/LINK..)에 따라 렌더링할 데이터 제공하는 역할
# :param content_id: 콘텐츠 식별자
# :return: 콘텐츠 상세 정보 JSON
# =========================
@router.get("/content/{content_id}")
def get_content_detail(content_id: int):
    # DB에서 콘텐츠 조회
    content = fetch_content_detail(content_id)
    if not content:
        return {"error": "콘텐츠를 찾을 수 없습니다."}

    return content