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

        /*
         * AI 비서 문서 작성은 장기 보관 대상이므로 ASSISTANT 세션으로 생성한다.
         */
        AiChatSessionEntity session = aiSecretaryService.createSession(
                requestDto.getEmpNo(),
                SessionType.ASSISTANT,
                requestDto.getTitle()
        );

        /*
         * USER 메시지에는 사용자가 입력한 폼 내용을 요약 저장한다.
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

        /*
         * 기존 AiLogService는 Chatbot 전용 signature라면,
         * 우선은 이 부분을 생략하고 나중에 ASSISTANT 로그용 메서드를 추가해도 된다.
         *
         * 현재 빠른 연결이 목표이므로 아래는 선택사항.
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

        /*
         * [1] USER 메시지 저장
         * 사용자가 입력한 수정 요청을 대화 이력으로 남긴다.
         */
        AiChatMessageEntity userMessage = AiChatMessageEntity.builder()
                .role(MessageRole.USER)
                .content(buildReviseUserMessage(requestDto))
                .build();

        AiChatMessageEntity savedUserMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), userMessage);

        /*
         * [2] 현재 문서 + 수정 지시로 프롬프트 구성
         */
        String prompt = buildRevisePrompt(requestDto);

        String revisedContent;
        String modelName = "gemini";

        /*
         * [3] Gemini 호출
         * 실패하면 fallback 문서로 대체한다.
         */
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

        /*
         * [4] ASSISTANT 메시지 저장
         * 수정된 문서 전문을 ASSISTANT 메시지로 저장한다.
         */
        AiChatMessageEntity aiMessage = AiChatMessageEntity.builder()
                .role(MessageRole.ASSISTANT)
                .content(revisedContent)
                .modelName(modelName)
                .build();

        AiChatMessageEntity savedAiMessage =
                aiSecretaryService.saveMessage(requestDto.getSessionId(), aiMessage);

        /*
         * [5] AI_LOG 저장
         * 현재 AiLogService가 챗봇용 메서드만 있다면 우선 동일 구조를 재사용할 수 있다.
         * 단, 가능하면 아래 7번의 saveAssistantLog() 추가를 추천한다.
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
            log.warn("[AI_LOG] AI 문서 수정 로그 저장 실패. reason={}", logException.getMessage());
        }

        /*
         * [6] 프론트로 수정 결과 반환
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

    // AI 템플릿 생성
    @Override
    public AssistantTemplateResponseDto createTemplate(AssistantTemplateRequestDto requestDto) {

        // 값이 있다면 앞뒤 공백제거 | 값이 null 이거나 비어 있을 시 fallback return
        String category = defaultValue(requestDto.getCategory(), "보고");
        String dept = defaultValue(requestDto.getDept(), "공통");
        String situation = defaultValue(requestDto.getSituation(), "업무 문서 작성");
        String tone = defaultValue(requestDto.getTone(), "공식적");

        // 타입 분류 (REPORT / MINUTES / APPROVAL)
        String type = normalizeTemplateType(requestDto.getType(), category, situation);

        // 템플릿 생성 프롬프트 제작
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
            // [1] AI에 프롬프트 주입하여 답변 받아오기
            String rawAnswer = aiModelClient.generateAnswer(prompt);

            // [2] AI 답변(json 객체)을 Map<String, Object> 객체로 변환
            Map<String, Object> parsed = parseTemplateJson(rawAnswer);

            // [3-1] 값 추출하여 Object 값 → 문자열(String) 변환 (타이틀/ 스크랩션/ 내용/ 미리보기)
            String title = stringValue(parsed.get("title"), situation + " 템플릿");
            String description = stringValue(
                    parsed.get("description"),
                    dept + " 영역의 " + situation + " 상황에 맞춰 생성된 AI 템플릿입니다. "
            );
            String content = stringValue(parsed.get("content"), rawAnswer);

            // [3-2] Object 값 → List<String> 변환
            List<String> preview = listValue(parsed.get("preview"));

            // [3-3] content에서 preview가 없을 때 미리보기 항목 생성
            if (preview.isEmpty()) {
                preview = buildPreviewFromContent(content);
            }

            // [4] AssistantTemplateResponseDto에 값 집어넣기
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

            // 답변을 노출 시키지 못했을 때 노출 시키는 비상용 예비 템플릿 노출
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

    // AssistantTemplateResponseDto helpler : 값이 null이거나 비어 있을 시 fallback return/ 값이 있다면 앞뒤 공백제거
    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    // AssistantTemplateResponseDto helpler : 타입 분류 (REPORT / MINUTES / APPROVAL)
    private String normalizeTemplateType(String type, String category, String situation) {
        // [1] type을 통한 빠른 판단
        // 값이 없으면 "" return, 값이 있으면 앞뒤 공백 제거 후 모두 대문자로 변환
        String normalized = safe(type).toUpperCase();

        // 어떤 타입이 들어왔는지 분별
        if ("MINUTES".equals(normalized)) return "MINUTES";
        if ("APPROVAL".equals(normalized)) return "APPROVAL";
        if ("REPORT".equals(normalized)) return "REPORT";

        // [2] type을 기준으로 판단이 어려울 시 2차 판단
        // 카테고리 + " " + 상황 한줄로 합치고 모두 대문자로 변환
        String joined = (safe(category) + " " + safe(situation)).toUpperCase();

        // 한줄로 합친 값 내, 특정 단어가 있다면 분기, 없으면 기본 값(REPORT) return
        if (joined.contains("회의") || joined.contains("회의록")) {
            return "MINUTES";
        }

        if (joined.contains("결재") || joined.contains("승인")) {
            return "APPROVAL";
        }

        return "REPORT";
    }

    // AssistantTemplateResponseDto helpler : 템플릿 생성 프롬프트 제작
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
               당신은 사내 그룹웨어의 AI 문서 템플릿 생성 비서입니다.

            아래 조건에 맞는 업무 문서 템플릿을 생성하세요.

            문서 유형: %s
            카테고리: %s
            부서/업무 영역: %s
            사용 상황: %s
            톤앤매너: %s
            제목 포함 여부: %s
            기본 문단 포함 여부: %s
            서명 포함 여부: %s

            반드시 아래 JSON 형식만 반환하세요.
            설명 문장, 마크다운 코드블록, ```json 표시는 절대 포함하지 마세요.

            {
              "title": "템플릿 제목",
              "description": "템플릿 설명",
              "preview": ["1. 첫 번째 항목", "2. 두 번째 항목", "3. 세 번째 항목"],
              "content": "실제로 사용할 수 있는 템플릿 본문 전체"
            }

            작성 규칙:
            1. 한국어로 작성하세요.
            2. 실무자가 바로 복사해서 사용할 수 있는 형태로 작성하세요.
            3. 입력값에 없는 구체적인 날짜, 금액, 인명, 정책명은 지어내지 마세요.
            4. 빈칸은 [입력] 또는 [작성] 형태로 표시하세요.
            5. content에는 제목, 본문 구조, 필요한 입력 항목을 포함하세요.
            6. preview는 content의 핵심 목차 3~5개로 구성하세요.
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

    // AssistantTemplateResponseDto helpler : json 객체를 Map<String, Object> 객체로 변환
    private Map<String, Object> parseTemplateJson(String rawAnswer) throws Exception {
        // [1] 순수 json 데이터 추출
        String json = extractJson(rawAnswer);

        // [2] 문자열 → 자바 객체로 변환
        return objectMapper.readValue(
                json, // json 객체를
                new TypeReference<Map<String, Object>>() {} // Map<String, Object> 객체로 변환
        );
    }

    // parseTemplateJson helpler : 순수 json 데이터 추출
    private String extractJson(String rawAnswer) {
        if(rawAnswer == null){
            return "{}";
        }

        String text = rawAnswer.trim();

        // 마크다운 제어 문자(Code Block)"를 제거하고 순수한 JSON 데이터만 추출
        if (text.startsWith("```json")) { // ```json으로 시작하는가? (마크다운 형식 체크)
            text = text.substring(7).trim(); // 마크다운 잘라내고 순수 텍스트만 추출
        }

        if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length() -3).trim();
        }

        // {}를 기준으로 잘라 순수한 JSON 데이터만 추출
        int start = text.indexOf("{"); // 가장 먼저 등장하는 {의 위치(인덱스 번호)를 찾아 start 변수에 담기
        int end  = text.lastIndexOf("}"); // 맨 뒤에서부터 읽어 등장하는 }의 위치(인덱스 번호)를 찾아 end 변수에 담기

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    // AssistantTemplateResponseDto helper : Object 값 → 문자열(String) 변환
    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        // 어떤 값이든 가져온 값을 String으로 변경 후 앞뒤 공백제거
        String text = String.valueOf(value).trim();

        if (text.isBlank()) {
            return fallback;
        }

        return text;
    }

    // AssistantTemplateResponseDto helper : Object 값을 List<String>으로 안전하게 변환
    private List<String> listValue(Object value) {
        if (value == null) {
            return List.of(); // 아무것도 들어있지 않은 불변(Immutable) 리스트를 즉시 생성해서 반환
        }

        if (value instanceof List<?> list) { // value가 리스트 형태가 맞는지 확인하고, 맞다면 list라는 변수에 담는다
            return list.stream() // 리스트의 요소를 하나씩 넣기
                    .filter(item -> item != null) // list 내 값 중 null 1차 필터링
                    .map(String::valueOf) // 1차 필터링 된 값을 모두 문자열로 변환
                    .map(String::trim) // 2차 변환 된 문자열의 앞뒤 공백 제거
                    .filter(text -> !text.isBlank()) // 공백 제거 후 빈 값이 된 문자열 필터링
                    .toList(); // 최종 결과물
        }

        if (value instanceof String text) {
            return text.lines() // \n을 기준으로 문장을 한 줄씩 쪼개서 stream()으로 변환
                    .map(String::trim) // 쪼개진 문장의 앞뒤 공백 제거
                    .filter(line -> !line.isBlank()) // 공백 제거 후 빈 값이 된 문자열 필터링
                    .toList(); // 살아남은 문장들을 모아 새로운 리스트 생성
        }

        return List.of();
    }

    // AssistantTemplateResponseDto helper : content에서 preview가 없을 때 미리보기 항목 생성
    private List<String> buildPreviewFromContent(String content) {
        if (content == null || content.isBlank()) {
            return List.of("1. 개요", "2. 주요 내용", "3. 세부 항목", "4. 후속 계획");
        }

        List<String> lines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(5)
                .toList();

        if (lines.isEmpty()) {
            return List.of("1. 개요", "2. 주요 내용", "3. 세부 항목", "4. 후속 계획");
        }

        return lines;
    }

    // AssistantTemplateResponseDto helpler : 응답을 하지 못하거나 시스템 오류가 발생했을 때 노출시키는 비상용 예비 템플릿
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
            - 작성 목적: [작성]
            - 배경: [작성]

            2. 주요 내용
            - 핵심 내용: [작성]
            - 관련 부서/대상: %s
            - 사용 상황: %s

            3. 세부 항목
            - 일정: [작성]
            - 담당자: [작성]
            - 참고 사항: [작성]

            4. 후속 계획
            - 다음 단계: [작성]
            - 확인 필요 사항: [작성]
            %s
            """.formatted(
                title,
                dept,
                situation,
                includeSignature ? "\n작성자: [부서/이름]" : ""
        );

        return AssistantTemplateResponseDto.builder()
                .type(type)
                .category(category)
                .dept(dept)
                .situation(situation)
                .tone(tone)
                .title(title)
                .description(dept + " 영역의 " + situation + " 상황에 맞춘 기본 템플릿입니다.")
                .preview(List.of("1. 개요", "2. 주요 내용", "3. 세부 항목", "4. 후속 계획"))
                .content(content)
                .modelName("gemini-fallback")
                .fallback(true)
                .build();
    }

    // helper :
    private String buildUserInputSummary(AssistantDraftRequestDto requestDto) {
        return """
                문서 유형: %s
                제목: %s
                작성 목적: %s
                대상 독자: %s
                정리 대상: %s
                핵심 내용: %s
                원하는 분량/방식: %s
                """.formatted(
                safe(requestDto.getType()),
                safe(requestDto.getTitle()),
                safe(requestDto.getPurpose()),
                safe(requestDto.getAudience()),
                joinTargets(requestDto.getTargets()),
                safe(requestDto.getDetail()),
                safe(requestDto.getAmount())
        );
    }

    //
    private String buildDraftPrompt(AssistantDraftRequestDto requestDto) {
        String documentType = normalizeDocumentType(requestDto.getType());

        String typeLabel = switch (documentType) {
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유서";
            default -> "보고서";
        };

        String typeInstruction = switch (documentType) {
            case "MINUTES" -> """
                회의 목적, 주요 논의 내용, 결정 사항, 액션 아이템 순서로 정리하세요.
                발언록이 부족한 경우 내용을 지어내지 말고 입력된 핵심 내용 중심으로 정리하세요.
                """;
            case "APPROVAL" -> """
                결재 배경, 필요성, 기대 효과, 요청 사항 순서로 정리하세요.
                과장된 표현은 피하고 승인자가 이해하기 쉽게 작성하세요.
                """;
            default -> """
                개요, 작성 목적, 주요 내용, 시사점, 후속 계획 순서로 정리하세요.
                실무 보고서 형식으로 명확하고 간결하게 작성하세요.
                """;
        };

        return """
            당신은 사내 그룹웨어의 AI 문서 작성 비서입니다.

            다음 입력값을 바탕으로 %s 초안을 작성하세요.

            작성 규칙:
            1. 한국어로 작성하세요.
            2. 업무 문서에 적합한 공손하고 명확한 문체를 사용하세요.
            3. 입력값에 없는 구체적인 수치, 일정, 정책명은 지어내지 마세요.
            4. 제목과 소제목을 포함해 읽기 쉽게 구성하세요.
            5. 불필요한 설명 없이 문서 초안만 출력하세요.
            6. 마크다운 기호(#, ##, ###, *, -)는 사용하지 말고 일반 문서 형식으로 작성하세요.

            유형별 지시:
            %s

            입력값:
            - 제목: %s
            - 작성 목적: %s
            - 대상 독자: %s
            - 정리 대상: %s
            - 핵심 내용: %s
            - 원하는 분량/방식: %s
            """.formatted(
                typeLabel,
                typeInstruction,
                safe(requestDto.getTitle()),
                safe(requestDto.getPurpose()),
                safe(requestDto.getAudience()),
                joinTargets(requestDto.getTargets()),
                safe(requestDto.getDetail()),
                safe(requestDto.getAmount())
        );
    }

    //
    private String buildFallbackDraft(AssistantDraftRequestDto requestDto) {
        String typeLabel = switch (safe(requestDto.getType())) {
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유서";
            default -> "보고서";
        };

        return """
                %s 초안 생성이 일시적으로 원활하지 않습니다.

                아래 입력 내용을 기준으로 초안을 다시 생성해 주세요.

                제목: %s
                작성 목적: %s
                대상 독자: %s
                핵심 내용:
                %s
                """.formatted(
                typeLabel,
                safe(requestDto.getTitle()),
                safe(requestDto.getPurpose()),
                safe(requestDto.getAudience()),
                safe(requestDto.getDetail())
        );
    }

    // 공통 helpler : 값이 없으면 빈 글자("") return, 값이 있으면 앞뒤 공백 제거 후 return
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // 공통 helpler :
    private String joinTargets(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return "";
        }

        return String.join(", ", targets);
    }

    //
    private String buildReviseUserMessage(AssistantReviseRequestDto requestDto) {
        return """
            문서 수정 요청
            문서 유형: %s
            제목: %s
            수정 요청: %s
            """.formatted(
                safe(requestDto.getType()),
                safe(requestDto.getTitle()),
                safe(requestDto.getInstruction())
        );
    }

    //
    private String buildRevisePrompt(AssistantReviseRequestDto requestDto) {
        String typeLabel = switch (safe(requestDto.getType())) {
            case "MINUTES" -> "회의록";
            case "APPROVAL" -> "결재 사유서";
            default -> "보고서";
        };

        return """
            당신은 사내 그룹웨어의 AI 문서 작성 비서입니다.

            아래의 기존 %s 초안을 사용자의 수정 요청에 맞게 다시 작성하세요.

            작성 규칙:
            1. 한국어로 작성하세요.
            2. 기존 문서의 의미와 핵심 내용은 유지하세요.
            3. 사용자의 수정 요청을 우선 반영하세요.
            4. 입력값에 없는 구체적인 수치, 일정, 정책명은 지어내지 마세요.
            5. 불필요한 설명 없이 수정된 문서 본문만 출력하세요.
            6. 마크다운 기호(#, ##, ###, *, -)는 사용하지 말고 일반 문서 형식으로 작성하세요.

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

    //
    private String buildReviseFallback(AssistantReviseRequestDto requestDto) {
        return """
            문서 수정 요청이 일시적으로 원활하지 않습니다.

            아래는 기존 문서 내용입니다.
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