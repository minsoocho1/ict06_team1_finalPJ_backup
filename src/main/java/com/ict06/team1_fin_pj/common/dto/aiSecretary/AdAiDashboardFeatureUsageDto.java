package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardFeatureUsageDto {
    private String label;
    private long count;
    private int percentage;
    private String barClass;
}
