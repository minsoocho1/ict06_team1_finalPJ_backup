from functools import lru_cache

from sentence_transformers import SentenceTransformer, util

SEMANTIC_MODEL_NAME = "jhgan/ko-sroberta-multitask"
EMBEDDING_MODEL_NAME = SEMANTIC_MODEL_NAME


@lru_cache(maxsize=1)
def get_semantic_embedding_model() -> SentenceTransformer:
    model = SentenceTransformer(SEMANTIC_MODEL_NAME)
    print(f"[NLP] SBERT model loaded ({SEMANTIC_MODEL_NAME})")
    return model


def get_semantic_similarity(answer1: str, answer2: str) -> float:
    """
    SBERT 모델을 사용하여 두 문장 간의 의미적 유사도를 계산합니다.
    - 반환값: 0.0 ~ 1.0 사이 값
    """

    if not answer1 or not answer2:
        return 0.0

    model = get_semantic_embedding_model()
    embeddings = model.encode([answer1, answer2], convert_to_tensor=True)
    cosine_score = util.pytorch_cos_sim(embeddings[0], embeddings[1])
    return round(float(cosine_score.item()), 2)
