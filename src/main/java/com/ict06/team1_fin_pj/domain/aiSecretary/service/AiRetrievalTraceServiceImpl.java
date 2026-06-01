/**
 * @FileName : AiRetrievalTraceServiceImpl.java
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

import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiRetrievalTraceEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiDocumentRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiRetrievalTraceRepository;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRetrievalTraceServiceImpl implements AiRetrievalTraceService {

    private final AiRetrievalTraceRepository aiRetrievalTraceRepository;
    private final AiDocumentRepository aiDocumentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void saveRetrievalTraces(AiLogEntity aiLog, List<RagRetrievedChunkDto> topChunks) {
        log.debug(
                "[RAG trace] save requested logId={}, requestedCount={}",
                aiLog == null ? null : aiLog.getLogId(),
                topChunks == null ? null : topChunks.size()
        );

        if (aiLog == null || aiLog.getLogId() == null || topChunks == null || topChunks.isEmpty()) {
            log.warn(
                    "[RAG trace] skip chunk. reason={}, docId={}, chunkId={}",
                    "invalid-request",
                    null,
                    null
            );
            return;
        }

        int savedCount = 0;
        for (RagRetrievedChunkDto chunkDto : topChunks) {
            if (chunkDto == null || chunkDto.getDocumentId() == null || chunkDto.getChunkId() == null) {
                log.warn(
                        "[RAG trace] skip chunk. reason={}, docId={}, chunkId={}",
                        "invalid-chunk",
                        chunkDto == null ? null : chunkDto.getDocumentId(),
                        chunkDto == null ? null : chunkDto.getChunkId()
                );
                continue;
            }

            try {
                log.debug(
                        "[RAG trace] saving logId={}, docId={}, chunkId={}, similarity={}",
                        aiLog.getLogId(),
                        chunkDto.getDocumentId(),
                        chunkDto.getChunkId(),
                        chunkDto.getSimilarityScore()
                );
                DocumentEntity document = aiDocumentRepository.findById(chunkDto.getDocumentId()).orElse(null);
                DocChunkEntity chunk = entityManager.find(DocChunkEntity.class, chunkDto.getChunkId());
                if (document == null || chunk == null) {
                    log.warn(
                            "[RAG trace] skip chunk. reason={}, docId={}, chunkId={}",
                            document == null ? "document-not-found" : "chunk-not-found",
                            chunkDto.getDocumentId(),
                            chunkDto.getChunkId()
                    );
                    continue;
                }

                AiRetrievalTraceEntity trace = AiRetrievalTraceEntity.builder()
                        .log(entityManager.getReference(AiLogEntity.class, aiLog.getLogId()))
                        .document(document)
                        .chunk(chunk)
                        .similarityScore(toBigDecimal(chunkDto.getSimilarityScore()))
                        .rerankScore(null)
                        .usedInAnswer(Boolean.TRUE)
                        .build();

                aiRetrievalTraceRepository.save(trace);
                savedCount++;
            } catch (Exception e) {
                log.warn(
                        "[RAG trace] save failed logId={}, message={}",
                        aiLog.getLogId(),
                        e.getMessage(),
                        e
                );
            }
        }

        log.info("[RAG trace] save completed logId={}, savedCount={}", aiLog.getLogId(), savedCount);
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }
}
