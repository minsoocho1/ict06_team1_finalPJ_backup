# 
#  @FileName : ai_evaluation.py
#  @Description : AI 평가 API Router
#                 - 주관식 답변 AI 채점 요청 처리
#                 - 콘텐츠 기준 객관식 퀴즈 초안 생성 요청 처리
#  @Author : 김다솜
#  @Date : 2026. 05. 02
#  @Modification_History
#  @
#  @ 수정일자        수정자        수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.02    김다솜        최초 생성 및 AI 채점 API 분리
#  @ 2026.05.11    김다솜        AI 퀴즈 자동 생성 API 추가
# 
from fastapi import APIRouter

from schemas.evaluation_schema import (
    AiEvaluationRequest,
    AiEvaluationResponse,
    AiQuizGenerationRequest,
    AiQuizGenerationResponse
)
from services.evaluation_service import evaluate_answer, generate_quiz_drafts

router = APIRouter()


@router.post("/evaluate", response_model=AiEvaluationResponse)
def evaluate(req: AiEvaluationRequest):
    return evaluate_answer(req)


# 콘텐츠 기준 AI 퀴즈 초안 생성
@router.post("/generate", response_model=AiQuizGenerationResponse)
def generate(req: AiQuizGenerationRequest):
    return generate_quiz_drafts(req)