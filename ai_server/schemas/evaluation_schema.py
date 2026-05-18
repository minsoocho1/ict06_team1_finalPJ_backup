# 
#  @FileName : evaluation_schema.py
#  @Description : AI 평가/퀴즈 생성 요청 및 응답 스키마
#  @Author : 김다솜
#  @Date : 2026. 05. 13
#  @Modification_History
#  @
#  @ 수정일자        수정자         수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.02    김다솜         초기 생성 및 AI 평가 스키마 정의
#  @ 2026.05.11    김다솜         AI 퀴즈 자동 생성 요청/응답 스키마 추가
#  @ 2026.05.13    김다솜         RAG 문맥 필드 및 주관식 초안 필드 확장
# 
from typing import List

from pydantic import BaseModel


class AiEvaluationRequest(BaseModel):
    user_answer: str
    reference_answer: str


class AiEvaluationResponse(BaseModel):
    score: float
    feedback: str
    similarity: float


class AiQuizGenerationRequest(BaseModel):
    content_id: int
    title: str
    category: str | None = None
    sub_category: str | None = None
    content_type: str | None = None
    difficulty: str | None = None
    question_count: int = 3
    tags: str | None = None
    path: str | None = None
    rag_summary: str | None = None
    rag_context: str | None = None


class AiQuizDraftItem(BaseModel):
    questionType: str
    questionText: str
    option1: str | None = None
    option2: str | None = None
    option3: str | None = None
    option4: str | None = None
    answerNo: int | None = None
    sampleAnswer: str | None = None
    keywordAnswer: List[str] | None = None
    rubric: str | None = None
    score: int | None = 10
    explanation: str


class AiQuizGenerationResponse(BaseModel):
    questions: List[AiQuizDraftItem]
