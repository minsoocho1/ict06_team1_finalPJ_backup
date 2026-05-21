/**
 * @FileName : DocumentQuestionAnswerService.java
 * @Description : 문서 청크 기반 질의응답 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜        최초 생성
 * @ 2026.05.15    김다솜        청크 선택 로직 및 질문 의도 기반 점수 보강
 * @ 2026.05.19    김다솜        PDF 구조 태그 청크가 AI 답변 근거로 사용되지 않도록 필터링 보강
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentQuestionRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentQuestionResponseDto;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentQuestionAnswerService {

    private static final int MAX_CONTEXT_CHUNKS = 8;
    private static final Pattern TERM_SPLIT_PATTERN = Pattern.compile("[^0-9A-Za-z가-힣]+");
    private static final List<String> PROCEDURE_HINTS = List.of("절차", "순서", "단계", "흐름", "과정");
    private static final List<String> CAUTION_HINTS = List.of("주의", "주의사항", "유의", "실수", "오류", "예외");
    private static final List<String> CORE_HINTS = List.of("중요", "핵심", "기준", "원칙", "포인트");
    private static final List<String> PDF_STRUCTURE_MARKERS = List.of(
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
            "BBox"
    );

    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    /**
     * @MethodName : answerQuestion
     * @Description : 문서 기반 질문 응답 생성
     *
     * @param docId    문서 식별자
     * @param question 질문 내용
     * @return 문서 질문 응답 DTO
     */
    public AiDocumentQuestionResponseDto answerQuestion(Integer docId, String question) {
        DocumentEntity document = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isBlank()) {
            throw new IllegalArgumentException("질문 내용을 입력해 주세요.");
        }

        List<String> selectedChunks = selectRelevantChunks(document, normalizedQuestion);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiDocumentQuestionRequestDto requestDto = AiDocumentQuestionRequestDto.builder()
                .title(document.getTitle())
                .question(normalizedQuestion)
                .summaryPreview(document.getSummaryPreview())
                .chunks(selectedChunks)
                .build();

        HttpEntity<AiDocumentQuestionRequestDto> entity = new HttpEntity<>(requestDto, headers);
        ResponseEntity<AiDocumentQuestionResponseDto> response = restTemplate.postForEntity(
                aiServerBaseUrl + "/api/ai/documents/answer",
                entity,
                AiDocumentQuestionResponseDto.class
        );

        AiDocumentQuestionResponseDto payload = response.getBody();
        if (payload == null || payload.getAnswer() == null || payload.getAnswer().isBlank()) {
            throw new IllegalStateException("문서 답변 생성 결과가 비어 있습니다.");
        }

        payload.setUsedChunkCount(selectedChunks.size());
        payload.setUsedChunks(selectedChunks);
        return payload;
    }

    /**
     * @MethodName : selectRelevantChunks
     * @Description : 질문 관련도 기준 참고 청크 선별
     *
     * @param document 문서 엔티티
     * @param question 질문 내용
     * @return 참고 청크 문자열 목록
     */
    private List<String> selectRelevantChunks(DocumentEntity document, String question) {
        List<DocChunkEntity> chunks = document.getChunks().stream()
                .filter(chunk -> !isPdfStructureArtifact(chunk))
                .sorted(Comparator.comparing(DocChunkEntity::getChunkNo))
                .toList();

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("본문으로 사용할 수 있는 문서 청크가 없습니다. PDF 구조 태그만 저장된 문서일 수 있으므로 재처리 또는 OCR 처리가 필요합니다.");
        }

        Set<String> questionTerms = extractTerms(question);
        questionTerms.addAll(extractTerms(document.getTitle()));

        List<DocChunkEntity> topChunks = chunks.stream()
                .sorted(Comparator
                        .comparingInt((DocChunkEntity chunk) -> scoreChunk(chunk, question, questionTerms))
                        .reversed()
                        .thenComparing(DocChunkEntity::getChunkNo))
                .limit(3)
                .toList();

        boolean hasUsefulMatch = topChunks.stream()
                .anyMatch(chunk -> scoreChunk(chunk, question, questionTerms) > 0);

        Set<Integer> selectedChunkNos = new LinkedHashSet<>();
        if (hasUsefulMatch) {
            for (DocChunkEntity topChunk : topChunks) {
                int currentNo = topChunk.getChunkNo();
                selectedChunkNos.add(currentNo - 1);
                selectedChunkNos.add(currentNo);
                selectedChunkNos.add(currentNo + 1);
            }
        }

        selectedChunkNos.add(1);
        selectedChunkNos.add(2);

        return chunks.stream()
                .filter(chunk -> selectedChunkNos.contains(chunk.getChunkNo()))
                .limit(MAX_CONTEXT_CHUNKS)
                .map(this::toChunkContext)
                .toList();
    }

    /**
     * @MethodName : scoreChunk
     * @Description : 질문 관련도 기준 청크 점수 계산
     *
     * @param chunk         청크 엔티티
     * @param question      질문 내용
     * @param questionTerms 질문 및 제목 토큰 집합
     * @return 관련도 점수
     */
    private int scoreChunk(DocChunkEntity chunk, String question, Set<String> questionTerms) {
        if (isPdfStructureArtifact(chunk)) {
            return Integer.MIN_VALUE;
        }

        if (questionTerms.isEmpty()) {
            return 0;
        }

        String content = chunk.getContent() == null ? "" : chunk.getContent();
        String sectionTitle = chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle();
        String normalizedContent = normalizeForSearch(content);
        String normalizedSectionTitle = normalizeForSearch(sectionTitle);
        String normalizedQuestion = normalizeForSearch(question);

        int score = 0;
        for (String term : questionTerms) {
            String normalizedTerm = normalizeForSearch(term);
            if (normalizedTerm.isBlank()) {
                continue;
            }

            if (normalizedSectionTitle.contains(normalizedTerm)) {
                score += Math.max(normalizedTerm.length(), 1) * 4;
            }

            if (normalizedContent.contains(normalizedTerm)) {
                score += Math.max(normalizedTerm.length(), 1) * 3;
            }
        }

        if (containsAny(normalizedQuestion, PROCEDURE_HINTS) && containsAny(normalizedContent, PROCEDURE_HINTS)) {
            score += 12;
        }
        if (containsAny(normalizedQuestion, CAUTION_HINTS) && containsAny(normalizedContent, CAUTION_HINTS)) {
            score += 10;
        }
        if (containsAny(normalizedQuestion, CORE_HINTS) && containsAny(normalizedContent, CORE_HINTS)) {
            score += 8;
        }
        if (normalizedContent.matches(".*\\d+\\..*") || normalizedContent.contains("1)") || normalizedContent.contains("첫째")) {
            score += 4;
        }

        return score;
    }

    /**
     * @MethodName : extractTerms
     * @Description : 질문/제목에서 검색용 핵심 토큰 추출
     *
     * @param text 원본 문자열
     * @return 토큰 집합
     */
    private Set<String> extractTerms(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashSet<>();
        }

        return TERM_SPLIT_PATTERN.splitAsStream(text.toLowerCase(Locale.ROOT))
                .map(String::trim)
                .map(this::stripKoreanParticle)
                .filter(term -> term.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @MethodName : stripKoreanParticle
     * @Description : 한국어 조사 제거
     *
     * @param term 토큰 문자열
     * @return 조사 제거 토큰
     */
    private String stripKoreanParticle(String term) {
        return term.replaceFirst("(은|는|이|가|을|를|에서|에게|으로|로|과|와|도|만|보다|부터|까지)$", "");
    }

    /**
     * @MethodName : containsAny
     * @Description : 키워드 포함 여부 확인
     *
     * @param source   검색 대상 문자열
     * @param keywords 키워드 목록
     * @return 포함 여부
     */
    private boolean containsAny(String source, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> source.contains(normalizeForSearch(keyword)));
    }

    /**
     * @MethodName : normalizeForSearch
     * @Description : 검색 비교용 문자열 정규화
     *
     * @param text 원본 문자열
     * @return 공백 제거 소문자 문자열
     */
    private String normalizeForSearch(String text) {
        return text == null
                ? ""
                : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    /**
     * @MethodName : toChunkContext
     * @Description : AI 서버 전달용 청크 문맥 문자열 변환
     *
     * @param chunk 청크 엔티티
     * @return 섹션 제목 포함 문맥 문자열
     */
    private String toChunkContext(DocChunkEntity chunk) {
        StringBuilder context = new StringBuilder();
        context.append("[청크 ").append(chunk.getChunkNo()).append("]");

        String sectionTitle = chunk.getSectionTitle();
        if (sectionTitle != null && !sectionTitle.isBlank()) {
            context.append("\n[섹션] ").append(sectionTitle);
        }

        context.append("\n").append(chunk.getContent());
        return context.toString();
    }

    private boolean isPdfStructureArtifact(DocChunkEntity chunk) {
        if (chunk == null) {
            return true;
        }

        String content = chunk.getContent() == null ? "" : chunk.getContent();
        String sectionTitle = chunk.getSectionTitle() == null ? "" : chunk.getSectionTitle();
        return isPdfStructureArtifact(content + "\n" + sectionTitle);
    }

    private boolean isPdfStructureArtifact(String text) {
        String sample = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (sample.isBlank()) {
            return true;
        }

        long markerHits = PDF_STRUCTURE_MARKERS.stream()
                .filter(sample::contains)
                .count();
        if (markerHits >= 3) {
            return true;
        }

        String[] words = sample.split("[^0-9A-Za-z가-힣]+");
        long meaningfulWords = java.util.Arrays.stream(words)
                .filter(word -> word != null && word.length() >= 2)
                .count();
        if (meaningfulWords == 0) {
            return true;
        }

        long markerWords = java.util.Arrays.stream(words)
                .filter(PDF_STRUCTURE_MARKERS::contains)
                .count();
        boolean hasKoreanContent = sample.matches(".*[가-힣]{2,}.*");
        double markerRatio = markerWords / (double) meaningfulWords;
        return markerRatio >= 0.35 && !hasKoreanContent;
    }
}
