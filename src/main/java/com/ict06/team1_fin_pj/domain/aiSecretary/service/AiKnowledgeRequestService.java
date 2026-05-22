package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestCreateDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestSuggestionsDto;

import java.util.List;

public interface AiKnowledgeRequestService {

    KnowledgeResponseDto createRequest(KnowledgeRequestCreateDto requestDto);

    List<KnowledgeResponseDto> getMyRequests(String empNo);

    KnowledgeRequestSuggestionsDto getSuggestions();
}
