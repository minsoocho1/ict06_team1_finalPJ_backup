/**
 * @FileName : ContentLearningAssistService.java
 * @Description : 학습 콘텐츠 AI 요약, 재설명, 직접 질문 처리 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜        최초 생성 및 콘텐츠 연계 AI 학습 도우미 기능 추가
 * @ 2026.05.18    김다솜        명시적 문서-콘텐츠 연결 우선 조회, 외부 참고 링크 보강 및 깨진 문자열 복구
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AiContentExplainResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentQuestionResponseDto;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ContentType;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentLearningAssistService {

    private final OnContentRepository onContentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentQuestionAnswerService documentQuestionAnswerService;

    public AiContentExplainResponseDto explainContent(Integer contentId, String mode) {
        OnContentEntity content = getContent(contentId);
        validateExplainableContent(content);

        DocumentEntity document = findLinkedDocument(content);
        String normalizedMode = normalizeMode(mode);
        String prompt = buildPrompt(content, normalizedMode);

        AiDocumentQuestionResponseDto answer = documentQuestionAnswerService.answerQuestion(document.getDocId(), prompt);
        return buildResponse(content, normalizedMode, normalizedMode, document, answer);
    }

    public AiContentExplainResponseDto askQuestion(Integer contentId, String question) {
        OnContentEntity content = getContent(contentId);
        validateExplainableContent(content);

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("질문 내용을 입력해 주세요.");
        }

        DocumentEntity document = findLinkedDocument(content);
        String prompt = buildQuestionPrompt(content, question.trim());
        AiDocumentQuestionResponseDto answer = documentQuestionAnswerService.answerQuestion(document.getDocId(), prompt);
        return buildResponse(content, "question", question.trim(), document, answer);
    }

    private AiContentExplainResponseDto buildResponse(
            OnContentEntity content,
            String mode,
            String requestHint,
            DocumentEntity document,
            AiDocumentQuestionResponseDto answer
    ) {
        String enrichedAnswer = appendReferenceLinksIfNeeded(content, requestHint, answer.getAnswer());

        return AiContentExplainResponseDto.builder()
                .mode(mode)
                .sourceTitle(document.getTitle())
                .answer(enrichedAnswer)
                .usedChunkCount(answer.getUsedChunkCount())
                .usedChunks(answer.getUsedChunks())
                .build();
    }

    private OnContentEntity getContent(Integer contentId) {
        return onContentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("학습 콘텐츠를 찾을 수 없습니다."));
    }

    private void validateExplainableContent(OnContentEntity content) {
        ContentType type = content.getType();
        if (type != ContentType.PDF && type != ContentType.LINK) {
            throw new IllegalArgumentException("문서형 콘텐츠에서만 AI 학습 도우미를 사용할 수 있습니다.");
        }
    }

    private DocumentEntity findLinkedDocument(OnContentEntity content) {
        DocumentEntity directDocument = documentRepository.findFirstByRelatedContents_ContentIdOrderByCreatedAtDesc(content.getContentId())
                .or(() -> documentRepository.findFirstByRelatedContent_ContentIdOrderByCreatedAtDesc(content.getContentId()))
                .orElse(null);
        if (directDocument != null) {
            return directDocument;
        }

        if (content.getPath() != null && !content.getPath().isBlank()) {
            return documentRepository.findFirstByFilePathOrderByCreatedAtDesc(content.getPath())
                    .orElseGet(() -> documentRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(content.getTitle())
                            .orElseThrow(() -> new IllegalArgumentException("연결된 문서를 찾을 수 없어 AI 학습 도우미를 사용할 수 없습니다.")));
        }

        return documentRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(content.getTitle())
                .orElseThrow(() -> new IllegalArgumentException("연결된 문서를 찾을 수 없어 AI 학습 도우미를 사용할 수 없습니다."));
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "summary";
        }
        return mode.trim().toLowerCase();
    }

    private String buildPrompt(OnContentEntity content, String mode) {
        if ("simple".equals(mode)) {
            return """
                    This is an onboarding study document.
                    Document title: '%s'

                    Task:
                    1. Pick only difficult terms, technical concepts, or process concepts that a beginner may struggle with.
                    2. Explain each item in practical, easy Korean.
                    3. Create 3 to 5 bullet points.
                    4. Do not invent anything that is not supported by the document.
                    5. Answer in Korean.
                    """.formatted(content.getTitle());
        }

        return """
                This is an onboarding study document.
                Document title: '%s'

                Task:
                1. Summarize the document into 4 to 6 meaningful bullet points.
                2. Cover purpose, key concepts, important procedures, and cautions when available.
                3. Prefer practical points that help a new employee understand what to do.
                4. Do not invent anything that is not supported by the document.
                5. Answer in Korean.
                """.formatted(content.getTitle());
    }

    private String buildQuestionPrompt(OnContentEntity content, String question) {
        return """
                This is a user question about an onboarding study document.
                Document title: '%s'
                User question: %s

                Answer rules:
                1. Use document evidence first.
                2. If the question asks about procedure, order, caution, important point, or practical flow, answer with 2 to 4 bullet points.
                3. If document evidence is thin, say so briefly and provide the best supported answer.
                4. Avoid generic theory-first answers.
                5. Answer in Korean.
                """.formatted(content.getTitle(), question);
    }

    private String appendReferenceLinksIfNeeded(OnContentEntity content, String requestHint, String answer) {
        if (answer == null || answer.isBlank() || !isLowConfidenceAnswer(answer)) {
            return answer;
        }

        String referenceLinks = buildReferenceLinks(content, requestHint);
        if (referenceLinks.isBlank()) {
            return answer;
        }

        return answer + "\n\n관련 참고 URL\n" + referenceLinks;
    }

    private boolean isLowConfidenceAnswer(String answer) {
        String normalized = answer.replaceAll("\\s+", "");
        return normalized.contains("문서에서확인할수없")
                || normalized.contains("자료가부족")
                || normalized.contains("명시적으로언급되지않")
                || normalized.contains("정확한답변이어렵")
                || normalized.contains("근거가충분하지않");
    }

    private String buildReferenceLinks(OnContentEntity content, String requestHint) {
        String source = ((content.getTitle() == null ? "" : content.getTitle()) + " "
                + (content.getCategory() == null ? "" : content.getCategory()) + " "
                + (content.getSubCategory() == null ? "" : content.getSubCategory()) + " "
                + (requestHint == null ? "" : requestHint)).toLowerCase();

        if (source.contains("spring")) {
            return """
                    - Spring Framework 예외 처리 문서: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
                    - Spring Boot 오류 처리 문서: https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.error-handling
                    """.trim();
        }
        if (source.contains("react")) {
            return """
                    - React 상태 관리: https://react.dev/learn/managing-state
                    - React 컴포넌트 간 상태 공유: https://react.dev/learn/sharing-state-between-components
                    """.trim();
        }
        if (source.contains("figma") || source.contains("design")) {
            return """
                    - Figma Help Center: https://help.figma.com/
                    - W3C Design Systems: https://design-system.w3.org/
                    """.trim();
        }
        if (source.contains("accessibility") || source.contains("접근성") || source.contains("a11y")) {
            return """
                    - MDN 접근성 가이드: https://developer.mozilla.org/ko/docs/Learn/Accessibility
                    - WAI 접근성 소개: https://www.w3.org/WAI/fundamentals/accessibility-intro/
                    """.trim();
        }
        if (source.contains("aws")) {
            return """
                    - AWS 개요 문서: https://docs.aws.amazon.com/whitepapers/latest/aws-overview/introduction.html
                    - AWS Documentation: https://docs.aws.amazon.com/
                    """.trim();
        }
        if (source.contains("security") || source.contains("보안")) {
            return """
                    - KISA 보호나라: https://www.boho.or.kr/
                    - OWASP Cheat Sheet Series: https://cheatsheetseries.owasp.org/
                    """.trim();
        }

        return "";
    }
}
