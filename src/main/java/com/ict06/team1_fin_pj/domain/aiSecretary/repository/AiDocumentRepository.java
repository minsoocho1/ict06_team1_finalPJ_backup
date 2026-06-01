/**
 * @FileName : AiDocumentRepository.java
 * @Description : AI/RAG 문서 관리 Repository
 *                - AI_RAG 문서 목록 및 상세 데이터 조회
 *                - 문서 상태, 접근 권한, 문서명 기준 필터링 조회
 *                - 관리자 RAG 문서 관리 및 챗봇 검색 대상 문서 조회에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.05.12  송혜진       최초 생성
 * @ 2026.05.22  송혜진       RAG 문서 상태 및 PUBLISHED 검색 기준 반영
 * @ 2026.05.27  송혜진       문서 상세 관리 및 권한 조건 조회 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentDomain;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AiDocumentRepository extends JpaRepository<DocumentEntity, Integer> {

    @Query(value = "select count(*) from document d where d.current_stage = :currentStage", nativeQuery = true)
    long countByCurrentStage(@Param("currentStage") String currentStage);

    @Query(value = "select count(*) from document d where d.current_stage in (:currentStages)", nativeQuery = true)
    long countByCurrentStageIn(@Param("currentStages") Collection<String> currentStages);

    @Query("select d from DocumentEntity d where d.documentDomain = :documentDomain order by d.createdAt desc")
    List<DocumentEntity> findByDocumentDomainOrderByCreatedAtDesc(@Param("documentDomain") DocumentDomain documentDomain);

    default List<DocumentEntity> findAiRagDocuments() {
        return findByDocumentDomainOrderByCreatedAtDesc(DocumentDomain.AI_RAG);
    }

    @EntityGraph(attributePaths = {"chunks", "chunks.vector"})
    List<DocumentEntity> findByDocumentDomainAndCurrentStageInOrderByUpdatedAtDesc(
            DocumentDomain documentDomain,
            Collection<DocumentStage> currentStages
    );

    @EntityGraph(attributePaths = {"chunks", "chunks.vector", "createdBy"})
    Optional<DocumentEntity> findFirstByFilePathAndDocumentDomainOrderByCreatedAtDesc(String filePath, DocumentDomain documentDomain);

    @Query(value = "select count(*) from document d where d.document_domain = 'AI_RAG' and d.current_stage = :currentStage", nativeQuery = true)
    long countAiRagByCurrentStage(@Param("currentStage") String currentStage);

    @Query(value = "select count(*) from document d where d.document_domain = 'AI_RAG' and d.current_stage in (:currentStages)", nativeQuery = true)
    long countAiRagByCurrentStageIn(@Param("currentStages") Collection<String> currentStages);
}
