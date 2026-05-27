package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;

import java.util.List;
import java.util.Map;

public interface RagRetrievalService {

    List<RagRetrievedChunkDto> retrieveTopChunks(String question, int topK, String empNo);

    Map<String, String> consumeLastPermissionDeniedInfo();
}
