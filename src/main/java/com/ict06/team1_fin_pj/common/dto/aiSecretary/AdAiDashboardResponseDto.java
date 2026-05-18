package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdAiDashboardResponseDto {

    @Builder.Default
    private List<AdAiDashboardSummaryDto> summaryCards = List.of();

    @Builder.Default
    private List<AdAiDashboardFeatureUsageDto> featureUsageList = List.of();

    @Builder.Default
    private List<AdAiDashboardUsageTrendDto> usageTrendList = List.of();

    @Builder.Default
    private List<AdAiDashboardRecentLogDto> recentLogList = List.of();

    @Builder.Default
    private List<AdAiDashboardDocumentStatusDto> documentStatusList = List.of();

    @Builder.Default
    private String documentStatusTitle = "";

    @Builder.Default
    private int currentPage = 1;

    @Builder.Default
    private int totalPages = 0;

    @Builder.Default
    private long totalLogCount = 0L;

    @Builder.Default
    private boolean hasPrevious = false;

    @Builder.Default
    private boolean hasNext = false;
}

