package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;

public interface AdAiSecretaryService {

    AdAiDashboardResponseDto getDashboardData(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result,
            int page
    );

    byte[] downloadRecentLogCsv(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result
    );
}
