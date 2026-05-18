#
#  @FileName : main.py
#  @Description : AI 서버 메인 엔트리포인트
#  @Author : 김다솜
#  @Date : 2026. 05. 13
#  @Modification_History
#  @
#  @ 수정일자        수정자         수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.04.27    김다솜         최초 생성 및 FastAPI 기본 서버 구성
#  @ 2026.05.11    김다솜         관리자 대시보드 통계 API 및 문서 처리 트리거 구성
#  @ 2026.05.12    김다솜         Spring 연동 경로에 맞춘 AI 평가/퀴즈/문서 처리 라우터 등록
#  @ 2026.05.13    김다솜         팀별/본부별 차트 토글용 대시보드 통계 구조 확장
#  @ 2026.05.14    김다솜         위치 기반 날씨 정보 및 독려 메시지 반환 API 추가 및 메시지 다양화 (시간대별 메시지 포함)
#

import os
from pathlib import Path

from dotenv import load_dotenv
from fastapi import BackgroundTasks, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime # 현재 시간 가져오기 위해 import
import random
from fastapi import Query # Query 파라미터 사용을 위해 import
import requests
from pydantic import BaseModel

from api.documents.ai_documents import router as document_router
from api.evaluation.ai_evaluation import router as evaluation_router
from api.roadmap.ai_roadmap import router as roadmap_router
from repositories import dashboard_repository
from schemas.evaluation_schema import AiEvaluationRequest, AiEvaluationResponse, AiQuizGenerationRequest
from services import ollama_client
from services import evaluation_service

load_dotenv(dotenv_path=Path(__file__).parent / ".env")

app = FastAPI(
    title="COREWORK AI Server",
    description="온보딩 로드맵, 평가, 퀴즈, 문서/RAG 처리를 담당하는 AI 서버",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://localhost:8081",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(evaluation_router, prefix="/api/ai/evaluation", tags=["AI Evaluation"])
app.include_router(document_router, prefix="/api/ai/documents", tags=["AI Documents"])
app.include_router(roadmap_router, prefix="/api/ai", tags=["AI Roadmap"])


class RagProcessRequest(BaseModel):
    doc_id: int
    file_path: str


@app.get("/")
def read_root():
    """
    AI 서버 기동 상태 확인 엔드포인트
    """
    return {
        "service": "COREWORK AI Server",
        "status": "running",
    }


@app.get("/health")
def health_check():
    """
    AI 서버 상태 확인 엔드포인트
    """
    return {
        "status": "ok",
        "server": "ai",
        "port": 8000,
    }


@app.get("/api/stats/dashboard", tags=["Dashboard"])
def get_dashboard_stats():
    """
    관리자 대시보드 통계 데이터 반환
    """
    return {
        "kpis": dashboard_repository.analyze_dashboard_kpis(),
        "onboardingTeam": dashboard_repository.analyze_onboarding_stats("team"),
        "onboardingDivision": dashboard_repository.analyze_onboarding_stats("division"),
        "rag": dashboard_repository.analyze_rag_status(),
        "evaluationStatus": dashboard_repository.analyze_evaluation_status(),
        "ai_usage": dashboard_repository.analyze_ai_usage_trend(),
        "quizTeam": dashboard_repository.analyze_quiz_performance("team"),
        "quizDivision": dashboard_repository.analyze_quiz_performance("division"),
        "recentActivities": dashboard_repository.analyze_recent_activities(),
    }


@app.post("/api/rag/process", tags=["RAG"])
def trigger_rag_processing(req: RagProcessRequest, background_tasks: BackgroundTasks):
    """
    문서/RAG 처리 요청 비동기 트리거 엔드포인트
    """
    return {
        "status": "processing",
        "message": f"Document {req.doc_id} is being processed in background.",
    }


@app.post("/api/ai/evaluate", tags=["Legacy AI"])
def evaluate_answer(req: AiEvaluationRequest):
    """
    기존 테스트 경로 주관식 채점 엔드포인트
    """
    return evaluation_service.evaluate_answer(req)


@app.post("/evaluate", response_model=AiEvaluationResponse, tags=["Legacy AI"])
async def ai_evaluation_endpoint(req: AiEvaluationRequest):
    """
    Swagger 테스트용 평가 엔드포인트
    """
    return evaluation_service.evaluate_answer(req)


@app.post("/api/ai/generate-quiz", tags=["Legacy AI"])
def generate_quiz(req: AiQuizGenerationRequest):
    """
    기존 테스트 경로 퀴즈 생성 엔드포인트
    """
    return evaluation_service.generate_quiz_drafts(req)


@app.get("/api/stats/evaluation/category-performance", tags=["Evaluation Statistics"])
def get_category_performance_stats(level: str = Query("team", description="필터링 레벨 (team 또는 division)")):
    """
    카테고리별 평균 점수 vs 기준점 통계 데이터 반환 엔드포인트
    """
    return dashboard_repository.analyze_category_performance_stats(level)


@app.get("/api/stats/evaluation/category-pass-rate", tags=["Evaluation Statistics"])
def get_category_pass_rate_stats(level: str = Query("team", description="필터링 레벨 (team 또는 division)")):
    """
    카테고리별 통과율 통계 데이터 반환 엔드포인트
    """
    return dashboard_repository.analyze_category_pass_rate_stats(level)


@app.get("/api/stats/evaluation/low-understanding-questions", tags=["Evaluation Statistics"])
def get_low_understanding_questions(level: str = Query("team", description="필터링 레벨 (team 또는 division)"), limit: int = Query(5, description="하위 이해도 문항 반환 개수")):
    """
    문항별 이해도 하위 구간 통계 데이터 반환 엔드포인트
    """
    return dashboard_repository.analyze_low_understanding_questions(level, limit)

@app.post("/api/ai/evaluation/analyze-stats", tags=["Evaluation Statistics"])
def analyze_evaluation_stats(stats_summary: dict):
    """
    전체 평가 통계 데이터를 기반으로 AI 분석 코멘트 생성
    """
    prompt = f"""
    당신은 기업 교육 전문가입니다. 아래의 온보딩 평가 지표를 분석하여 관리자에게 핵심 인사이트를 요약해 주세요.
    지표: {stats_summary}
    조건: 한국어로 답변하고, 가장 우수한 카테고리와 보충이 필요한 카테고리를 언급하며 격려하는 어조로 작성해 주세요. 3문장 내외로 짧게 작성하세요.
    """
    analysis = ollama_client.chat(prompt)
    if not analysis:
        return {"analysis": "AI 서버(Ollama)로부터 응답을 받지 못했습니다. 설정 또는 모델 상태를 확인해 주세요."}
    return {"analysis": analysis}

@app.get("/api/weather", tags=["External"])
def get_current_weather(lat: float = Query(...), lon: float = Query(...)):
    # 현재 위치 기반 실시간 날씨 정보를 조회하고 AI를 이용한 응원 메시지 생성
    api_key = os.getenv("OPENWEATHER_API_KEY")
    if not api_key:
        return {"status": "error", "message": "API Key is missing in .env"}

    url = f"https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={api_key}&units=metric&lang=kr"
    try:
        response = requests.get(url, timeout=5)
        response.raise_for_status()
        data = response.json()

        # 날씨 기반 독려 메시지 생성 로직
        weather_main = data['weather'][0]['main'] # Clear, Clouds, Rain, Snow 등
        weather_desc = data['weather'][0]['description'] # 맑음, 구름 많음, 가벼운 비 등
        temp = data['main']['temp']
        current_hour = datetime.now().hour # 현재 시간 (0-23시)
        
        # 1. AI(Ollama)를 통한 실시간 메시지 생성 시도
        ai_message = ollama_client.generate_weather_encouragement(weather_desc, temp, current_hour)
        
        if ai_message:
            data['encouragement_message'] = ai_message
        else:
            # 2. AI 비활성 또는 실패 시 폴백 메시지 무작위 선택
            fallbacks = [
                "오늘도 COREWORK와 함께 활기찬 하루 보내세요!",
                "당신의 노력이 빛나는 하루가 되기를 응원합니다!",
                "계획하신 일들이 모두 잘 풀리는 기분 좋은 하루 되세요."
            ]
            data['encouragement_message'] = random.choice(fallbacks)

        return data
    except Exception as e:
        return {"status": "error", "message": str(e)}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
