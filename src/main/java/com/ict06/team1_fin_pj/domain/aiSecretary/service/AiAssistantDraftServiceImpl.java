/**
 * @FileName : AiAssistantDraftServiceImpl.js
 * @Description : 蹂닿퀬???뚯쓽濡?寃곗옱 ?ъ쑀 珥덉븞 ?앹꽦 ?먮쫫 ?대떦
 * @Author : ?≫삙吏?
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ ?섏젙??        ?섏젙??       ?섏젙?댁슜
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.05.06    ?≫삙吏?       理쒖큹 ?앹꽦 (珥덉븞 ?앹꽦/ ?섏젙 硫붿꽌??異붽?)
 * @ 2026.05.11    ?≫삙吏?       ?쒗뵆由??앹꽦 硫붿꽌??異붽?
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.*;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.llm.AiModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantDraftServiceImpl implements AiAssistantDraftService {

    // 첨부 참고 자료 본문은 프롬프트 길이를 과도하게 키우지 않도록 한 번 더 제한한다.
    private static final int MAX_REFERENCE_TEXT_PROMPT_LENGTH = 10_000;

    private final AiSecretaryService aiSecretaryService;
    private final AiModelClient aiModelClient;
    private final AiLogService aiLogService;
    private final ObjectMapper objectMapper;

    // 珥덉븞 ?앹꽦
    @Override
    @Transactional
    public AssistantDraftResponseDto createDraft(AssistantDraftRequestDto requestDto) {

        long startTime = System.currentTimeMillis();

        boolean providerSuccess = false;
        boolean fallback = false;
        String errorMessage = null;

        /*
         * AI 鍮꾩꽌 臾몄꽌 ?묒꽦? ?κ린 蹂닿? ??곸씠誘濡?ASSISTANT ?몄뀡?쇰줈 ?앹꽦?쒕떎.
         */
        AiChatSessionEntity session = aiSecretaryService.createSession(
                requestDto.getEmpNo(),
                SessionType.ASSISTANT,
                requestDto.getTitle()
        );

        /*
         * USER 硫붿떆吏?먮뒗 ?ъ슜?먭? ?낅젰?????댁슜???붿빟 ??ν븳??
         */
        AiChatMessageEntity userMessage = AiChatMessageEntity.builder()
                .role(MessageRole.USER)
                .content(buildUserInputSummary(requestDto))
                .build();

        AiChatMessageEntity savedUserMessage =
                aiSecretaryService.saveMessage(session.getSessionId(), userMessage);

        String prompt = buildDraftPrompt(requestDto);

        String content;
        String modelName = "gemini";

        try {
            content = aiModelClient.generateAnswer(prompt);
            providerSuccess = true;
            fallback = false;
            modelName = "gemini";
        } catch (Exception e) {
            log.warn("[ASSISTANT_DRAFT] Gemini 珥덉븞 ?앹꽦 ?ㅽ뙣. fallback?쇰줈 ?泥댄빀?덈떎. reason={}", e.getMessage());

            content = buildFallbackDraft(requestDto);
            providerSuccess = false;
            fallback = true;
            errorMessage = e.getMessage();
            modelName = "gemini-fallback";
        }

        AiChatMessageEntity aiMessage = AiChatMessageEntity.builder()
                .role(MessageRole.ASSISTANT)
                .content(content)
                .modelName(modelName)
                .build();

        AiChatMessageEntity savedAiMessage =
                aiSecretaryService.saveMessage(session.getSessionId(), aiMessage);

        long durationMs = System.currentTimeMillis() - startTime;

        /*
         * 湲곗〈 AiLogService??Chatbot ?꾩슜 signature?쇰㈃,
         * ?곗꽑? ??遺遺꾩쓣 ?앸왂?섍퀬 ?섏쨷??ASSISTANT 濡쒓렇??硫붿꽌?쒕? 異붽??대룄 ?쒕떎.
         *
         * ?꾩옱 鍮좊Ⅸ ?곌껐??紐⑺몴?대?濡??꾨옒???좏깮?ы빆.
         */
        try {
            aiLogService.saveAssistantLog(
                    savedUserMessage,
                    savedAiMessage,
                    "ASSISTANT_DRAFT",
                    providerSuccess,
                    fallback,
                    durationMs,
                    errorMessage
            );
        } catch (Exception logException) {
            log.warn("[AI_LOG] AI 鍮꾩꽌 珥덉븞 濡쒓렇 ????ㅽ뙣. reason={}", logException.getMessage());
        }

        return AssistantDraftResponseDto.builder()
                .sessionId(session.getSessionId())
                .userMessageId(savedUserMessage.getMessageId())
                .aiMessageId(savedAiMessage.getMessageId())
                .type(requestDto.getType())
                .title(requestDto.getTitle())
                .content(content)
                .modelName(modelName)
                .fallback(fallback)
                .build();
    }

    // 珥덉븞 ?섏젙
    @Override
    @Transactional
    public AssistantReviseResponseDto reviseDraft(AssistantReviseRequestDto requestDto) {

        long startTime = System.currentTimeMillis();

        boolean providerSuccess = false;
        boolean fallback = false;
        String errorMessage = null;

        /*
         * [1] USER 硫붿떆吏 ???
         * ?ъ슜?먭? ?낅젰???섏젙 ?붿껌??????대젰?쇰줈 ?④릿??
         */
        AiChatMessageEntity userMessage = AiChatMessageEntity.builder()
                .role(MessageRole.USER)
                .content(buildReviseUserMessage(requestDto))
                .build();

        AiChatMessageEntity savedUserMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), userMessage);

        /*
         * [2] ?꾩옱 臾몄꽌 + ?섏젙 吏?쒕줈 ?꾨＼?꾪듃 援ъ꽦
         */
        String prompt = buildRevisePrompt(requestDto);

        String revisedContent;
        String modelName = "gemini";

        /*
         * [3] Gemini ?몄텧
         * ?ㅽ뙣?섎㈃ fallback 臾몄꽌濡??泥댄븳??
         */
        try {
            revisedContent = aiModelClient.generateAnswer(prompt);
            providerSuccess = true;
            fallback = false;
            modelName = "gemini";
        } catch (Exception e) {
            log.warn("[ASSISTANT_REVISE] Gemini 臾몄꽌 ?섏젙 ?ㅽ뙣. fallback?쇰줈 ?泥댄빀?덈떎. reason={}", e.getMessage());

            revisedContent = buildReviseFallback(requestDto);
            providerSuccess = false;
            fallback = true;
            errorMessage = e.getMessage();
            modelName = "gemini-fallback";
        }

        /*
         * [4] ASSISTANT 硫붿떆吏 ???
         * ?섏젙??臾몄꽌 ?꾨Ц??ASSISTANT 硫붿떆吏濡???ν븳??
         */
        AiChatMessageEntity aiMessage = AiChatMessageEntity.builder()
                .role(MessageRole.ASSISTANT)
                .content(revisedContent)
                .modelName(modelName)
                .build();

        AiChatMessageEntity savedAiMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), aiMessage);

        /*
         * [5] AI_LOG ???
         * ?꾩옱 AiLogService媛 梨쀫큸??硫붿꽌?쒕쭔 ?덈떎硫??곗꽑 ?숈씪 援ъ“瑜??ъ궗?⑺븷 ???덈떎.
         * ?? 媛?ν븯硫??꾨옒 7踰덉쓽 saveAssistantLog() 異붽?瑜?異붿쿇?쒕떎.
         */
        long durationMs = System.currentTimeMillis() - startTime;

        try {
            aiLogService.saveAssistantLog(
                    savedUserMessage,
                    savedAiMessage,
                    "ASSISTANT_REVISE",
                    providerSuccess,
                    fallback,
                    durationMs,
                    errorMessage
            );
        } catch (Exception logException) {
            log.warn("[AI_LOG] AI 臾몄꽌 ?섏젙 濡쒓렇 ????ㅽ뙣. reason={}", logException.getMessage());
        }

        /*
         * [6] ?꾨줎?몃줈 ?섏젙 寃곌낵 諛섑솚
         */
        return AssistantReviseResponseDto.builder()
                .sessionId(requestDto.getSessionId())
                .userMessageId(savedUserMessage.getMessageId())
                .aiMessageId(savedAiMessage.getMessageId())
                .type(requestDto.getType())
                .title(requestDto.getTitle())
                .content(revisedContent)
                .modelName(modelName)
                .fallback(fallback)
                .build();
    }


    // AI ??? ??
    @Override
    public AssistantTemplateResponseDto createTemplate(AssistantTemplateRequestDto requestDto) {

        String category = defaultValue(requestDto.getCategory(), "??");
        String dept = defaultValue(requestDto.getDept(), "??");
        String situation = defaultValue(requestDto.getSituation(), "???? ?? ??");
        String tone = defaultValue(requestDto.getTone(), "???");

        String type = normalizeTemplateType(requestDto.getType(), category, situation);

        String prompt = buildTemplatePrompt(
                type,
                category,
                dept,
                situation,
                tone,
                Boolean.TRUE.equals(requestDto.getIncludeTitle()),
                Boolean.TRUE.equals(requestDto.getIncludeParagraphs()),
                Boolean.TRUE.equals(requestDto.getIncludeSignature())
        );

        try {
            String rawAnswer = aiModelClient.generateAnswer(prompt);
            Map<String, Object> parsed = parseTemplateJson(rawAnswer);

            String title = stringValue(parsed.get("title"), situation + " ???");
            String description = stringValue(
                    parsed.get("description"),
                    dept + " ??? " + situation + " ??? ?? AI ??????."
            );
            String content = stringValue(parsed.get("content"), rawAnswer);
            List<String> preview = listValue(parsed.get("preview"));

            if (preview.isEmpty()) {
                preview = buildPreviewFromContent(content);
            }

            return AssistantTemplateResponseDto.builder()
                    .type(type)
                    .category(category)
                    .dept(dept)
                    .situation(situation)
                    .tone(tone)
                    .title(title)
                    .description(description)
                    .preview(preview)
                    .content(content)
                    .modelName("gemini")
                    .fallback(false)
                    .build();

        } catch (Exception e) {
            log.warn("[ASSISTANT_TEMPLATE] Gemini ??? ?? ??. fallback?? ?????. reason={}", e.getMessage());

            return buildFallbackTemplate(
                    type,
                    category,
                    dept,
                    situation,
                    tone,
                    Boolean.TRUE.equals(requestDto.getIncludeSignature())
            );
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeTemplateType(String type, String category, String situation) {
        String normalized = safe(type).toUpperCase();

        if ("MINUTES".equals(normalized)) return "MINUTES";
        if ("APPROVAL".equals(normalized)) return "APPROVAL";
        if ("REPORT".equals(normalized)) return "REPORT";

        String joined = (safe(category) + " " + safe(situation)).toUpperCase();

        if (joined.contains("??") || joined.contains("???")) {
            return "MINUTES";
        }

        if (joined.contains("??") || joined.contains("??")) {
            return "APPROVAL";
        }

        return "REPORT";
    }

    private String buildTemplatePrompt(
            String type,
            String category,
            String dept,
            String situation,
            String tone,
            boolean includeTitle,
            boolean includeParagraphs,
            boolean includeSignature
    ) {
        String typeLabel = switch (type) {
            case "MINUTES" -> "???";
            case "APPROVAL" -> "?? ???";
            default -> "???";
        };

        return """
                ??? ?? ????? AI ?? ?? ?????.

                ?? ??? ?? ?? ?? ???? ?????.

                ?? ??: %s
                ????: %s
                ?? ??: %s
                ?? ??: %s
                ??? ?: %s
                ?? ?? ??: %s
                ?? ?? ?? ??: %s
                ?? ?? ?? ??: %s

                ??? ?? JSON ??? ?????.
                ?? ??, ???? ????, ```json ?? ??? ?? ???.

                {
                  "title": "??? ??",
                  "description": "??? ??",
                  "preview": ["1. ? ?? ??", "2. ? ?? ??", "3. ? ?? ??"],
                  "content": "??? ??? ? ?? ??? ?? ??"
                }

                ?? ??:
                1. ???? ?????.
                2. ?? ???? ??? ? ?? ??? ?????.
                3. ???? ?? ???? ??, ??, ??, ???? ???? ???.
                4. ??? [??] ?? [??] ???? ?????.
                5. content?? ??, ?? ??, ??? ?? ??? ?????.
                6. preview? content? ?? ?? 3~5?? ?????.
                """.formatted(
                typeLabel,
                category,
                dept,
                situation,
                tone,
                includeTitle ? "??" : "???",
                includeParagraphs ? "??" : "???",
                includeSignature ? "??" : "???"
        );
    }

    private Map<String, Object> parseTemplateJson(String rawAnswer) throws Exception {
        String json = extractJson(rawAnswer);
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private String extractJson(String rawAnswer) {
        if (rawAnswer == null) {
            return "{}";
        }

        String text = rawAnswer.trim();

        if (text.startsWith("```json")) {
            text = text.substring(7).trim();
        }

        if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private List<String> listValue(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(text -> !text.isBlank())
                    .toList();
        }

        if (value instanceof String text) {
            return text.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        }

        return List.of();
    }

    private List<String> buildPreviewFromContent(String content) {
        if (content == null || content.isBlank()) {
            return List.of("1. ??", "2. ?? ??", "3. ?? ??", "4. ?? ??");
        }

        List<String> lines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(5)
                .toList();

        if (lines.isEmpty()) {
            return List.of("1. ??", "2. ?? ??", "3. ?? ??", "4. ?? ??");
        }

        return lines;
    }

    private AssistantTemplateResponseDto buildFallbackTemplate(
            String type,
            String category,
            String dept,
            String situation,
            String tone,
            Boolean includeSignature
    ) {
        String title = situation + " ???";

        String content = """
                %s

                1. ??
                - ?? ??: [??]
                - ?? ??: [??]

                2. ?? ??
                - ?? ??: %s
                - ?? ??: %s

                3. ?? ??
                - ??: [??]
                - ???: [??]
                - ?? ??: [??]

                4. ?? ??
                - ?? ?? ??: [??]
                - ?? ?? ? ??: [??]
                %s
                """.formatted(
                title,
                dept,
                situation,
                includeSignature ? "\n서명: [관련 부서명]" : ""
        );

        return AssistantTemplateResponseDto.builder()
                .type(type)
                .category(category)
                .dept(dept)
                .situation(situation)
                .tone(tone)
                .title(title)
                .description(dept + " ??? " + situation + " ??? ?? ?? ??????.")
                .preview(List.of("1. ??", "2. ?? ??", "3. ?? ??", "4. ?? ??"))
                .content(content)
                .modelName("gemini-fallback")
                .fallback(true)
                .build();
    }

    private String buildUserInputSummary(AssistantDraftRequestDto requestDto) {
        String title = safe(requestDto.getTitle());
        if (!title.isBlank()) {
            return title + " 작성 요청";
        }

        String purpose = singleLine(requestDto.getPurpose());
        if (!purpose.isBlank()) {
            return purpose;
        }

        String detail = singleLine(requestDto.getDetail());
        if (!detail.isBlank()) {
            return shorten(detail, 120);
        }

        return switch (normalizeDocumentType(requestDto.getType())) {
            case "MINUTES" -> "회의록 작성 요청";
            case "APPROVAL" -> "결재 사유 작성 요청";
            default -> "AI 문서 초안 작성 요청";
        };
    }

    // ?? ?? ?? ??? ?? ?? ????? ???.
    private String buildDraftPrompt(AssistantDraftRequestDto requestDto) {
        String documentType = normalizeDocumentType(requestDto.getType());
        // referenceText? ?? ?? ?? ?? ?? ??? ???.
        String referenceSection = buildReferenceTextSection(requestDto.getReferenceText());

        String typeLabel = switch (documentType) {
            case "MINUTES" -> "???";
            case "APPROVAL" -> "?? ???";
            default -> "???";
        };

        String typeInstruction = switch (documentType) {
            case "MINUTES" -> """
                ?? ??, ?? ?? ??, ?? ??, ?? ??? ??? ?????.
                ???? ??? ?? ??? ???? ?? ??? ?? ?? ???? ?????.
                """;
            case "APPROVAL" -> """
                ?? ??, ???, ?? ??, ?? ?? ??? ?????.
                ??? ??? ??? ???? ???? ?? ?????.
                """;
            default -> """
                ??, ?? ??, ?? ??, ???, ?? ?? ??? ?????.
                ?? ??? ???? ???? ???? ?????.
                """;
        };

        return """
            ??? ?? ????? AI ?? ?? ?????.

            ?? ???? ???? %s ??? ?????.

            ?? ??:
            1. ???? ?????.
            2. ?? ??? ??? ???? ??? ??? ?????.
            3. ???? ?? ???? ??, ??, ???? ???? ???.
            4. ??? ???? ??? ?? ?? ?????.
            5. ???? ?? ?? ?? ??? ?????.
            6. ???? ??(#, ##, ###, *, -)? ???? ?? ?? ?? ???? ?????.

            ??? ??:
            %s%s

            ???:
            - ??: %s
            - ?? ??: %s
            - ?? ??: %s
            - ?? ??: %s
            - ?? ??: %s
            - ??? ??/??: %s
            """.formatted(
                typeLabel,
                typeInstruction,
                referenceSection,
                safe(requestDto.getTitle()),
                safe(requestDto.getPurpose()),
                safe(requestDto.getAudience()),
                joinTargets(requestDto.getTargets()),
                safe(requestDto.getDetail()),
                safe(requestDto.getAmount())
        );
    }

    private String buildReferenceTextSection(String referenceText) {
        String normalized = safe(referenceText).trim();
        if (normalized.isEmpty()) {
            return "";
        }

        // ????? ????? ????? ? ? ? ????.
        String limitedText = normalized.length() > MAX_REFERENCE_TEXT_PROMPT_LENGTH
                ? normalized.substring(0, MAX_REFERENCE_TEXT_PROMPT_LENGTH)
                : normalized;

        return """

            [?? ?? ?? ??]
            ?? ??? ???? ??? ???? ?? ??? ?????.
            ? ??? ?? ????, ?? ??? ?? ??? ??? ??? ???.

            %s
            """.formatted(limitedText);
    }

    private String buildFallbackDraft(AssistantDraftRequestDto requestDto) {
        String typeLabel = switch (safe(requestDto.getType())) {
            case "MINUTES" -> "???";
            case "APPROVAL" -> "?? ???";
            default -> "???";
        };

        return """
                %s ?? ??? ????? ??? ? ?? ?????.

                ?? ?? ??? ???? ?? ?? ??? ??? ???.

                ??: %s
                ?? ??: %s
                ?? ??: %s
                ?? ??:
                %s
                """.formatted(
                typeLabel,
                safe(requestDto.getTitle()),
                safe(requestDto.getPurpose()),
                safe(requestDto.getAudience()),
                safe(requestDto.getDetail())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String joinTargets(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return "";
        }

        return String.join(", ", targets);
    }

    private String buildReviseUserMessage(AssistantReviseRequestDto requestDto) {
        String instruction = singleLine(requestDto.getInstruction());
        if (!instruction.isBlank()) {
            return shorten(instruction, 200);
        }

        return "문서 수정 요청";
    }

    private String singleLine(String value) {
        return safe(value)
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String shorten(String value, int maxLength) {
        String normalized = safe(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength).trim() + "...";
    }

    private String buildRevisePrompt(AssistantReviseRequestDto requestDto) {
        String typeLabel = switch (safe(requestDto.getType())) {
            case "MINUTES" -> "???";
            case "APPROVAL" -> "?? ???";
            default -> "???";
        };

        return """
            ??? ?? ????? AI ?? ?? ?????.

            ??? ?? %s ??? ???? ?? ??? ?? ?? ?????.

            ?? ??:
            1. ???? ?????.
            2. ?? ??? ?? ??? ?????.
            3. ???? ?? ??? ?? ?????.
            4. ???? ?? ???? ??, ??, ???? ???? ???.
            5. ???? ?? ?? ??? ?? ??? ?????.
            6. ???? ??(#, ##, ###, *, -)? ???? ?? ?? ?? ???? ?????.

            ?? ??:
            %s

            ?? ??:
            %s

            ??? ?? ??:
            %s
            """.formatted(
                typeLabel,
                safe(requestDto.getTitle()),
                safe(requestDto.getCurrentContent()),
                safe(requestDto.getInstruction())
        );
    }

    private String buildReviseFallback(AssistantReviseRequestDto requestDto) {
        return """
            ?? ?? ??? ?? ??? ? ?? ?????.

            ??? ?? ?? ?????.
            ?? ? ?? ?? ??? ??? ???.

            %s
            """.formatted(
                safe(requestDto.getCurrentContent())
        );
    }

    private String normalizeDocumentType(String type) {
        if (type == null || type.isBlank()) {
            return "REPORT";
        }

        String normalized = type.trim().toUpperCase();

        return switch (normalized) {
            case "MINUTES" -> "MINUTES";
            case "APPROVAL" -> "APPROVAL";
            default -> "REPORT";
        };
    }
}
