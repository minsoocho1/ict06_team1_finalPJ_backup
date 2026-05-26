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
        saveChatbotLogAndReturn(userMessage, aiMessage, providerSuccess, fallback, durationMs, errorMessage);
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

        String queryMeta = buildQueryMeta(userMessage);
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

    private String buildQueryMeta(AiChatMessageEntity userMessage) {
        String content = userMessage.getContent();

        int questionLength = content == null ? 0 : content.length();

        return "requestMessageId=%d, questionLength=%d"
                .formatted(userMessage.getMessageId(), questionLength);
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
