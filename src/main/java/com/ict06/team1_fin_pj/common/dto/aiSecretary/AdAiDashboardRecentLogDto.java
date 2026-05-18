package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardRecentLogDto {
    private String logId;
    private String user;
    private String department;
    private String type;
    private String requestSummary;
    private String responseSummary;
    private String result;
    private String resultLabel;
    private String durationText;
    private String createdAt;
    private String errorMessage;
    private String messageContent;
    private String sessionId;
    private String messageId;
    private boolean fallback;
}
