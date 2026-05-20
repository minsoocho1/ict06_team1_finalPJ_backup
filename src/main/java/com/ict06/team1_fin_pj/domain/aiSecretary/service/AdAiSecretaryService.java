package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;

import java.util.List;
import java.util.Map;

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

    List<KnowledgeResponseDto> getKnowledgeRequestsForAdmin(
            String requestStartDate,
            String requestEndDate,
            String requestStatus,
            String requestType,
            String requestCategory
    );

    List<Map<String, Object>> getDocumentManagementRows(List<KnowledgeResponseDto> knowledgeRequests);

    KnowledgeResponseDto reviewKnowledgeRequest(
            Long requestId,
            String status,
            String adminComment,
            String reviewerEmpNo
    );
}
