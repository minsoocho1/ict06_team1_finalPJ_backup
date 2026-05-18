/**
 * @FileName : ContentLearningAssistService.java
 * @Description : 학습 콘텐츠 AI 요약, 재설명, 직접 질문 처리 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜        최초 생성, 콘텐츠 우측에 AI 학습 도우미 추가(핵심 요약/용어 정리, 간단한 질문-응답 챗봇)
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

    /**
     * @MethodName : explainContent
     * @Description : 문서 기반 핵심 요약 또는 쉬운 설명 생성
     *
     * @param contentId 콘텐츠 식별자
     * @param mode      summary 또는 simple
     * @return 학습 도우미 응답 DTO
     */
    public AiContentExplainResponseDto explainContent(Integer contentId, String mode) {
        OnContentEntity content = getContent(contentId);
        validateExplainableContent(content);

        DocumentEntity document = findLinkedDocument(content);
        String normalizedMode = normalizeMode(mode);
        String prompt = buildPrompt(content, normalizedMode);

        AiDocumentQuestionResponseDto answer = documentQuestionAnswerService.answerQuestion(document.getDocId(), prompt);
        return buildResponse(content, normalizedMode, normalizedMode, document, answer);
    }

    /**
     * @MethodName : askQuestion
     * @Description : 콘텐츠 문서 기반 직접 질문 답변 생성
     *
     * @param contentId 콘텐츠 식별자
     * @param question  사용자 질문
     * @return 학습 도우미 응답 DTO
     */
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

    /**
     * @MethodName : buildResponse
     * @Description : 학습 도우미 공통 응답 DTO 생성
     *
     * @param mode     응답 모드
     * @param document 연결 문서 엔티티
     * @param answer   문서 질문 응답 DTO
     * @return 학습 도우미 응답 DTO
     */
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

    /**
     * @MethodName : getContent
     * @Description : 콘텐츠 식별자 기준 콘텐츠 조회
     *
     * @param contentId 콘텐츠 식별자
     * @return 콘텐츠 엔티티
     */
    private OnContentEntity getContent(Integer contentId) {
        return onContentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("학습 콘텐츠를 찾을 수 없습니다."));
    }

    /**
     * @MethodName : validateExplainableContent
     * @Description : AI 학습 도우미 사용 가능 콘텐츠 유형 검증
     *
     * @param content 콘텐츠 엔티티
     */
    private void validateExplainableContent(OnContentEntity content) {
        ContentType type = content.getType();
        if (type != ContentType.PDF && type != ContentType.LINK) {
            throw new IllegalArgumentException("문서형 콘텐츠에서만 AI 학습 도우미를 사용할 수 있습니다.");
        }
    }

    /**
     * @MethodName : findLinkedDocument
     * @Description : 콘텐츠 경로 또는 제목 기준 연결 문서 조회
     *
     * @param content 콘텐츠 엔티티
     * @return 연결 문서 엔티티
     */
    private DocumentEntity findLinkedDocument(OnContentEntity content) {
        if (content.getPath() != null && !content.getPath().isBlank()) {
            return documentRepository.findFirstByFilePathOrderByCreatedAtDesc(content.getPath())
                    .orElseGet(() -> documentRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(content.getTitle())
                            .orElseThrow(() -> new IllegalArgumentException("연결된 문서가 없어 AI 학습 도우미를 사용할 수 없습니다.")));
        }

        return documentRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(content.getTitle())
                .orElseThrow(() -> new IllegalArgumentException("연결된 문서가 없어 AI 학습 도우미를 사용할 수 없습니다."));
    }

    /**
     * @MethodName : normalizeMode
     * @Description : 학습 도우미 모드 기본값 정규화
     *
     * @param mode 요청 모드
     * @return summary 또는 simple
     */
    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "summary";
        }
        return mode.trim().toLowerCase();
    }

    /**
     * @MethodName : buildPrompt
     * @Description : 요약/재설명 모드별 프롬프트 생성
     *
     * @param content 콘텐츠 엔티티
     * @param mode    응답 모드
     * @return 프롬프트 문자열
     */
    private String buildPrompt(OnContentEntity content, String mode) {
        if ("simple".equals(mode)) {
            return """
                    This is an onboarding study document.
                    Document title: '%s'

                    Task:
                    1. Do not repeat the same summary bullets.
                    2. Pick only difficult terms, technical concepts, or process concepts that a beginner may struggle with.
                    3. Write each item in this format:
                       - 용어: 쉬운 의미 정리.
                    4. Explain why the term matters in practice when possible.
                    5. Create 3 to 5 items only.
                    6. Use noun-style Korean endings only.
                    7. Never end with full sentence endings such as 합니다, 입니다, 됩니다.
                    8. Good ending examples:
                       - 전역 예외 처리 방식.
                       - 공통 오류 응답 기준.
                       - 운영 추적에 필요한 로그 기록 포인트.
                    9. Answer in Korean.
                    10. Do not invent anything that is not supported by the document.
                    """.formatted(content.getTitle());
        }

        return """
                This is an onboarding study document.
                Document title: '%s'

                Task:
                1. Summarize the document into 4 to 6 bullet points.
                2. Each bullet must be a meaningful summary line, not a heading only.
                3. Explain the meaning, purpose, procedure, or caution in each bullet.
                4. Use noun-style Korean endings only, not full sentence endings.
                5. Never end with 합니다, 입니다, 됩니다.
                6. Never append awkward label-like tails after a comma.
                5. Forbidden style examples:
                   - ... , 목적 기준
                   - ... , 구현 절차
                   - ... , 참고 자료 기준
                7. Good style examples:
                   - 전역 예외 처리 구조를 통한 오류 응답 일관성 확보.
                   - 예외 로그 기록을 통한 원인 추적 기반 마련.
                   - 공통 예외 처리 애노테이션 활용 절차 정리.
                8. End each bullet with a period.
                9. Cover purpose, key concepts, important procedures, and cautions when available.
                10. Answer in Korean.
                11. Do not invent anything that is not supported by the document.
                """.formatted(content.getTitle());
    }

    /**
     * @MethodName : buildQuestionPrompt
     * @Description : 직접 질문 응답 품질 보강용 프롬프트 생성
     *
     * @param content  콘텐츠 엔티티
     * @param question 사용자 질문
     * @return 프롬프트 문자열
     */
    private String buildQuestionPrompt(OnContentEntity content, String question) {
        return """
                This is a user question about an onboarding study document.
                Document title: '%s'
                User question: %s

                Answer rules:
                1. Use document evidence only.
                2. If the question asks about procedure, order, caution, important point, or practical flow, answer with 2 to 4 bullet points.
                3. If the document is overview-heavy, still organize whatever standards, responsibilities, exception-handling points, or caution points are present.
                4. Do not stop too early with 'the document does not say'. First provide the best structured answer based on available evidence.
                5. If document evidence is thin, you may add official external reference URLs relevant to the topic.
                6. Avoid generic theory-first answers. Prefer practical points that help the learner act or understand faster.
                7. End with one short line starting with '실무 포인트:' when useful.
                8. Answer in Korean.
                """.formatted(content.getTitle(), question);
    }

    /**
     * @MethodName : appendReferenceLinksIfNeeded
     * @Description : 저신뢰 답변 하단 공식 참고 URL 보강
     *
     * @param content     콘텐츠 엔티티
     * @param requestHint 요청 힌트
     * @param answer      원본 답변
     * @return URL 보강 답변
     */
    private String appendReferenceLinksIfNeeded(OnContentEntity content, String requestHint, String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }

        if (!isLowConfidenceAnswer(answer)) {
            return answer;
        }

        String referenceLinks = buildReferenceLinks(content, requestHint);
        if (referenceLinks.isBlank()) {
            return answer;
        }

        return answer + "\n\n추가 참고 URL\n" + referenceLinks;
    }

    /**
     * @MethodName : isLowConfidenceAnswer
     * @Description : 문서 근거 부족 답변 패턴 판별
     *
     * @param answer 답변 문자열
     * @return 저신뢰 여부
     */
    private boolean isLowConfidenceAnswer(String answer) {
        String normalized = answer.replaceAll("\\s+", "");
        return normalized.contains("문서만으로는")
                || normalized.contains("정확히파악할수없")
                || normalized.contains("명시적으로언급되지않")
                || normalized.contains("구체적인주의점이나지침은제공되지않")
                || normalized.contains("정확한답변이어렵");
    }

    /**
     * @MethodName : buildReferenceLinks
     * @Description : 주제별 공식 참고 URL 구성
     *
     * @param content     콘텐츠 엔티티
     * @param requestHint 요청 힌트
     * @return URL 목록 문자열
     */
    private String buildReferenceLinks(OnContentEntity content, String requestHint) {
        String source = ((content.getTitle() == null ? "" : content.getTitle()) + " "
                + (content.getCategory() == null ? "" : content.getCategory()) + " "
                + (content.getSubCategory() == null ? "" : content.getSubCategory()) + " "
                + (requestHint == null ? "" : requestHint)).toLowerCase();

        if (source.contains("spring")) {
            return """
                    - Spring Framework 예외 처리 공식 문서: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
                    - Spring Boot 예외 응답 공식 문서: https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.error-handling
                    """.trim();
        }
        if (source.contains("react")) {
            return """
                    - React 공식 문서(State 관리): https://react.dev/learn/managing-state
                    - React 공식 문서(컴포넌트 공유 상태): https://react.dev/learn/sharing-state-between-components
                    """.trim();
        }
        if (source.contains("figma") || source.contains("design")) {
            return """
                    - Figma Help Center: https://help.figma.com/
                    - W3C Design Systems 참고: https://design-system.w3.org/
                    """.trim();
        }
        if (source.contains("accessibility") || source.contains("웹 접근성") || source.contains("a11y")) {
            return """
                    - MDN 접근성 가이드: https://developer.mozilla.org/ko/docs/Learn/Accessibility
                    - WAI 접근성 참고: https://www.w3.org/WAI/fundamentals/accessibility-intro/
                    """.trim();
        }
        if (source.contains("aws")) {
            return """
                    - AWS 공식 시작 가이드: https://docs.aws.amazon.com/whitepapers/latest/aws-overview/introduction.html
                    - AWS 문서 메인: https://docs.aws.amazon.com/
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
