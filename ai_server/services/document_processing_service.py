#
#  @FileName : document_processing_service.py
#  @Description : 문서 본문 추출/청크 분할/벡터 생성 서비스 모듈
#  @Author : 김다솜
#  @Date : 2026. 05. 12
#  @Modification_History
#  @
#  @ 수정일자        수정자          수정내용
#  @ ----------    ---------    -------------------------------
#  @ 2026.05.12    김다솜        문서 자동 처리 파이프라인 구현, 원격 문서 요청 timeout/User-Agent 보강 및 상단 주석 보강
#  @ 2026.05.18    김다솜        문서 처리 응답에 요청 docId를 포함하도록 보완
#  @ 2026.05.19    김다솜        PDF 구조 태그와 객체 문법 필터링 및 청크 부족 시 제목 기반 외부 참고 요약 보강

import hashlib
import html
import json
import math
import os
import re
from io import BytesIO
from urllib.parse import parse_qs, quote_plus, urlparse

import pdfplumber
import requests
from groq import Groq
from pypdf import PdfReader

from services.ollama_client import summarize_document
from schemas.document_schema import (
    DocumentChunkResponse,
    DocumentProcessRequest,
    DocumentProcessResponse,
    DocumentQuestionRequest,
    DocumentQuestionResponse,
)

MAX_CHUNK_CHARS = 900
EMBED_DIMENSION = 256
REMOTE_CONNECT_TIMEOUT = 10
REMOTE_READ_TIMEOUT = 90
WEB_SEARCH_TIMEOUT = 8
PDF_STRUCTURE_MARKERS = {
    "PDF",
    "StructElem",
    "StructTreeRoot",
    "MarkedContent",
    "MCID",
    "RowSpan",
    "ColSpan",
    "ListNumbering",
    "Table",
    "TR",
    "TH",
    "TD",
    "Pg",
    "Page",
    "Pages",
    "MediaBox",
    "TrimBox",
    "BleedBox",
    "CropBox",
    "Resources",
    "Font",
    "Image",
    "Text",
    "XObject",
    "ProcSet",
    "Contents",
    "Parent",
    "Kids",
    "Length",
    "Filter",
    "BBox",
    "K",
    "P",
}
PDF_OBJECT_PATTERN = re.compile(
    r"(%PDF-\d(?:\.\d)?|\b\d+\s+\d+\s+obj\b|\bendobj\b|\bstream\b|\bendstream\b|"
    r"\bxref\b|\btrailer\b|\bstartxref\b|"
    r"/Type\s*/(?:Page|Pages|Catalog|Font|XObject)\b|"
    r"/(?:MediaBox|TrimBox|BleedBox|CropBox|Resources|Font|Image|Text|ProcSet|Contents|Parent|Kids|Length|Filter)\b)"
)
PDF_DICTIONARY_TOKEN_PATTERN = re.compile(r"/[A-Za-z][A-Za-z0-9]*(?:\s*/[A-Za-z][A-Za-z0-9]*)?")
REMOTE_REQUEST_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0 Safari/537.36"
    ),
    "Accept": "text/html,application/pdf,text/plain,application/json,*/*",
}


def process_document(req: DocumentProcessRequest) -> DocumentProcessResponse:
    text, source_type = extract_text(req.filePath)
    cleaned_text = normalize_text(text)

    if not cleaned_text:
        raise ValueError("문서에서 추출한 본문이 비어 있습니다.")

    if source_type.endswith("pdf") and is_low_quality_extraction(cleaned_text):
        raise ValueError("PDF 본문 추출 품질이 너무 낮습니다. 다른 PDF를 사용하거나 OCR 처리가 필요합니다.")

    chunks = filter_content_chunks(split_text(cleaned_text))
    if not chunks:
        preview_text = build_external_fallback_summary(req.title, build_reference_links(req.title, "요약"))
        if not preview_text:
            raise ValueError("본문으로 사용할 수 있는 문서 청크를 생성할 수 없습니다. PDF 구조 태그만 추출된 문서일 수 있어 OCR 또는 원본 파일 확인이 필요합니다.")
    else:
        preview_text = build_preview_text(req.title, cleaned_text)

    response_chunks: list[DocumentChunkResponse] = []
    for index, chunk_text in enumerate(chunks, start=1):
        token_count = estimate_token_count(chunk_text)
        embedding = make_hash_embedding(chunk_text, EMBED_DIMENSION)

        response_chunks.append(
            DocumentChunkResponse(
                chunkNo=index,
                content=chunk_text,
                tokenCount=token_count,
                sectionTitle=build_section_title(req.title, index, chunk_text),
                embeddingData=json.dumps(embedding),
                modelName="hash-embedding-v1",
                dimension=EMBED_DIMENSION,
            )
        )

    return DocumentProcessResponse(
        docId=req.docId,
        sourceType=source_type,
        extractedTextPreview=preview_text[:300],
        chunkCount=len(response_chunks),
        vectorCount=len(response_chunks),
        chunks=response_chunks,
    )


def answer_document_question(req: DocumentQuestionRequest) -> DocumentQuestionResponse:
    question = (req.question or "").strip()
    if not question:
        raise ValueError("질문 내용을 입력해 주세요.")

    context_chunks = filter_content_chunks([
        normalize_text(chunk).strip()
        for chunk in req.chunks
        if chunk and chunk.strip()
    ])
    summary_text = normalize_text((req.summaryPreview or "").strip()) if (req.summaryPreview or "").strip() else ""

    if not context_chunks and not summary_text:
        raise ValueError("문서 요약 또는 청크가 없어 답변을 생성할 수 없습니다.")

    context_text = "\n\n".join(
        f"[참고 청크 {index}]\n{chunk[:1200]}"
        for index, chunk in enumerate(context_chunks[:5], start=1)
    )
    reference_links = build_reference_links(req.title, question)

    if has_thin_answer_evidence(summary_text, context_chunks):
        external_answer = build_reference_grounded_answer(req.title, question, reference_links)
        if external_answer:
            return DocumentQuestionResponse(
                answer=clean_answer_text(external_answer),
                usedChunkCount=0,
            )

    answer = generate_document_answer(req.title, question, summary_text, context_text, reference_links)
    return DocumentQuestionResponse(
        answer=clean_answer_text(answer),
        usedChunkCount=min(len(context_chunks), 5),
    )

def generate_document_answer(
    title: str,
    question: str,
    summary_text: str,
    context_text: str,
    reference_links: list[tuple[str, str]],
) -> str:
    reference_block = format_reference_links(reference_links)
    prompt = f"""
You are an onboarding document study assistant.
Use the document summary and reference chunks below as the primary evidence.
When the document evidence is thin, you may add only the official external reference URLs provided below.
Do not invent missing details. Focus on procedures, principles, cautions, and practical checkpoints.
Always answer in Korean.

Answer rules:
1. Answer the user's actual question first. Avoid broad textbook explanations unless they directly help answer the question.
2. If the question asks about procedures, order, important points, or cautions, answer with 2-4 bullet points.
3. If the question asks about a concept, give one short core sentence first, then add brief supporting details.
4. Even if the document is overview-heavy, reorganize the available evidence into practical points instead of repeating the source wording.
5. Only if evidence is truly thin, briefly say that the document provides limited procedural detail, then add official external references if available.
6. Add one final line starting with '실무 포인트:' when useful.
7. If you use external references, add a final section titled '추가 참고 URL' with bullet links.

[Document Title]
{title}

[Document Summary]
{summary_text or "None"}

[Reference Chunks]
{context_text or "None"}

[Official External References]
{reference_block or "None"}

[Question]
{question}
"""

    try:
        client = Groq(api_key=os.getenv("GROQ_API_KEY"))
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "You answer onboarding document questions. Use document evidence only and respond in Korean with a structured, practical answer.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.2,
        )

        answer = (response.choices[0].message.content or "").strip()
        if answer:
            refined = rewrite_document_answer(
                title=title,
                question=question,
                draft_answer=answer,
                reference_links=reference_links,
            )
            return refined or answer
    except Exception:
        pass

    return build_document_answer_fallback(title, question, summary_text, context_text, reference_links)

def rewrite_document_answer(
    title: str,
    question: str,
    draft_answer: str,
    reference_links: list[tuple[str, str]],
) -> str:
    reference_block = format_reference_links(reference_links)
    prompt = f"""
You are improving an onboarding study assistant answer.
Rewrite the draft into a more practical and less generic Korean answer.

Rewrite rules:
1. Preserve only points that are actually useful for answering the user's question.
2. Reduce broad textbook wording and repeated source-like wording.
3. Prefer concise practical bullets for procedures, cautions, and key points.
4. Keep unsupported claims out.
5. If the draft still lacks enough evidence, you may keep that limitation brief and add the official references below.
6. If external references are used, append them under '추가 참고 URL'.

[Document Title]
{title}

[Question]
{question}

[Draft Answer]
{draft_answer}

[Official External References]
{reference_block or "None"}
"""

    try:
        client = Groq(api_key=os.getenv("GROQ_API_KEY"))
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "Rewrite answers into concise, practical Korean. Avoid generic filler and unsupported claims.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.15,
        )
        return (response.choices[0].message.content or "").strip()
    except Exception:
        if looks_too_generic_answer(draft_answer) and reference_links:
            return append_reference_links(draft_answer, reference_links)
        return draft_answer


def build_document_answer_fallback(
    title: str,
    question: str,
    summary_text: str,
    context_text: str,
    reference_links: list[tuple[str, str]],
) -> str:
    summary = summary_text.strip() if summary_text else ""
    sentences = split_summary_sentences(context_text)
    relevant_sentences = select_relevant_sentences(question, sentences)

    if not summary and not relevant_sentences:
        base = "현재 문서에서 질문과 직접 연결되는 근거를 충분히 찾지 못했습니다. 문서를 다시 재처리하거나 질문을 조금 더 구체적으로 입력해 주세요."
        return append_reference_links(base, reference_links)

    intro = f"{title} 문서를 기준으로 보면 다음 내용을 우선 확인하는 것이 좋습니다."
    points = relevant_sentences[:4]
    if not points and summary:
        points = split_summary_sentences(summary)[:3]

    bullet_lines = [f"- {trim_summary_clause(point)}" for point in points if point.strip()]
    if summary and not bullet_lines:
        bullet_lines.append(f"- {trim_summary_clause(summary)}")

    closing = None
    lowered_question = question.lower()
    if any(keyword in lowered_question for keyword in ["절차", "순서", "단계", "흐름", "과정", "주의", "핵심", "포인트"]):
        closing = "실무 포인트: 예외 처리 기준, 로그 기록 방식, 사용자 응답 메시지 일관성을 함께 확인하는 것이 좋습니다."

    result_parts = [intro]
    if bullet_lines:
        result_parts.append("\n".join(bullet_lines))
    if closing:
        result_parts.append(closing)
    return append_reference_links("\n\n".join(result_parts), reference_links)


def build_reference_links(title: str, question: str) -> list[tuple[str, str]]:
    source = f"{title} {question}".lower()

    if any(keyword in source for keyword in ["표준프레임워크", "전자정부", "egov", "egovframe"]):
        return [
            ("eGovFrame 표준프레임워크 포털", "https://www.egovframe.go.kr/home/main.do"),
            ("표준프레임워크 실행환경 가이드", "https://www.egovframe.go.kr/home/sub.do?menuNo=25"),
            ("표준프레임워크 개발환경 가이드", "https://www.egovframe.go.kr/home/sub.do?menuNo=26"),
            ("표준프레임워크 가이드 시작하기", "https://www.egovframe.go.kr/home/sub.do?menuNo=24"),
        ]

    if "spring" in source:
        return [
            ("Spring Framework 예외 처리", "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"),
            ("Spring Boot 오류 처리", "https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.error-handling"),
        ]
    if "react" in source:
        return [
            ("React 상태 관리", "https://react.dev/learn/managing-state"),
            ("React 상태 공유", "https://react.dev/learn/sharing-state-between-components"),
        ]
    if "aws" in source:
        return [
            ("AWS 개요", "https://docs.aws.amazon.com/whitepapers/latest/aws-overview/introduction.html"),
            ("AWS 문서 메인", "https://docs.aws.amazon.com/"),
        ]
    if "접근성" in source or "a11y" in source or "accessibility" in source:
        return [
            ("MDN 접근성 가이드", "https://developer.mozilla.org/ko/docs/Learn/Accessibility"),
            ("W3C 접근성 소개", "https://www.w3.org/WAI/fundamentals/accessibility-intro/"),
        ]
    if "보안" in source or "security" in source:
        return [
            ("KISA 보호나라", "https://www.boho.or.kr/"),
            ("OWASP Cheat Sheet", "https://cheatsheetseries.owasp.org/"),
        ]
    if "figma" in source or "design" in source or "디자인" in source:
        return [
            ("Figma Help Center", "https://help.figma.com/"),
            ("W3C Design Systems", "https://design-system.w3.org/"),
        ]
    if title and is_search_fallback_enabled():
        return [
            (
                "제목 기반 웹 검색",
                f"https://duckduckgo.com/?q={quote_plus(title)}",
            )
        ]
    return []


def format_reference_links(reference_links: list[tuple[str, str]]) -> str:
    if not reference_links:
        return ""
    return "\n".join(f"- {label}: {url}" for label, url in reference_links)


def append_reference_links(answer: str, reference_links: list[tuple[str, str]]) -> str:
    if not reference_links:
        return answer
    if "추가 참고 URL" in answer:
        return answer
    return answer + "\n\n추가 참고 URL\n" + format_reference_links(reference_links)


def looks_too_generic_answer(answer: str) -> bool:
    normalized = re.sub(r"\s+", "", answer)
    generic_patterns = [
        "문서는.*다룹니다",
        "중요합니다",
        "도움을줍니다",
        "유지하는것입니다",
        "개선하는데중요합니다",
    ]
    return any(re.search(pattern, normalized) for pattern in generic_patterns)


def clean_answer_text(answer: str) -> str:
    cleaned = answer or ""
    cleaned = cleaned.replace("\uf9ce", "")
    cleaned = re.sub(r"[\uf900-\ufaff]+", "", cleaned)
    cleaned = re.sub(r"[ \t]{2,}", " ", cleaned)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    cleaned = re.sub(r"\s+([,.!?])", r"\1", cleaned)
    cleaned = re.sub(r"([가-힣A-Za-z0-9])\s*([·•])\s*", r"\1 \2 ", cleaned)
    return cleaned.strip()

def select_relevant_sentences(question: str, sentences: list[str]) -> list[str]:
    question_terms = extract_summary_tokens(question)
    if not question_terms:
        return sentences[:2]

    scored = []
    for sentence in sentences:
        normalized = sentence.lower()
        score = sum(1 for term in question_terms if term in normalized)
        if score > 0:
            scored.append((score, sentence))

    scored.sort(key=lambda item: item[0], reverse=True)
    return [sentence for _, sentence in scored[:3]]


def extract_text(file_path: str) -> tuple[str, str]:
    if file_path.startswith(("http://", "https://")):
        return extract_remote_text(file_path)

    if file_path.lower().endswith(".pdf"):
        return extract_local_pdf_text(file_path), "local-pdf"

    with open(file_path, "r", encoding="utf-8") as file:
        return file.read(), "local-text"


def extract_remote_text(file_path: str) -> tuple[str, str]:
    request_url = normalize_drive_url(file_path)
    try:
        response = requests.get(
            request_url,
            headers=REMOTE_REQUEST_HEADERS,
            timeout=(REMOTE_CONNECT_TIMEOUT, REMOTE_READ_TIMEOUT),
        )
    except requests.Timeout as exc:
        raise TimeoutError(
            f"원격 문서 응답 시간이 {REMOTE_READ_TIMEOUT}초를 초과했습니다. "
            "해당 URL을 브라우저에서 열어 접근 가능한지 확인하거나 파일을 로컬/드라이브 문서로 등록해 주세요."
        ) from exc

    response.raise_for_status()

    content_type = response.headers.get("content-type", "").lower()
    if "text/plain" in content_type or "application/json" in content_type:
        return decode_response_text(response), "remote-text"

    if "text/html" in content_type:
        return html_to_text(decode_response_text(response)), "remote-html"

    if "application/pdf" in content_type or request_url.lower().endswith(".pdf"):
        return extract_pdf_text(response.content), "remote-pdf"

    decoded = decode_response_text(response)
    return decoded, "remote-binary-text"


def extract_local_pdf_text(file_path: str) -> str:
    with open(file_path, "rb") as file:
        return extract_pdf_text(file.read())


def extract_pdf_text(pdf_bytes: bytes) -> str:
    text = extract_pdf_text_with_pypdf(pdf_bytes)
    if is_low_quality_extraction(text):
        fallback_text = extract_pdf_text_with_pdfplumber(pdf_bytes)
        if not is_low_quality_extraction(fallback_text):
            text = fallback_text

    if not text:
        raise ValueError("PDF에서 추출한 본문이 비어 있습니다.")

    return text


def extract_pdf_text_with_pypdf(pdf_bytes: bytes) -> str:
    reader = PdfReader(BytesIO(pdf_bytes))
    pages: list[str] = []

    for page in reader.pages:
        page_text = page.extract_text() or ""
        page_text = page_text.strip()
        if page_text:
            pages.append(page_text)

    return "\n\n".join(pages).strip()


def extract_pdf_text_with_pdfplumber(pdf_bytes: bytes) -> str:
    pages: list[str] = []

    with pdfplumber.open(BytesIO(pdf_bytes)) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text() or ""
            page_text = page_text.strip()
            if page_text:
                pages.append(page_text)

    return "\n\n".join(pages).strip()


def normalize_drive_url(file_path: str) -> str:
    parsed = urlparse(file_path)
    if "drive.google.com" not in parsed.netloc:
        return file_path

    match = re.search(r"/file/d/([^/]+)/", file_path)
    if match:
        return f"https://drive.google.com/uc?export=download&id={match.group(1)}"

    query = parse_qs(parsed.query)
    if "id" in query and query["id"]:
        return f"https://drive.google.com/uc?export=download&id={query['id'][0]}"

    return file_path


def html_to_text(raw_html: str) -> str:
    without_script = re.sub(r"<script.*?>.*?</script>", " ", raw_html, flags=re.IGNORECASE | re.DOTALL)
    without_style = re.sub(r"<style.*?>.*?</style>", " ", without_script, flags=re.IGNORECASE | re.DOTALL)
    without_tags = re.sub(r"<[^>]+>", " ", without_style)
    return html.unescape(without_tags)


def decode_response_text(response: requests.Response) -> str:
    encoding = response.encoding or response.apparent_encoding or "utf-8"

    try:
        text = response.content.decode(encoding, errors="ignore")
    except LookupError:
        text = response.content.decode("utf-8", errors="ignore")

    return repair_mojibake_text(text)


def normalize_text(text: str) -> str:
    text = repair_mojibake_text(text)
    text = text.replace("\x00", " ")
    text = text.replace("\r", "\n")
    text = re.sub(r"[\x01-\x08\x0B\x0C\x0E-\x1F\x7F]", " ", text)
    text = remove_pdf_structure_lines(text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


def remove_pdf_structure_lines(text: str) -> str:
    lines = []
    for line in text.splitlines():
        normalized = line.strip()
        if not normalized:
            lines.append(line)
            continue
        if is_pdf_structure_artifact(normalized):
            continue
        lines.append(line)
    return remove_pdf_object_blocks("\n".join(lines))


def filter_content_chunks(chunks: list[str]) -> list[str]:
    return [chunk for chunk in chunks if not is_pdf_structure_artifact(chunk)]


def remove_pdf_object_blocks(text: str) -> str:
    # PDF dictionary/object blocks may be extracted as wrapped text, not line-by-line text.
    text = re.sub(r"<<[^<>]{0,1200}>>", " ", text)
    text = re.sub(r"\b\d+\s+\d+\s+obj\b.*?\bendobj\b", " ", text, flags=re.DOTALL)
    text = re.sub(r"\bstream\b.*?\bendstream\b", " ", text, flags=re.DOTALL)
    return text


def is_pdf_structure_artifact(text: str) -> bool:
    sample = re.sub(r"\s+", " ", text or "").strip()
    if not sample:
        return True

    hangul_count = len(re.findall(r"[가-힣]", sample))
    pdf_dictionary_hits = len(PDF_DICTIONARY_TOKEN_PATTERN.findall(sample))
    if PDF_OBJECT_PATTERN.search(sample):
        if hangul_count < 20 or pdf_dictionary_hits >= 3:
            return True

    marker_hits = sum(
        1
        for marker in PDF_STRUCTURE_MARKERS
        if re.search(rf"\b{re.escape(marker)}\b", sample)
    )
    if marker_hits >= 3:
        return True

    words = re.findall(r"[A-Za-z가-힣]{2,}", sample)
    if not words:
        return True

    marker_words = sum(1 for word in words if word in PDF_STRUCTURE_MARKERS)
    marker_ratio = marker_words / max(1, len(words))
    has_korean_content = bool(re.search(r"[가-힣]{2,}", sample))
    return marker_ratio >= 0.35 and not has_korean_content


def split_text(text: str) -> list[str]:
    paragraphs = [paragraph.strip() for paragraph in text.split("\n\n") if paragraph.strip()]
    chunks: list[str] = []
    current = ""

    for paragraph in paragraphs:
        candidate = f"{current}\n\n{paragraph}".strip() if current else paragraph
        if len(candidate) <= MAX_CHUNK_CHARS:
            current = candidate
            continue

        if current:
            chunks.append(current)
            current = ""

        if len(paragraph) <= MAX_CHUNK_CHARS:
            current = paragraph
            continue

        chunks.extend(split_long_paragraph(paragraph))

    if current:
        chunks.append(current)

    return chunks


def split_long_paragraph(paragraph: str) -> list[str]:
    sentences = re.split(r"(?<=[.!?。！？])\s+", paragraph)
    chunks: list[str] = []
    current = ""

    for sentence in sentences:
        sentence = sentence.strip()
        if not sentence:
            continue

        candidate = f"{current} {sentence}".strip() if current else sentence
        if len(candidate) <= MAX_CHUNK_CHARS:
            current = candidate
            continue

        if current:
            chunks.append(current)

        if len(sentence) <= MAX_CHUNK_CHARS:
            current = sentence
        else:
            chunks.extend(
                sentence[index:index + MAX_CHUNK_CHARS].strip()
                for index in range(0, len(sentence), MAX_CHUNK_CHARS)
                if sentence[index:index + MAX_CHUNK_CHARS].strip()
            )
            current = ""

    if current:
        chunks.append(current)

    return chunks


def estimate_token_count(text: str) -> int:
    return max(1, math.ceil(len(text.split()) * 1.3))


def extract_section_title(text: str) -> str | None:
    first_line = text.splitlines()[0].strip()
    return first_line[:120] if first_line else None


def build_preview_text(title: str, text: str) -> str:
    reference_links = build_reference_links(title, "요약")

    if is_insufficient_document_evidence(text):
        external_summary = build_external_fallback_summary(title, reference_links)
        if external_summary:
            return external_summary

    try:
        summary = summarize_document(title, text)
        if summary:
            normalized_summary = normalize_summary_text(summary)
            if normalized_summary and not looks_like_leading_copy(normalized_summary, text) and not looks_too_generic_answer(normalized_summary):
                return normalized_summary
    except Exception:
        pass

    extracted_summary = build_extractive_summary(title, text)
    if extracted_summary:
        return extracted_summary

    external_summary = build_external_fallback_summary(title, reference_links)
    if external_summary:
        return external_summary

    return text[:300]


def build_section_title(document_title: str, chunk_no: int, text: str) -> str | None:
    extracted_title = extract_section_title(text)
    if extracted_title and is_readable_title(extracted_title):
        return extracted_title[:120]

    return f"{document_title} - Chunk {chunk_no}"


def build_external_fallback_summary(title: str, reference_links: list[tuple[str, str]]) -> str | None:
    source = (title or "").lower()
    if any(keyword in source for keyword in ["표준프레임워크", "전자정부", "egov", "egovframe"]):
        summary = (
            f"{title} 문서는 전자정부 표준프레임워크(eGovFrame)의 적용·개발·실행환경 기준을 확인하기 위한 가이드로 볼 수 있습니다. "
            "본문 추출 품질이 낮아 세부 절차는 공식 포털의 실행환경/개발환경 가이드와 적용 기준 문서를 함께 확인하는 것이 좋습니다."
        )
        return append_reference_links(normalize_summary_text(summary), reference_links)

    snippets = search_web_snippets(title)
    if snippets:
        summary = f"{title} 문서는 외부 검색 결과 기준으로 {trim_summary_clause(snippets[0])}"
        if len(snippets) > 1:
            summary += f" {trim_summary_clause(snippets[1])}"
        return append_reference_links(normalize_summary_text(summary), reference_links)

    if reference_links:
        summary = (
            f"{title} 문서는 현재 추출된 본문만으로는 세부 요약이 어렵습니다. "
            "아래 참고 URL을 확인해 공식 문서 기준으로 목적, 적용 범위, 절차를 보완해 주세요."
        )
        return append_reference_links(normalize_summary_text(summary), reference_links)

    return None


def build_reference_grounded_answer(title: str, question: str, reference_links: list[tuple[str, str]]) -> str | None:
    source = f"{title} {question}".lower()
    if any(keyword in source for keyword in ["표준프레임워크", "전자정부", "egov", "egovframe"]):
        answer = f"""
{title} 문서는 전자정부 표준프레임워크(eGovFrame)를 프로젝트에 적용할 때 확인해야 하는 실행환경, 개발환경, 적용 범위, 호환성 기준을 안내하는 자료로 볼 수 있습니다.

* 표준프레임워크는 공공 정보화 사업에서 재사용 가능한 표준 개발 기반을 제공하기 위한 프레임워크입니다.
* 적용 시 실행환경 버전, 개발환경 구성, 공통컴포넌트 사용 여부, 호환성 기준을 먼저 확인해야 합니다.
* 프로젝트 구조와 패키지, 라이브러리 버전, Spring 기반 설정 방식이 표준프레임워크 기준과 충돌하지 않는지 점검하는 것이 핵심입니다.
* 현재 등록 문서의 본문 추출 품질이 낮아 세부 절차는 공식 eGovFrame 가이드와 함께 확인하는 방식이 안전합니다.

실무 포인트:
문서 재처리 결과가 부족할 때는 원본 PDF/OCR 상태를 먼저 점검하고, 적용 기준은 공식 포털의 실행환경/개발환경 가이드를 기준으로 보완하세요.
""".strip()
        return append_reference_links(answer, reference_links)

    snippets = search_web_snippets(title)
    if snippets:
        answer = f"{title} 문서는 등록된 본문 근거가 부족하여 제목 기반 웹 검색 결과를 보조로 참고했습니다.\n"
        answer += "\n".join(f"* {trim_summary_clause(snippet)}" for snippet in snippets)
        answer += "\n\n실무 포인트:\n외부 검색 결과는 보조 자료이므로 원본 문서 재처리 또는 공식 출처 확인을 함께 진행하세요."
        return append_reference_links(answer, reference_links)

    return None


def build_extractive_summary(title: str, text: str) -> str | None:
    sentences = split_summary_sentences(text)
    if not sentences:
        return None

    title_tokens = set(extract_summary_tokens(title))
    text_tokens = extract_summary_tokens(text[:4000])
    token_weights: dict[str, int] = {}
    for token in text_tokens:
        token_weights[token] = token_weights.get(token, 0) + 1

    scored_sentences: list[tuple[float, int, str]] = []
    for index, sentence in enumerate(sentences[:20]):
        score = score_summary_sentence(sentence, title_tokens, token_weights)
        if score > 0:
            scored_sentences.append((score, index, sentence))

    if not scored_sentences:
        return None

    selected = sorted(
        sorted(scored_sentences, key=lambda item: item[0], reverse=True)[:2],
        key=lambda item: item[1],
    )
    sentences = [sentence for _, _, sentence in selected]
    if not sentences:
        return None

    if len(sentences) == 1:
        summary = f"{title} 문서는 {trim_summary_clause(sentences[0])}"
        return normalize_summary_text(summary)

    summary = (
        f"{title} 문서는 {trim_summary_clause(sentences[0])} "
        f"{trim_summary_clause(sentences[1])}"
    )
    return normalize_summary_text(summary)


def split_summary_sentences(text: str) -> list[str]:
    normalized = re.sub(r"\s+", " ", text)
    parts = re.split(r"(?<=[.!?。다요])\s+", normalized)
    results: list[str] = []
    for part in parts:
        sentence = part.strip()
        if len(sentence) < 20:
            continue
        if is_navigation_like_sentence(sentence):
            continue
        results.append(sentence[:180])
    return results


def is_insufficient_document_evidence(text: str) -> bool:
    normalized = re.sub(r"\s+", " ", text or "").strip()
    if len(normalized) < 220:
        return True

    sentences = split_summary_sentences(normalized)
    if len(sentences) < 2:
        return True

    tokens = extract_summary_tokens(normalized[:1500])
    return len(set(tokens)) < 12


def has_thin_answer_evidence(summary_text: str, context_chunks: list[str]) -> bool:
    combined = " ".join([summary_text or "", *context_chunks])
    normalized = re.sub(r"\s+", "", combined)
    if not normalized:
        return True

    weak_markers = [
        "내용이제공되지않아",
        "문서의내용이제공되지않아",
        "주요개념이나절차를파악하기어렵습니다",
        "정보가부족",
        "근거를충분히찾지못했습니다",
        "본문추출품질이낮",
        "세부요약이어렵습니다",
    ]
    if any(marker in normalized for marker in weak_markers):
        return True

    return is_insufficient_document_evidence(combined)


def is_search_fallback_enabled() -> bool:
    return os.getenv("ENABLE_WEB_SEARCH_FALLBACK", "true").lower() == "true"


def search_web_snippets(title: str) -> list[str]:
    if not title or not is_search_fallback_enabled():
        return []

    try:
        response = requests.get(
            "https://duckduckgo.com/html/",
            params={"q": title},
            headers=REMOTE_REQUEST_HEADERS,
            timeout=WEB_SEARCH_TIMEOUT,
        )
        response.raise_for_status()
    except Exception:
        return []

    snippets = re.findall(
        r'<a[^>]+class="result__snippet"[^>]*>(.*?)</a>|<div[^>]+class="result__snippet"[^>]*>(.*?)</div>',
        response.text,
        flags=re.IGNORECASE | re.DOTALL,
    )
    results: list[str] = []
    for groups in snippets:
        raw = next((group for group in groups if group), "")
        text = normalize_text(html_to_text(raw))
        if text and not is_pdf_structure_artifact(text):
            results.append(text[:180])
        if len(results) >= 2:
            break
    return results


def extract_summary_tokens(text: str) -> list[str]:
    tokens = re.findall(r"[A-Za-z가-힣0-9]{2,}", text.lower())
    stopwords = {
        "the", "and", "for", "with", "that", "this", "from", "into", "http", "https",
        "docs", "main", "menu", "skip", "content", "home", "guide", "page",
        "문서", "내용", "관련", "대한", "위한", "에서", "으로", "하는", "하기", "있습니다",
    }
    return [token for token in tokens if token not in stopwords]


def score_summary_sentence(sentence: str, title_tokens: set[str], token_weights: dict[str, int]) -> float:
    sentence_tokens = extract_summary_tokens(sentence)
    if not sentence_tokens:
        return 0.0

    unique_tokens = set(sentence_tokens)
    title_bonus = sum(3 for token in unique_tokens if token in title_tokens)
    frequency_score = sum(min(token_weights.get(token, 0), 4) for token in unique_tokens)
    length_penalty = 0.0 if len(sentence) <= 150 else 1.5
    return float(title_bonus + frequency_score - length_penalty)


def normalize_summary_text(text: str) -> str:
    summary = re.sub(r"\s+", " ", text).strip()
    summary = re.sub(r"^(skip to .*? )", "", summary, flags=re.IGNORECASE)
    summary = re.sub(r"(skip to .*?$)", "", summary, flags=re.IGNORECASE)
    return summary[:240].strip()


def trim_summary_clause(sentence: str) -> str:
    trimmed = sentence.strip()
    trimmed = re.sub(r"^(이 문서는|본 문서는|문서는)\s*", "", trimmed)
    trimmed = re.sub(r"\s+", " ", trimmed)
    return trimmed[:120].rstrip(" ,")


def looks_like_leading_copy(summary: str, text: str) -> bool:
    original = re.sub(r"\s+", " ", text).strip()
    if not original:
        return False

    leading = original[:400]
    if summary in leading:
        return True

    summary_tokens = extract_summary_tokens(summary)
    leading_tokens = extract_summary_tokens(leading)
    if not summary_tokens or not leading_tokens:
        return False

    overlap = sum(1 for token in summary_tokens if token in leading_tokens)
    return overlap / max(1, len(summary_tokens)) > 0.85


def is_navigation_like_sentence(sentence: str) -> bool:
    lowered = sentence.lower()
    markers = [
        "skip to main", "skip to search", "main content", "breadcrumb",
        "navigation", "menu", "sign in", "log in", "home /", "search",
    ]
    return any(marker in lowered for marker in markers)


def is_readable_title(text: str) -> bool:
    candidate = text.strip()
    if not candidate:
        return False

    if len(candidate) < 2:
        return False

    allowed = sum(
        1 for ch in candidate
        if ch.isalnum() or ch.isspace() or ch in "-_:/()[]{}.,"
    )
    ratio = allowed / len(candidate)

    if ratio < 0.6:
        return False

    weird_markers = ["�", "\u0000", "硫", "??", "Ã", "ð", "ì", "ë", "ê", "í", "ã", "â"]
    if any(marker in candidate for marker in weird_markers):
        return False

    return True


def repair_mojibake_text(text: str) -> str:
    candidate = text.strip()
    if not candidate:
        return text

    suspicious_chars = sum(1 for ch in candidate if 0x00C0 <= ord(ch) <= 0x00FF)
    ratio = suspicious_chars / max(1, len(candidate))

    if ratio < 0.05:
        return text

    try:
        repaired = candidate.encode("latin1", errors="ignore").decode("utf-8", errors="ignore")
    except Exception:
        return text

    if not repaired.strip():
        return text

    repaired_suspicious = sum(1 for ch in repaired if 0x00C0 <= ord(ch) <= 0x00FF)
    if repaired_suspicious < suspicious_chars:
        return repaired

    return text


def is_low_quality_extraction(text: str) -> bool:
    candidate = (text or "").strip()
    if not candidate:
        return True

    sample = candidate[:1000]
    weird_chars = sum(
        1 for ch in sample
        if not (ch.isalnum() or ch.isspace() or ch in ".,!?;:'\"-_/()[]{}<>@#%&*+=~|")
    )
    weird_ratio = weird_chars / max(1, len(sample))

    gibberish_markers = ["�", "硫", "Ã", "ð", "¿", "½", "¼", " IZ5 ", " d'N^ "]
    if any(marker in sample for marker in gibberish_markers):
        return True

    return weird_ratio > 0.35


def make_hash_embedding(text: str, dimension: int) -> list[float]:
    vector = [0.0] * dimension
    tokens = re.findall(r"[\w가-힣]+", text.lower())

    if not tokens:
        return vector

    for token in tokens:
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % dimension
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        weight = 1.0 + (digest[5] / 255.0)
        vector[index] += sign * weight

    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector

    return [round(value / norm, 6) for value in vector]
