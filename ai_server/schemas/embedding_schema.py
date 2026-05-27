from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    text: str


class EmbeddingResponse(BaseModel):
    embedding: list[float] = Field(default_factory=list)
    modelName: str = "jhgan/ko-sroberta-multitask"
    dimension: int = 0
