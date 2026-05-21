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
#  @ 2026.05.19    김다솜         자기 평가와 AI 평가 비교 피드백 생성 스키마 추가
#  @ 2026.05.19    김다솜         주관식 채점 문맥과 채점 기준 필드 추가
# 
from typing import List

from pydantic import BaseModel, Field


class AiEvaluationRequest(BaseModel):
    user_answer: str
    reference_answer: str
    question_text: str | None = None
    keyword_answer: str | None = None
    rubric: str | None = None
    explanation: str | None = None


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


class AiSelfCheckReviewItem(BaseModel):
    question: str | None = None
    user_answer: str | None = None
    correct_answer: str | None = None
    explanation: str | None = None
    score: int | None = None
    max_score: int | None = None
    is_correct: bool | None = None


class AiSelfCheckFeedbackRequest(BaseModel):
    content_title: str
    category_name: str | None = None
    self_score_rate: float
    evaluation_score_rate: float | None = None
    score_gap: float | None = None
    need_more_explanation: bool = False
    memo: str | None = None
    review_items: List[AiSelfCheckReviewItem] = Field(default_factory=list)


class AiSelfCheckFeedbackResponse(BaseModel):
    feedback: str
