/**
 * @FileName : AiChatbotServiceImpl.java
 * @Description : 사용자 챗봇 질문 처리, RAG 검색 결과 기반 답변 생성 및 retrieval trace 저장 담당
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 (챗봇 질문 처리 및 AI 응답 메시지 저장)
 * @ 2026.05.12    송혜진        챗봇 세션 기반 질문 처리 및 AI_LOG 저장 흐름 정리
 * @ 2026.05.22    송혜진        RAG 검색 결과를 프롬프트에 반영하고 중복 threshold 후처리 제거
 * @ 2026.05.22    송혜진        답변에 사용된 RAG chunk 기준으로 AI_RETRIEVAL_TRACE 저장 연동
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatMessageResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotAskResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotReferenceDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import com.ict06.team1_fin_pj.domain.aiSecretary.llm.AiModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatbotServiceImpl implements AiChatbotService {

    private final AiSecretaryService aiSecretaryService;
    private final AiModelClient aiModelClient;
    private final AiLogService aiLogService;
    private final RagRetrievalService ragRetrievalService;
    private final AiRetrievalTraceService aiRetrievalTraceService;

    @Override
    @Transactional
    public ChatbotAskResponseDto ask(Integer sessionId, String content) {

        long startTime = System.currentTimeMillis();

        AiChatMessageEntity savedUserMessage = null;
        AiChatMessageEntity savedAiMessage = null;

        boolean providerSuccess = false;
        boolean fallback = false;
        String errorMessage = null;
        Map<String, String> permissionDeniedInfo = Map.of();

        AiChatMessageEntity userMessage = AiChatMessageEntity.builder()
                .role(MessageRole.USER)
                .content(content)
                .build();

        savedUserMessage = aiSecretaryService.saveMessage(sessionId, userMessage);
        String requesterEmpNo = savedUserMessage != null
                && savedUserMessage.getSession() != null
                && savedUserMessage.getSession().getEmployee() != null
                ? savedUserMessage.getSession().getEmployee().getEmpNo()
                : null;

        List<RagRetrievedChunkDto> ragChunks = List.of();
        try {
            ragChunks = ragRetrievalService.retrieveTopChunks(content, 3, requesterEmpNo);
            log.info("[RAG] retrieved chunk count={}", ragChunks.size());
            ragChunks = ragChunks.stream()
                    .filter(chunk -> chunk != null)
                    .toList();
            log.info("[RAG] context chunk count={}", ragChunks.size());
            permissionDeniedInfo = ragRetrievalService.consumeLastPermissionDeniedInfo();
        } catch (Exception e) {
            log.warn("[RAG] retrieval failed. fallback to non-RAG prompt. reason={}", e.getMessage());
            ragChunks = List.of();
            permissionDeniedInfo = Map.of();
        }

        String prompt = ragChunks.isEmpty()
                ? buildPrompt(content)
                : buildRagPrompt(content, ragChunks);

        String answer;
        String modelName = "gemini";

        try {
            answer = aiModelClient.generateAnswer(prompt);
            providerSuccess = true;
            fallback = false;
            modelName = "gemini";
        } catch (Exception e) {
            answer = buildFallbackAnswer(content);
            providerSuccess = false;
            fallback = true;
            errorMessage = e.getMessage();
            modelName = "gemini-fallback";
        }

        AiChatMessageEntity aiMessage = AiChatMessageEntity.builder()
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .modelName(modelName)
                .build();

        savedAiMessage = aiSecretaryService.saveMessage(sessionId, aiMessage);

        long durationMs = System.currentTimeMillis() - startTime;

        AiLogEntity savedAiLog = aiLogService.saveChatbotLogAndReturn(
                savedUserMessage,
                savedAiMessage,
                providerSuccess,
                fallback,
                durationMs,
                errorMessage,
                permissionDeniedInfo
        );

        log.debug(
                "[RAG trace] condition logId={}, providerSuccess={}, fallback={}, ragChunkCount={}",
                savedAiLog == null ? null : savedAiLog.getLogId(),
                providerSuccess,
                fallback,
                ragChunks == null ? null : ragChunks.size()
        );
        if (savedAiLog != null && providerSuccess && !fallback && !ragChunks.isEmpty()) {
            try {
                log.debug(
                        "[RAG trace] save call start logId={}, chunkCount={}",
                        savedAiLog.getLogId(),
                        ragChunks.size()
                );
                aiRetrievalTraceService.saveRetrievalTraces(savedAiLog, ragChunks);
            } catch (Exception e) {
                log.warn("[RAG] trace save failed. reason={}", e.getMessage(), e);
            }
        } else {
            log.debug(
                    "[RAG trace] save skipped. logId={}, providerSuccess={}, fallback={}, ragChunkCount={}",
                    savedAiLog == null ? null : savedAiLog.getLogId(),
                    providerSuccess,
                    fallback,
                    ragChunks == null ? null : ragChunks.size()
            );
        }

        List<ChatbotReferenceDto> references =
                providerSuccess && !fallback && !ragChunks.isEmpty()
                        ? buildReferences(ragChunks)
                        : List.of();

        return ChatbotAskResponseDto.builder()
                .userMessage(AiChatMessageResponseDto.from(savedUserMessage))
                .aiMessage(AiChatMessageResponseDto.from(savedAiMessage))
                .references(references)
                .build();
    }

    private List<ChatbotReferenceDto> buildReferences(List<RagRetrievedChunkDto> ragChunks) {
        if (ragChunks == null || ragChunks.isEmpty()) {
            return List.of();
        }

        Map<Integer, ChatbotReferenceDto> deduplicated = new LinkedHashMap<>();
        for (RagRetrievedChunkDto chunk : ragChunks) {
            if (chunk == null || chunk.getDocumentId() == null) {
                continue;
            }

            deduplicated.computeIfAbsent(chunk.getDocumentId(), docId ->
                    ChatbotReferenceDto.builder()
                            .docId(docId)
                            .title(safe(chunk.getDocumentTitle(), "참고 문서 " + docId))
                            .url(normalizeReferenceUrl(chunk.getFilePath()))
                            .build()
            );
        }

        return List.copyOf(deduplicated.values());
    }

    private String normalizeReferenceUrl(String filePath) {
        String normalized = safe(filePath, "");
        return normalized.isBlank() ? null : normalized;
    }

    private String buildPrompt(String userQuestion) {
        return buildNoContextPrompt(userQuestion);
    }

    private String buildNoContextPrompt(String userQuestion) {
        return """
                당신은 사내 그룹웨어 교육자료 검색용 AI 챗봇입니다.

                답변 규칙:
                1. 사용자의 질문에 한국어로 답하세요.
                2. 최대한 친절하고 간결하게 답하세요.
                3. 확실하지 않은 내용은 추측하지 말고, 확인이 필요하다고 안내하세요.
                4. 현재 관련 문서를 찾지 못했습니다. 자료 등록 요청이 필요한 경우 간단히 안내하세요.
                5. 답변은 3~6문장 정도로 작성하세요.

                사용자 질문:
                %s
                """.formatted(userQuestion);
    }

    private String buildRagPrompt(String userQuestion, List<RagRetrievedChunkDto> ragChunks) {
        String context = ragChunks.stream()
                .map(chunk -> """
                        - 문서명: %s
                          섹션: %s
                          유사도: %.4f
                          내용:
                          %s
                        """.formatted(
                        safe(chunk.getDocumentTitle(), "-"),
                        safe(chunk.getSectionTitle(), "-"),
                        chunk.getSimilarityScore() == null ? 0.0d : chunk.getSimilarityScore(),
                        limitPromptText(safe(chunk.getContent(), "-"), 1200)
                ))
                .collect(Collectors.joining("\n"));

        return """
                당신은 사내 그룹웨어 교육자료 검색용 AI 챗봇입니다.

                답변 규칙:
                1. 아래 참고 문서에 있는 내용만 근거로 답변하세요.
                2. 참고 문서에 없는 내용은 추측하지 마세요.
                3. 근거가 부족하면 자료 등록 요청이 필요하다고 안내하세요.
                4. 한국어로 실무적으로, 간결하게 답변하세요.
                5. 답변은 3~6문장 정도로 작성하세요.

                [참고 문서]
                %s

                [사용자 질문]
                %s
                """.formatted(context, userQuestion);
    }

    private String buildFallbackAnswer(String userQuestion) {
        return """
                현재 AI 응답 생성 요청이 일시적으로 지연되어 우선 안내 답변을 드립니다.

                입력하신 질문:
                "%s"

                지금은 관련 문서를 충분히 찾지 못했거나 AI 응답이 잠시 지연된 상태입니다.
                잠시 후 다시 시도해 주세요.
                """.formatted(userQuestion);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String limitPromptText(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, Math.max(maxLength - 1, 0)) + "...";
    }
}
