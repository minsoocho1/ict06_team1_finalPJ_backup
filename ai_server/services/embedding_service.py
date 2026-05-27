from schemas.embedding_schema import EmbeddingResponse
from utils.nlp_helper import EMBEDDING_MODEL_NAME, get_semantic_embedding_model


def make_semantic_embedding(text: str) -> list[float]:
    normalized = (text or "").strip()
    if not normalized:
        return []

    model = get_semantic_embedding_model()
    embedding = model.encode(
        normalized,
        convert_to_numpy=True,
        normalize_embeddings=True,
    )

    if embedding is None:
        return []

    embedding_list = embedding.tolist() if hasattr(embedding, "tolist") else list(embedding)
    if not embedding_list:
        return []

    return [float(value) for value in embedding_list]


def build_embedding_response(text: str) -> EmbeddingResponse:
    normalized = (text or "").strip()
    if not normalized:
        raise ValueError("text is required")

    embedding = make_semantic_embedding(normalized)
    if not embedding:
        raise ValueError("embedding generation failed")

    return EmbeddingResponse(
        embedding=embedding,
        modelName=EMBEDDING_MODEL_NAME,
        dimension=len(embedding),
    )
