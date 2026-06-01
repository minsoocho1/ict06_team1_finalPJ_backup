/**
 * @FileName : AiLogServiceImpl.java
 * @Description : AI 사용 로그 Service 구현체
 *                - AI 비서 및 챗봇 사용 로그 저장
 *                - 사용자 요청/AI 응답 메시지 기반 로그 데이터 구성
 *                - 성공 여부, fallback 여부, 처리 시간, 오류 메시지 기록
 *                - 관리자 AI 운영 대시보드 및 권한 차단 로그 조회 데이터 제공
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.12    송혜진       AI 비서/챗봇 사용 로그 저장 구조 정리
 * @ 2026.05.22    송혜진       RAG 권한 차단 및 retrieval trace 연계 기준 반영
 * @ 2026.05.27    송혜진       관리자 AI 대시보드 및 권한 차단 로그 조회 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiLogRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLogServiceImpl implements AiLogService {

    private final AiLogRepository aiLogRepository;

    /**
     * CHATBOT 요청 1건에 대한 AI_LOG 저장.
     *
     * 현재 AI_LOG 테이블 구조상 provider/fallback 컬럼이 따로 없으므로
     * query/response에는 원문이 아니라 분석용 메타 정보만 저장한다.
     */
    @Override
    @Transactional
    public void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    ) {
        saveChatbotLogAndReturn(userMessage, aiMessage, providerSuccess, fallback, durationMs, errorMessage, null);
    }

    @Override
    @Transactional
    public void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    ) {
        saveChatbotLogAndReturn(userMessage, aiMessage, providerSuccess, fallback, durationMs, errorMessage, permissionDeniedInfo);
    }

    @Override
    @Transactional
    public AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    ) {
        return saveChatbotLogAndReturn(userMessage, aiMessage, providerSuccess, fallback, durationMs, errorMessage, null);
    }

    @Override
    @Transactional
    public AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    ) {
        if (userMessage == null || aiMessage == null) {
            log.warn("[AI_LOG] skip save: userMessage or aiMessage is null");
            return null;
        }

        if (userMessage.getMessageId() == null || aiMessage.getMessageId() == null) {
            log.warn(
                    "[AI_LOG] skip save: missing messageId. userMessageId={}, aiMessageId={}",
                    userMessage.getMessageId(),
                    aiMessage.getMessageId()
            );
            return null;
        }

        AiChatSessionEntity session = aiMessage.getSession();
        EmpEntity employee = session != null ? session.getEmployee() : null;

        String queryMeta = buildQueryMeta(userMessage, permissionDeniedInfo);
        String responseMeta = buildResponseMeta(aiMessage, providerSuccess, fallback);

        AiLogEntity log = AiLogEntity.builder()
                .employee(employee)
                .session(session)
                .message(aiMessage)
                .type(AiLogType.CHATBOT)
                .query(queryMeta)
                .response(responseMeta)
                .durationMs((int) durationMs)
                .success(true)
                .errorMessage(trimErrorMessage(errorMessage))
                .build();

        return aiLogRepository.save(log);
    }

    private String buildQueryMeta(AiChatMessageEntity userMessage, Map<String, String> permissionDeniedInfo) {
        String content = userMessage.getContent();

        int questionLength = content == null ? 0 : content.length();

        StringBuilder meta = new StringBuilder("requestMessageId=%d, questionLength=%d"
                .formatted(userMessage.getMessageId(), questionLength));

        if (permissionDeniedInfo != null && !permissionDeniedInfo.isEmpty()) {
            appendMeta(meta, "permissionDenied", permissionDeniedInfo.get("permissionDenied"));
            appendMeta(meta, "deniedReason", permissionDeniedInfo.get("deniedReason"));
            appendMeta(meta, "deniedDocId", permissionDeniedInfo.get("deniedDocId"));
            appendMeta(meta, "deniedDocTitle", permissionDeniedInfo.get("deniedDocTitle"));
            appendMeta(meta, "userHeadquarter", permissionDeniedInfo.get("userHeadquarter"));
            appendMeta(meta, "userTeam", permissionDeniedInfo.get("userTeam"));
            appendMeta(meta, "userPosition", permissionDeniedInfo.get("userPosition"));
            appendMeta(meta, "targetDept", permissionDeniedInfo.get("targetDept"));
        }

        return meta.toString();
    }

    private String buildResponseMeta(
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback
    ) {
        return "responseMessageId=%d, modelName=%s, providerSuccess=%s, fallback=%s"
                .formatted(
                        aiMessage.getMessageId(),
                        aiMessage.getModelName(),
                        providerSuccess,
                        fallback
                );
    }

    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        if (errorMessage.length() <= 1000) {
            return errorMessage;
        }

        return errorMessage.substring(0, 1000);
    }

    private void appendMeta(StringBuilder meta, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }

        meta.append(", ")
                .append(key)
                .append("=")
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    //
    @Override
    @Transactional
    public void saveAssistantLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            String feature,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    ) {
        if (userMessage == null || aiMessage == null) {
            return;
        }

        AiChatSessionEntity session = aiMessage.getSession();
        EmpEntity employee = session != null ? session.getEmployee() : null;

        String queryMeta = "feature=%s, requestMessageId=%d, inputLength=%d"
                .formatted(
                        feature,
                        userMessage.getMessageId(),
                        userMessage.getContent() == null ? 0 : userMessage.getContent().length()
                );

        String responseMeta = "responseMessageId=%d, modelName=%s, providerSuccess=%s, fallback=%s"
                .formatted(
                        aiMessage.getMessageId(),
                        aiMessage.getModelName(),
                        providerSuccess,
                        fallback
                );

        AiLogEntity log = AiLogEntity.builder()
                .employee(employee)
                .session(session)
                .message(aiMessage)
                .type(AiLogType.ASSISTANT)
                .query(queryMeta)
                .response(responseMeta)
                .durationMs((int) durationMs)
                .success(true)
                .errorMessage(trimErrorMessage(errorMessage))
                .build();

        aiLogRepository.save(log);
    }
}
