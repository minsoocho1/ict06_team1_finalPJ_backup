package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardSummaryDto {
    private String label;
    private String value;
    private String unit;
    private String changeRate;
    private String compareText;
    private String trend;
}
