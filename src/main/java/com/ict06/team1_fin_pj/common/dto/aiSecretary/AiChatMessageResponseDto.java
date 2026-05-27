// 메시지 응답 DTO
package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.MessageRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AiChatMessageResponseDto {

    private Integer messageId;
    private Integer sessionId;
    private MessageRole role;
    private String content;
    private Integer seqNo;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatbotReferenceDto> references;

    public static AiChatMessageResponseDto from(AiChatMessageEntity message) {
        return AiChatMessageResponseDto.builder()
                .messageId(message.getMessageId())
                .sessionId(message.getSession().getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .seqNo(message.getSeqNo())
                .modelName(message.getModelName())
                .promptTokens(message.getPromptTokens())
                .completionTokens(message.getCompletionTokens())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .references(List.of())
                .build();
    }

    public static AiChatMessageResponseDto from(
            AiChatMessageEntity message,
            List<ChatbotReferenceDto> references
    ) {
        return AiChatMessageResponseDto.builder()
                .messageId(message.getMessageId())
                .sessionId(message.getSession().getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .seqNo(message.getSeqNo())
                .modelName(message.getModelName())
                .promptTokens(message.getPromptTokens())
                .completionTokens(message.getCompletionTokens())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .references(references == null ? List.of() : references)
                .build();
    }

}

/* JSON 예시 - 메시지 저장
   {
     "messageId": 101,
     "sessionId": 5,
     "role": "USER",
     "content": "보고서 초안을 작성해줘",
     "seqNo": 1,
     "modelName": "gpt-4",
     "promptTokens": 150,
     "completionTokens": 300,
     "createdAt": "2024-05-20T14:30:00"
   }
*/

