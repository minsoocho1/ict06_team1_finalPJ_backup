package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotReferenceDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;

import java.util.List;
import java.util.Map;

public interface AiSecretaryService {

    AiChatSessionEntity createSession(String empNo, SessionType sessionType, String title);

    AiChatSessionEntity createAssistantSession(String empNo, String title);

    AiChatSessionEntity getOrCreateChatbotSession(String empNo);

    List<AiChatSessionEntity> getSessionList(String empNo, SessionType sessionType);

    List<AiChatMessageEntity> getMessageList(Integer sessionId);

    Map<Integer, List<ChatbotReferenceDto>> getMessageReferences(List<Integer> assistantMessageIds);

    AiChatMessageEntity saveMessage(Integer sessionId, AiChatMessageEntity message);
}
