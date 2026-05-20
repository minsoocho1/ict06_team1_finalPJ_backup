#
#  @FileName : document_schema.py
#  @Description : 문서 처리 API 요청/응답 스키마 모듈
#  @Author : 김다솜
#  @Date : 2026. 05. 12
#  @Modification_History
#  @
#  @ 수정일자        수정자          수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.12    김다솜        문서 처리 요청/응답 스키마 정의 및 상단 주석 보강
#  @ 2026.05.18    김다솜        문서 처리 응답에 요청 문서 식별자 docId 추가
#

from pydantic import BaseModel, Field


class DocumentProcessRequest(BaseModel):
    docId: int | None = None
    title: str
    filePath: str


class DocumentChunkResponse(BaseModel):
    chunkNo: int
    content: str
    tokenCount: int
    sectionTitle: str | None = None
    embeddingData: str
    modelName: str = "hash-embedding-v1"
    dimension: int = 256


class DocumentProcessResponse(BaseModel):
    docId: int | None = None
    status: str = "SUCCESS"
    sourceType: str
    extractedTextPreview: str
    chunkCount: int
    vectorCount: int
    embeddingModel: str = "hash-embedding-v1"
    chunks: list[DocumentChunkResponse] = Field(default_factory=list)


class DocumentQuestionRequest(BaseModel):
    title: str
    question: str
    summaryPreview: str | None = None
    chunks: list[str] = Field(default_factory=list)


class DocumentQuestionResponse(BaseModel):
    answer: str
    usedChunkCount: int
