// 세션 응답 DTO
package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionStatus;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiChatSessionResponseDto {

    private Integer sessionId;
    private String empNo;
    private SessionType sessionType;
    private String documentType;
    private String title;
    private SessionStatus status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
