/**
 * @FileName : AiDocumentRepository.java
 * @Description : AI/RAG 문서 조회 및 상태 관리를 위한 DOCUMENT Repository
 * @Author : 송혜진
 * @Date : 2026. 05. 20
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.05.20    송혜진        최초 생성 (AI_RAG 문서 조회 및 관리자 RAG 관리 화면 연동)
 * @ 2026.05.22    송혜진        documentDomain 및 currentStage 기준 RAG 문서 조회 조건 정리
 * @ 2026.05.22    송혜진        PUBLISHED 상태 문서만 RAG 검색 대상으로 사용하도록 조회 기준 반영
 * @ 2026.05.22    송혜진        관리자 문서 활성화/비활성화 상태 전이에 필요한 문서 조회 기준 정리
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
