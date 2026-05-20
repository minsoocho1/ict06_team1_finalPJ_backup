from fastapi import APIRouter, HTTPException

from schemas.document_schema import (
    DocumentProcessRequest,
    DocumentProcessResponse,
    DocumentQuestionRequest,
    DocumentQuestionResponse,
)
from services.document_processing_service import process_document, answer_document_question

router = APIRouter()


@router.post("/process", response_model=DocumentProcessResponse)
def process_document_endpoint(req: DocumentProcessRequest):
    try:
        return process_document(req)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/answer", response_model=DocumentQuestionResponse)
def answer_document_question_endpoint(req: DocumentQuestionRequest):
    try:
        return answer_document_question(req)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
