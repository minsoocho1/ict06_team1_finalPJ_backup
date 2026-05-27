from fastapi import APIRouter, HTTPException

from schemas.embedding_schema import EmbeddingRequest, EmbeddingResponse
from services.embedding_service import build_embedding_response

router = APIRouter()


@router.post("/embeddings", response_model=EmbeddingResponse)
def create_embedding(req: EmbeddingRequest):
    try:
        return build_embedding_response(req.text)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
