#
# @FileName : nlp_helper.py
# @Description : SBERT 기반 문장 의미 유사도(Semantic Similarity) 측정 엔진
#                - Sentence-Transformers를 활용한 한국어 답변 임베딩 및 비교
#                - 코사인 유사도(Cosine Similarity)를 이용한 주관식 자동 채점 기초 로직
# @Author : 김다솜
# @Date : 2026. 05. 06
# @Modification_History
# @
# @ 수정일         수정자        수정내용
# @ ----------    ---------    -------------------------------
# @ 2026.05.06    김다솜        최초 생성 및 SBERT 모델 로딩 구현
#

from sentence_transformers import SentenceTransformer, util
import torch

# 1. 모델 로드 (한국어 문장 임베딩에 최적화된 SBERT 모델 활용)
# - snunlp 모델: KLUE 데이터셋으로 학습되어 한국어 문맥 이해도가 높음
model = SentenceTransformer('jhgan/ko-sroberta-multitask')
print("✅ [NLP] 한국어 SBERT 모델 로딩 완료 (jhgan/ko-sroberta-multitask)")

# 두 문장의 의미적 유사도 계산 -> 0~1 사이 값으로 반환
def get_semantic_similarity(answer1: str, answer2: str) -> float:
    """
    SBERT 모델을 사용하여 두 문장 간의 의미적 유사도를 계산합니다.
    - 파라미터: answer1(사용자 답변), answer2(모범 답안)
    - 반환값: 0.0 ~ 1.0 사이의 코사인 유사도 실수값
    """
    
    # 예외 처리: 답변이 비어있는 경우 유사도 0 반환
    if not answer1 or not answer2:
        return 0.0
    
    # 2. 문장 Embedding 생성
    # convert_to_tensor=True를 통해 GPU 연산(사용 가능한 경우) 및 빠른 코사인 유사도 계산 지원
    embeddings = model.encode([answer1, answer2], convert_to_tensor=True)
    
    # 3. Cosine similarity 계산
    # 두 벡터 사이의 각도 측정하여 문맥적 유사성 판별
    cosine_score = util.pytorch_cos_sim(embeddings[0], embeddings[1])
    
    # 텐서 형태의 결과에서 실수값만 추출해 소수점 둘째 자리까지 반올림
    return round(float(cosine_score.item()), 2)