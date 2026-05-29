/**
 * @FileName : AiAssistantDraftServiceImpl.js
 * @Description : 보고서/회의록/결재 사유 초안 생성 흐름 담당
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.05.06    송혜진        최초 생성 (초안 생성/ 수정 메서드 추가)
 * @ 2026.05.11    송혜진        템플릿 생성 메서드 추가
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

    // 초안 생성
    @Override
    @Transactional
    public AssistantDraftResponseDto createDraft(AssistantDraftRequestDto requestDto) {

        long startTime = System.currentTimeMillis();

        boolean providerSuccess = false;
        boolean fallback = false;
        String errorMessage = null;

        // AI 비서 문서 작성은 장기 보관 대상이므로 ASSISTANT 세션으로 생성
        AiChatSessionEntity session = aiSecretaryService.createSession(
                requestDto.getEmpNo(),
                SessionType.ASSISTANT,
                requestDto.getTitle()
        );

        // USER 메시지에는 사용자가 입력한 요청 내용을 요약 저장
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
            log.warn("[ASSISTANT_DRAFT] Gemini 초안 생성 실패. fallback으로 대체합니다. reason={}", e.getMessage());

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

        // AI 비서 사용 로그 저장
        // Gemini 호출 성공 여부, fallback 여부, 처리 시간을 함께 기록한다.
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
            log.warn("[AI_LOG] AI 비서 초안 로그 저장 실패. reason={}", logException.getMessage());
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

    // 초안 수정
    @Override
    @Transactional
    public AssistantReviseResponseDto reviseDraft(AssistantReviseRequestDto requestDto) {

        long startTime = System.currentTimeMillis();

        boolean providerSuccess = false;
        boolean fallback = false;
        String errorMessage = null;

        // [1] USER 메시지 저장
        // 사용자가 입력한 수정 요청을 대화 이력으로 남김
        AiChatMessageEntity userMessage = AiChatMessageEntity.builder()
                .role(MessageRole.USER)
                .content(buildReviseUserMessage(requestDto))
                .build();

        AiChatMessageEntity savedUserMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), userMessage);

        // [2] 현재 문서 + 수정 지시로 프롬프트 구성
        String prompt = buildRevisePrompt(requestDto);

        String revisedContent;
        String modelName = "gemini";

        // [3] Gemini 호출
        // 실패하면 fallback 문서로 대체
        try {
            revisedContent = aiModelClient.generateAnswer(prompt);
            providerSuccess = true;
            fallback = false;
            modelName = "gemini";
        } catch (Exception e) {
            log.warn("[ASSISTANT_REVISE] Gemini 문서 수정 실패. fallback으로 대체합니다. reason={}", e.getMessage());

            revisedContent = buildReviseFallback(requestDto);
            providerSuccess = false;
            fallback = true;
            errorMessage = e.getMessage();
            modelName = "gemini-fallback";
        }

        // [4] ASSISTANT 메시지 저장
        // 수정된 문서 전문을 ASSISTANT 메시지로 저장
        AiChatMessageEntity aiMessage = AiChatMessageEntity.builder()
                .role(MessageRole.ASSISTANT)
                .content(revisedContent)
                .modelName(modelName)
                .build();

        AiChatMessageEntity savedAiMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), aiMessage);

        // [5] AI_LOG 저장
        // 수정 요청 처리 결과를 AI 비서 사용 로그로 남긴다.
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
            log.warn("[AI_LOG] AI 문서 수정 로그 저장 실패. reason={}", logException.getMessage());
        }

        // [6] 프론트로 수정 결과 반환
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


    // AI 템플릿 생성
    @Override
    public AssistantTemplateResponseDto createTemplate(AssistantTemplateRequestDto requestDto) {

        String category = defaultValue(requestDto.getCategory(), "일반");
        String dept = defaultValue(requestDto.getDept(), "공통");
        String situation = defaultValue(requestDto.getSituation(), "업무 상황");
        String tone = defaultValue(requestDto.getTone(), "정중한");

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

            String title = stringValue(parsed.get("title"), situation + " 템플릿");
            String description = stringValue(
                    parsed.get("description"),
                    dept + "에서 사용할 " + situation + " 템플릿입니다."
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
            log.warn("[ASSISTANT_TEMPLATE] Gemini 템플릿 생성 실패. fallback으로 대체합니다. reason={}", e.getMessage());

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

        if (joined.contains("회의") || joined.contains("회의록")) {
            return "MINUTES";
        }

        if (joined.contains("결재") || joined.contains("승인")) {
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
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유";
            default -> "보고서";
        };

        return """
            당신은 사내 업무 문서 템플릿을 작성하는 AI 비서입니다.

            아래 조건에 맞는 실무용 템플릿을 작성해 주세요.

            문서 유형: %s
            카테고리: %s
            사용 부서: %s
            사용 상황: %s
            작성 톤: %s
            제목 포함 여부: %s
            문단 구조 포함 여부: %s
            서명 영역 포함 여부: %s

            반드시 아래 JSON 형식으로만 응답해 주세요.
            설명 문장, 마크다운 코드블록, ```json 표기는 포함하지 마세요.

            {
              "title": "템플릿 제목",
              "description": "템플릿 설명",
              "preview": ["1. 주요 항목", "2. 주요 항목", "3. 주요 항목"],
              "content": "실제 사용 가능한 템플릿 본문"
            }

            작성 기준:
            1. 한국어로 작성하세요.
            2. 실제 회사 업무에서 바로 사용할 수 있는 형식으로 작성하세요.
            3. 필요한 경우 제목, 개요, 목적, 세부 내용, 요청 사항을 포함하세요.
            4. 변수 영역은 [항목] 또는 [입력값] 형태로 표시하세요.
            5. content에는 제목, 본문, 항목 구조를 모두 포함하세요.
            6. preview는 content의 핵심 항목 3~5개로 작성하세요.
            """.formatted(
                typeLabel,
                category,
                dept,
                situation,
                tone,
                includeTitle ? "포함" : "미포함",
                includeParagraphs ? "포함" : "미포함",
                includeSignature ? "포함" : "미포함"
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
            return List.of("1. 제목", "2. 주요 내용", "3. 세부 항목", "4. 마무리");
        }

        List<String> lines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(5)
                .toList();

        if (lines.isEmpty()) {
            return List.of("1. 제목", "2. 주요 내용", "3. 세부 항목", "4. 마무리");
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
        String title = situation + " 템플릿";

        String content = """
            %s

            1. 개요
            - 작성 목적: [입력]
            - 작성 배경: [입력]

            2. 주요 내용
            - 사용 부서: %s
            - 사용 상황: %s

            3. 세부 항목
            - 항목: [입력]
            - 담당자: [입력]
            - 처리 기한: [입력]

            4. 요청 사항
            - 검토 요청 내용: [입력]
            - 추가 확인 사항: [입력]
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
                .description(dept + "에서 사용할 " + situation + " 업무 템플릿입니다.")
                .preview(List.of("1. 개요", "2. 주요 내용", "3. 세부 항목", "4. 요청 사항"))
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

    // 문서 유형별 초안 생성 프롬프트를 구성한다.
    private String buildDraftPrompt(AssistantDraftRequestDto requestDto) {
        String documentType = normalizeDocumentType(requestDto.getType());

        // referenceText가 있으면 첨부 참고 자료 본문을 프롬프트에 포함한다.
        String referenceSection = buildReferenceTextSection(requestDto.getReferenceText());

        String typeLabel = switch (documentType) {
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유";
            default -> "보고서";
        };

        String typeInstruction = switch (documentType) {
            case "MINUTES" -> """
            회의 개요, 주요 논의 내용, 결정 사항, 액션 아이템을 포함해 주세요.
            정리되지 않은 회의 내용을 공식 회의록 문체로 정돈해 주세요.
            """;
            case "APPROVAL" -> """
            신청 배경, 필요성, 기대 효과, 결재 요청 문구를 포함해 주세요.
            업무상 필요성과 승인 사유가 명확하게 드러나도록 작성해 주세요.
            """;
            default -> """
            개요, 현황 분석, 주요 내용, 기대 효과, 결론을 포함해 주세요.
            보고 대상자가 빠르게 이해할 수 있도록 구조화해 주세요.
            """;
        };

        return """
        당신은 사내 문서를 작성하는 AI 비서입니다.

        아래 입력값을 바탕으로 %s 초안을 작성해 주세요.

        작성 기준:
        1. 한국어로 작성하세요.
        2. 업무 문서에 적합한 공식적인 문체를 사용하세요.
        3. 문서 유형에 맞는 구조를 갖추어 작성하세요.
        4. 불필요한 설명 없이 바로 사용할 수 있는 초안을 작성하세요.
        5. 사용자가 입력한 조건을 최대한 반영하세요.
        6. 필요한 경우 제목(#, ##, ###), 목록(*, -), 표 형식을 적절히 사용하세요.

        문서 유형별 작성 지침:
        %s%s

        사용자 입력:
        - 제목: %s
        - 작성 목적: %s
        - 대상 독자: %s
        - 보고 대상: %s
        - 상세 요청: %s
        - 분량/형식: %s
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

        // 프롬프트 길이가 과도하게 커지지 않도록 첨부 본문 길이를 제한한다.
        String limitedText = normalized.length() > MAX_REFERENCE_TEXT_PROMPT_LENGTH
                ? normalized.substring(0, MAX_REFERENCE_TEXT_PROMPT_LENGTH)
                : normalized;

        return """

        [첨부 참고 자료 본문]
        아래 내용은 사용자가 첨부한 참고 자료에서 추출한 본문입니다.
        이 내용을 우선 참고하되, 필요한 경우 사용자의 입력 조건과 함께 종합해 주세요.

        %s
        """.formatted(limitedText);
    }

    private String buildFallbackDraft(AssistantDraftRequestDto requestDto) {
        String typeLabel = switch (safe(requestDto.getType())) {
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유";
            default -> "보고서";
        };

        return """
            %s 초안 생성에 일시적인 문제가 발생했습니다.

            아래 입력 내용을 기준으로 문서 초안을 다시 생성해 주세요.

            제목: %s
            작성 목적: %s
            대상 독자: %s
            상세 요청:
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
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유";
            default -> "보고서";
        };

        return """
        당신은 사내 문서를 수정하는 AI 비서입니다.

        아래 기존 %s 문서를 사용자의 수정 요청에 맞게 다시 작성해 주세요.

        수정 기준:
        1. 한국어로 작성하세요.
        2. 사용자의 수정 요청을 충실히 반영하세요.
        3. 기존 문서의 핵심 내용은 유지하세요.
        4. 문서 유형에 맞는 제목, 본문, 목록, 표 형식을 유지하세요.
        5. 불필요한 설명 없이 수정된 문서만 작성하세요.
        6. 필요한 경우 제목(#, ##, ###), 목록(*, -), 표 형식을 적절히 사용하세요.

        문서 제목:
        %s

        기존 문서:
        %s

        사용자 수정 요청:
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
            문서 수정 중 일시적인 문제가 발생했습니다.
    
            기존 문서 내용을 유지합니다.
            잠시 후 다시 수정 요청을 시도해 주세요.
    
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
