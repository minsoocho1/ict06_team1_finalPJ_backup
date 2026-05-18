package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestResponseDto;

import java.util.List;

public interface AiTemplateRequestService {

    TemplateRequestResponseDto createRequest(TemplateRequestCreateRequestDto requestDto);

    List<TemplateRequestResponseDto> getMyRequests(String empNo);
}