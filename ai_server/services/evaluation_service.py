# 
#  @FileName : evaluation_service.py
#  @Description : AI 평가 서비스 로직
#  @Author : 김다솜
#  @Date : 2026. 05. 13
#  @Modification_History
#  @
#  @ 수정일자        수정자        수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.02    김다솜         초기 생성 및 AI 평가 처리 로직 정의
#  @ 2026.05.06    김다솜         Groq API 기반 주관식 채점 로직 반영
#  @ 2026.05.11    김다솜         AI 퀴즈 자동 생성 로직 추가
#  @ 2026.05.13    김다솜         RAG 문맥 우선 출제 기준 및 객관식/단답형 혼합 초안 생성 강화
#  @ 2026.05.19    김다솜         자기 평가와 AI 평가 비교 기반 학습 피드백 생성 추가
#  @ 2026.05.19    김다솜         문항 문맥과 채점 기준 기반 주관식 유사도 보정 추가
#  @ 2026.05.19    김다솜         자기 평가 피드백 프롬프트 용어를 서비스 문맥에 맞게 조정
#  @ 2026.05.19    김다솜         자기 평가 피드백 불렛 및 줄바꿈 형식 개선
# 

import json
import os

from groq import Groq

from schemas.evaluation_schema import (
    AiEvaluationRequest,
    AiEvaluationResponse,
    AiQuizDraftItem,
    AiQuizGenerationRequest,
    AiQuizGenerationResponse,
    AiSelfCheckFeedbackRequest,
    AiSelfCheckFeedbackResponse,
)
from utils.nlp_helper import get_semantic_similarity


# 주관식 AI 채점 함수
def evaluate_answer(req: AiEvaluationRequest) -> AiEvaluationResponse:
    client = Groq(api_key=os.getenv("GROQ_API_KEY"))

    user_answer = req.user_answer.strip()
    reference_answer = req.reference_answer.strip()

    if not user_answer:
        return AiEvaluationResponse(
            score=0,
            feedback="답변이 입력되지 않았습니다.",
            similarity=0,
        )

    nlp_similarity = get_semantic_similarity(user_answer, reference_answer)

    scoring_guideline = (
        "- 기준 답안이 짧아도 사용자가 같은 뜻을 풀어 썼다면 높게 반영\n"
        "- 문항, 기준 답안, 키워드, 채점 기준, 풀이 도움말을 함께 보고 포괄 여부 판단\n"
        "- 핵심 키워드 포함 여부\n"
        "- 업무 지식으로서의 논리와 정확성\n"
        "- 불필요한 내용이 조금 있어도 정답 요지를 포함하면 과도하게 감점하지 않음\n"
        "- 단순 키워드 나열이 아닌 문장 수준의 설명 여부"
    )

    prompt = f"""
당신은 온보딩 교육 평가 전문가입니다.
아래 NLP 유사도 점수는 참고값입니다. 기준 답안이 짧고 사용자 답변이 풀어쓴 형태라면, 문맥과 포함 관계를 직접 판단하여 similarity를 보정하세요.
NLP 참고 유사도: {nlp_similarity}

[채점 기준]
{scoring_guideline}

[문항]
{req.question_text or "없음"}

[기준 답안]
{reference_answer}

[핵심 키워드]
{req.keyword_answer or "없음"}

[루브릭]
{req.rubric or "없음"}

[풀이 도움말]
{req.explanation or "없음"}

[사용자 답변]
{user_answer}

아래 JSON 형식으로만 응답하세요.
{{
  "score": 0~100 사이 점수,
  "similarity": 0.0~1.0 사이 숫자,
  "feedback": "구체적인 피드백 2~3문장. similarity가 NLP 참고값과 달라졌다면 왜 보정했는지 짧게 설명"
}}
"""

    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "당신은 교육 평가 전문가입니다. 반드시 JSON 형식으로만 응답하세요.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.2,
            response_format={"type": "json_object"},
        )

        raw = response.choices[0].message.content.strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"):
                raw = raw[4:]
        raw = raw.strip()

        result = json.loads(raw)
        ai_similarity = clamp_similarity(result.get("similarity", nlp_similarity))

        return AiEvaluationResponse(
            score=int(result["score"]),
            feedback=str(result["feedback"]),
            similarity=ai_similarity,
        )

    except Exception as e:
        print(f"[Groq 평가 오류] {e}")
        return AiEvaluationResponse(
            score=70 if user_answer else 0,
            feedback=f"AI 채점 중 오류가 발생하여 테스트용 결과를 반환합니다. {str(e)}",
            similarity=0.65 if user_answer else 0.0,
        )


def clamp_similarity(value) -> float:
    try:
        number_value = float(value)
    except (TypeError, ValueError):
        return 0.0
    return round(max(0.0, min(1.0, number_value)), 2)


def generate_self_check_feedback(req: AiSelfCheckFeedbackRequest) -> AiSelfCheckFeedbackResponse:
    client = Groq(api_key=os.getenv("GROQ_API_KEY"))
    review_items = req.review_items[:3] if req.review_items else []
    review_text = "\n".join(
        [
            (
                f"- 문제: {item.question or '없음'}\n"
                f"  내 답: {item.user_answer or '없음'}\n"
                f"  맞는 답/도움말: {item.correct_answer or item.explanation or '없음'}\n"
                f"  받은 점수: {item.score if item.score is not None else '-'}"
                f"/{item.max_score if item.max_score is not None else '-'}"
            )
            for item in review_items
        ]
    ) or "- 틀렸거나 덜 맞은 문제가 많지 않습니다."

    prompt = f"""
사용자의 온보딩 학습 자기 평가와 AI 평가 결과를 비교해, 바로 실행할 수 있는 피드백을 작성하세요.

[학습 정보]
- 콘텐츠: {req.content_title}
- 카테고리: {req.category_name or '없음'}
- 자기 평가: {req.self_score_rate}%
- AI 평가: {req.evaluation_score_rate if req.evaluation_score_rate is not None else '없음'}%
- 차이: {req.score_gap if req.score_gap is not None else '없음'}%
- 추가 설명이 필요한 콘텐츠: {req.need_more_explanation}
- 내가 작성한 의견: {req.memo or '없음'}

[다시 볼 문제]
{review_text}

[작성 조건]
- 한국어로만 작성하세요.
- 한자 표기는 한글 표기로 바꾸되, 자기 평가, AI 평가, 차이, 추가 설명, 콘텐츠, 문제, 풀이 도움말 같은 서비스 용어는 그대로 사용하세요.
- 지나치게 쉬운 말로 바꾸지 말고 사용자에게 자연스러운 교육 피드백 문장으로 작성하세요.
- 콘텐츠 이름과 다시 볼 문제를 반드시 언급하세요.
- 어떤 콘텐츠의 어느 문제 또는 어느 풀이 도움말을 다시 봐야 하는지 구체적으로 말하세요.
- 사용자가 바로 할 수 있는 행동 1~2개를 넣으세요.
- 제목 1줄과 불렛 2~3개로 구성하세요.
- 각 불렛은 "- "로 시작하고 줄바꿈으로 구분하세요.
- 첫 줄에는 자기 평가와 AI 평가의 관계를 요약하세요.
- 불렛에는 다시 볼 콘텐츠/문제, 보완 행동, 추가 설명 필요 여부를 나누어 적으세요.
- 전체 6줄 이내로 작성하세요.
- JSON만 반환하세요.

반환 형식:
{{
  "feedback": "요약 문장\n- 불렛 1\n- 불렛 2"
}}
"""

    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "당신은 온보딩 학습 도우미입니다. 한자 표기는 한글로 쓰되 서비스 용어는 자연스럽게 유지하고 반드시 JSON만 반환하세요.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.35,
            response_format={"type": "json_object"},
        )

        raw = response.choices[0].message.content.strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"):
                raw = raw[4:]
        parsed = json.loads(raw.strip())
        feedback = str(parsed.get("feedback", "")).strip()
        if not feedback:
            raise ValueError("피드백 문장이 비어 있습니다.")
        return AiSelfCheckFeedbackResponse(feedback=feedback)

    except Exception as e:
        print(f"[Groq 자기 평가 피드백 오류] {e}")
        return AiSelfCheckFeedbackResponse(
            feedback=build_self_check_fallback(req, review_items)
        )


def build_self_check_fallback(req: AiSelfCheckFeedbackRequest, review_items: list) -> str:
    score_part = (
        f"자기 평가 {req.self_score_rate}%, AI 평가 {req.evaluation_score_rate}%, 차이 {req.score_gap}%입니다."
        if req.evaluation_score_rate is not None
        else f"자기 평가 {req.self_score_rate}%가 저장되었습니다."
    )
    if review_items:
        first = review_items[0]
        review_part = f"'{req.content_title}'에서 '{first.question}' 문제와 풀이 도움말을 먼저 다시 보세요."
    else:
        review_part = f"'{req.content_title}'에서 핵심 개념과 적용 순서를 한 번 더 정리해 보세요."
    extra_part = " 추가 설명이 필요하다고 표시했으니 이 내용을 AI 학습 도우미에 질문해 보세요." if req.need_more_explanation else ""
    return (
        f"{score_part}\n"
        f"- 다시 볼 콘텐츠: {review_part}\n"
        f"- 보완 방법: 본인 말로 짧게 요약한 뒤 다시 풀어 보면 좋습니다.\n"
        f"- 추가 설명: {extra_part.strip() if extra_part else '현재 선택된 추가 설명 요청은 없습니다.'}"
    )


# 콘텐츠 기반 객관식/단답형 퀴즈 초안 생성 함수
def generate_quiz_drafts(req: AiQuizGenerationRequest) -> AiQuizGenerationResponse:
    difficulty = (req.difficulty or "EASY").upper()
    rag_section = ""
    title_only_section = f"[학습 콘텐츠 제목]\n{req.title}\n"

    difficulty_instructions = {
        "EASY": "핵심 개념과 용어를 확인하는 쉬운 문제 중심으로 출제합니다.",
        "NORMAL": "업무 적용과 기본 이해를 함께 확인하는 수준으로 출제합니다.",
        "HARD": "업무 상황, 예외 상황, 세부 개념을 함께 묻는 수준으로 출제합니다.",
    }
    selected_instruction = difficulty_instructions.get(difficulty, difficulty_instructions["EASY"])

    if req.rag_summary or req.rag_context:
        rag_section = f"""
[RAG 참고 문서 요약]
{req.rag_summary or "없음"}

[RAG 참고 문서 청크]
{req.rag_context or "없음"}
"""

    source_priority_instruction = (
        "RAG 참고 문서 요약과 청크 본문이 있으면 그 내용을 최우선 출제 근거로 사용합니다.\n"
        "카테고리, 태그, 경로, 콘텐츠 유형, 난이도 같은 관리용 메타데이터는 출제 근거로 사용하지 않습니다.\n"
        "문서에 있는 핵심 개념, 절차, 원칙, 용어, 주의사항만으로 문제를 구성합니다."
        if rag_section
        else
        "RAG 문서가 없을 때만 제목과 카테고리를 아주 제한적으로 보조 정보로 사용합니다.\n"
        "태그, 경로, 콘텐츠 유형, 난이도는 출제 근거로 사용하지 않습니다."
    )

    subjective_instruction = (
        "question_count가 2 이상이면 최소 1문항은 SHORT_ANSWER로 생성합니다.\n"
        "SHORT_ANSWER 문항에는 sampleAnswer, keywordAnswer, rubric, explanation을 반드시 포함합니다.\n"
        "MULTIPLE_CHOICE 문항에는 보기 4개와 answerNo를 반드시 포함합니다."
    )

    content_scope = title_only_section if rag_section else f"""
[콘텐츠 기본 정보]
- 제목: {req.title}
- 카테고리: {req.category or "공통"}
- 학습 영역: {req.sub_category or "없음"}
"""

    prompt = f"""
당신은 기업 온보딩 교육용 평가 문항 출제 도우미입니다.
아래 콘텐츠 정보와 참고 문서를 바탕으로 실제 학습 내용을 이해했는지 평가할 수 있는 문제 {req.question_count}개를 생성하세요.

{content_scope}
{rag_section}

[출제 근거 우선순위]
{source_priority_instruction}

[난이도 가이드]
{selected_instruction}

[출제 규칙]
1. 문제는 MULTIPLE_CHOICE 또는 SHORT_ANSWER만 사용합니다.
2. question_count가 2 이상이면 최소 1문항은 SHORT_ANSWER로 생성합니다.
3. 제목 반복보다 실제 학습 내용을 이해했는지 확인하는 문제를 우선 생성합니다.
4. RAG 참고 문서가 있으면 해당 문서의 핵심 내용, 용어, 절차, 원칙을 우선 반영합니다.
5. category, sub_category, tags, path만으로 정답을 유추할 수 있는 문제는 만들지 않습니다.
6. "난이도는 무엇인가", "콘텐츠 유형은 무엇인가", "태그는 무엇인가", "경로는 무엇인가", "카테고리는 무엇인가" 같은 메타데이터형 문제는 절대 만들지 않습니다.
7. 객관식 보기에도 VIDEO, PDF, LINK, EASY, MEDIUM, HARD, 태그, 경로 같은 메타데이터 표현을 넣지 않습니다.
8. 문서에 없는 내용은 과도하게 추론하지 않습니다.
9. explanation은 모든 문항에 포함합니다.
10. score는 각 문항 10점 기준으로 작성합니다.
11. 반드시 JSON만 반환합니다.

[유형별 필수 규칙]
{subjective_instruction}

반환 형식:
{{
  "questions": [
    {{
      "questionType": "MULTIPLE_CHOICE",
      "questionText": "문제 내용",
      "option1": "보기 1",
      "option2": "보기 2",
      "option3": "보기 3",
      "option4": "보기 4",
      "answerNo": 1,
      "sampleAnswer": null,
      "keywordAnswer": null,
      "rubric": null,
      "score": 10,
      "explanation": "정답 해설"
    }},
    {{
      "questionType": "SHORT_ANSWER",
      "questionText": "단답형 문제 내용",
      "option1": null,
      "option2": null,
      "option3": null,
      "option4": null,
      "answerNo": null,
      "sampleAnswer": "모범 답안",
      "keywordAnswer": ["키워드1", "키워드2"],
      "rubric": "채점 기준",
      "score": 10,
      "explanation": "출제 의도 및 채점 포인트"
    }}
  ]
}}
"""

    try:
        client = Groq(api_key=os.getenv("GROQ_API_KEY"))
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "당신은 온보딩 퀴즈 출제 도우미입니다. 반드시 JSON 형식으로만 응답하세요.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.4,
            response_format={"type": "json_object"},
        )

        raw = response.choices[0].message.content.strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"):
                raw = raw[4:]
        raw = raw.strip()

        parsed = json.loads(raw)
        questions = [AiQuizDraftItem(**question) for question in parsed.get("questions", [])]
        questions = _sanitize_generated_questions(req, questions)

        if not questions:
            raise ValueError("생성된 문제가 없습니다.")

        return AiQuizGenerationResponse(questions=questions)

    except Exception as e:
        print(f"[Quiz generation fallback] {e}")
        return AiQuizGenerationResponse(
            questions=_build_fallback_quiz_drafts(req)
        )


# 퀴즈 생성 fallback 초안 구성 함수
def _build_fallback_quiz_drafts(req: AiQuizGenerationRequest) -> list[AiQuizDraftItem]:
    category = req.category or "공통"
    title = req.title
    count = max(1, min(req.question_count, 5))
    source_hint = _extract_source_hint(req)

    drafts: list[AiQuizDraftItem] = []

    objective_count = count - 1 if count >= 2 else 1
    for _ in range(objective_count):
        drafts.append(
            AiQuizDraftItem(
                questionType="MULTIPLE_CHOICE",
                questionText=f"{title} 문서 내용과 가장 일치하는 설명은 무엇인가요?",
                option1="문서 제목과 형식만 기억하면 충분하다는 내용이다",
                option2=source_hint["correct_option"],
                option3="문서 내용과 무관하게 감으로 판단해도 된다는 내용이다",
                option4="학습 없이 평가만 통과하면 된다는 내용이다",
                answerNo=2,
                sampleAnswer=None,
                keywordAnswer=None,
                rubric=None,
                score=10,
                explanation=source_hint["objective_explanation"],
            )
        )

    if count >= 2:
        drafts.append(
            AiQuizDraftItem(
                questionType="SHORT_ANSWER",
                questionText=f"{title} 문서에서 강조하는 핵심 개념이나 절차가 실제 업무에 왜 중요한지 간단히 설명하세요.",
                option1=None,
                option2=None,
                option3=None,
                option4=None,
                answerNo=None,
                sampleAnswer=source_hint["sample_answer"],
                keywordAnswer=source_hint["keywords"],
                rubric="문서에서 강조한 핵심 개념 또는 절차와 실제 업무 적용 이유를 함께 설명하면 정답으로 평가",
                score=10,
                explanation=source_hint["subjective_explanation"],
            )
        )

    return drafts


# 생성 문제 메타데이터형 문항 제거 함수
def _sanitize_generated_questions(req: AiQuizGenerationRequest, questions: list[AiQuizDraftItem]) -> list[AiQuizDraftItem]:
    sanitized: list[AiQuizDraftItem] = []

    for question in questions:
        if _is_metadata_question(question):
            continue
        sanitized.append(question)

    if sanitized:
        return sanitized

    return _build_fallback_quiz_drafts(req)


# 메타데이터형 문항 판별 함수
def _is_metadata_question(question: AiQuizDraftItem) -> bool:
    metadata_terms = [
        "난이도",
        "difficulty",
        "콘텐츠 유형",
        "content type",
        "유형은 무엇",
        "태그",
        "tag",
        "경로",
        "path",
        "카테고리",
        "category",
        "sub category",
        "sub_category",
        "video",
        "pdf",
        "link",
        "easy",
        "medium",
        "hard",
    ]

    objective_pattern_terms = [
        "무엇인가요",
        "무엇인가",
        "어떤 유형",
        "어떤 난이도",
        "무슨 태그",
        "무슨 경로",
    ]

    text_pool = [
        question.questionText or "",
        question.option1 or "",
        question.option2 or "",
        question.option3 or "",
        question.option4 or "",
    ]
    merged_text = " ".join(text_pool).lower()

    has_metadata_term = any(term.lower() in merged_text for term in metadata_terms)
    has_objective_pattern = any(term in (question.questionText or "") for term in objective_pattern_terms)
    option_mentions_metadata = sum(
        1 for option in [question.option1, question.option2, question.option3, question.option4]
        if option and any(term.lower() in option.lower() for term in metadata_terms)
    ) >= 2

    return has_metadata_term and (has_objective_pattern or option_mentions_metadata)


# 문서 기반 fallback 문항 단서 추출 함수
def _extract_source_hint(req: AiQuizGenerationRequest) -> dict:
    source_text = " ".join(
        part for part in [req.rag_summary or "", req.rag_context or ""]
        if part
    ).strip()

    if not source_text:
        return {
            "correct_option": f"{req.title} 문서를 통해 핵심 개념과 실제 적용 포인트를 이해하는 것이 중요하다는 내용이다",
            "objective_explanation": f"{req.title} 문서는 실제 업무와 연결되는 핵심 개념 이해를 돕기 위한 자료입니다.",
            "sample_answer": f"{req.title} 문서는 업무에 필요한 핵심 개념과 절차를 이해하고 실제 적용 기준을 정리하는 데 도움이 된다.",
            "keywords": [req.title, "핵심 개념", "절차", "업무 적용"],
            "subjective_explanation": "문서에서 다루는 핵심 개념 또는 절차와 실제 업무 적용 이유를 설명할 수 있어야 합니다.",
        }

    cleaned = source_text.replace("\n", " ").strip()
    first_sentence = cleaned.split(". ")[0].strip()
    first_sentence = first_sentence[:140] if len(first_sentence) > 140 else first_sentence
    first_sentence = first_sentence or f"{req.title} 문서의 핵심 내용"

    keywords = [token for token in [req.title, req.category, "핵심 개념", "절차", "업무 적용"] if token]

    return {
        "correct_option": f"문서에서 강조하는 내용은 {first_sentence}에 가깝다",
        "objective_explanation": f"문서 핵심 내용은 {first_sentence}이며, 이를 바탕으로 실제 업무 적용 포인트를 이해해야 합니다.",
        "sample_answer": f"{req.title} 문서는 {first_sentence}와 관련된 기준이나 절차를 이해하고 실제 업무에 적용하는 데 도움이 된다.",
        "keywords": keywords[:5],
        "subjective_explanation": "문서에서 강조한 핵심 내용과 그 업무상 중요성을 연결해서 설명할 수 있어야 합니다.",
    }
