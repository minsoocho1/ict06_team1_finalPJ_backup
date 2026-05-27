package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;

import java.util.List;

public interface AiRetrievalTraceService {

    void saveRetrievalTraces(AiLogEntity aiLog, List<RagRetrievedChunkDto> topChunks);
}
