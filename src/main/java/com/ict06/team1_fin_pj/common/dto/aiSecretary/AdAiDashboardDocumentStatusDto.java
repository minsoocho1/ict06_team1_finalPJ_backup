package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardDocumentStatusDto {
    private String key;
    private String title;
    private long count;
    private String description;
    private String badgeClass;
}
