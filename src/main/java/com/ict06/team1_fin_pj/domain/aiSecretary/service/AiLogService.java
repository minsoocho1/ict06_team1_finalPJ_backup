package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import java.util.Map;

public interface AiLogService {

    void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );

    void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    );

    AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );

    AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    );

    void saveAssistantLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            String feature,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );
}
