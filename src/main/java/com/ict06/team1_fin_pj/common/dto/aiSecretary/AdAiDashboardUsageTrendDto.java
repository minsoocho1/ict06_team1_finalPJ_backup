package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardUsageTrendDto {
    private String label;
    private String dateValue;
    private long count;
    private long totalCount;
    private long chatbotCount;
    private long assistantCount;
}
