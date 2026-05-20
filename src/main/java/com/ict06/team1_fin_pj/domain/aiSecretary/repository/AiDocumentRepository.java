package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AiDocumentRepository extends JpaRepository<DocumentEntity, Integer> {

    @Query(value = "select count(*) from document d where d.current_stage = :currentStage", nativeQuery = true)
    long countByCurrentStage(@Param("currentStage") String currentStage);

    @Query(value = "select count(*) from document d where d.current_stage in (:currentStages)", nativeQuery = true)
    long countByCurrentStageIn(@Param("currentStages") Collection<String> currentStages);

    @Query(value = """
            select d.doc_id, d.title, d.file_path, d.summary_preview, d.dept_id, d.access_level, d.current_stage, d.created_by, d.created_at, d.updated_at
            from document d
            where d.document_domain = 'AI_RAG'
            order by d.created_at desc
            """, nativeQuery = true)
    List<DocumentEntity> findAiRagDocuments();

    @Query(value = "select count(*) from document d where d.document_domain = 'AI_RAG' and d.current_stage = :currentStage", nativeQuery = true)
    long countAiRagByCurrentStage(@Param("currentStage") String currentStage);

    @Query(value = "select count(*) from document d where d.document_domain = 'AI_RAG' and d.current_stage in (:currentStages)", nativeQuery = true)
    long countAiRagByCurrentStageIn(@Param("currentStages") Collection<String> currentStages);

    List<DocumentEntity> findAllByOrderByCreatedAtDesc();
}
