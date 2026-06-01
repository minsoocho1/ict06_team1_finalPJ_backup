/**
 * @FileName : AiRetrievalTraceRepository.java
 * @Description : AI RAG 검색 trace Repository
 *                - 챗봇 답변 생성 시 실제 사용된 문서/청크 trace 저장
 *                - similarity score, rerank score, 사용 여부 관리
 *                - RAG 검색 검증 및 관리자 분석 데이터 조회에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.22  송혜진       권한 기반 RAG 검색 trace 저장 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiRetrievalTraceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRetrievalTraceRepository extends JpaRepository<AiRetrievalTraceEntity, Integer> {

    @EntityGraph(attributePaths = {"document", "log", "log.message"})
    List<AiRetrievalTraceEntity> findByLog_LogIdInAndUsedInAnswerTrueOrderByTraceIdAsc(List<Integer> logIds);
}
