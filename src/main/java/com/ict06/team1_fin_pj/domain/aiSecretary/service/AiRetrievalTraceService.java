/**
 * @FileName : AiRetrievalTraceService.java
 * @Description : AI RAG 검색 trace 저장 Service 구현체
 *                - 챗봇 답변 생성 시 실제 사용된 RAG 문서/청크 trace 저장
 *                - AI_LOG와 DOCUMENT, DOC_CHUNKS 간 검색 근거 연결
 *                - similarity score, rerank score, 답변 사용 여부 관리
 *                - 관리자 RAG 검증 및 AI 답변 근거 추적 데이터 제공
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.22    송혜진       권한 기반 RAG 검색 trace 저장 기준 정리
 * @ 2026.05.27    송혜진       최종 ragChunks 기준 references 및 trace 정책 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;

import java.util.List;

public interface AiRetrievalTraceService {

    void saveRetrievalTraces(AiLogEntity aiLog, List<RagRetrievedChunkDto> topChunks);
}
