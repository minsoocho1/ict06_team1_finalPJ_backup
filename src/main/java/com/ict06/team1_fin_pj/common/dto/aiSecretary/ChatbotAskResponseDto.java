package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiChatMessageResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatbotAskResponseDto {

    private AiChatMessageResponseDto userMessage;

    private AiChatMessageResponseDto aiMessage;

    private List<ChatbotReferenceDto> references;
}
