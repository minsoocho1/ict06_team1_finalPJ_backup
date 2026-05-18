package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface AiDocumentRepository extends JpaRepository<DocumentEntity, Integer> {

    @Query(value = "select count(*) from document d where d.current_stage = :currentStage", nativeQuery = true)
    long countByCurrentStage(@Param("currentStage") String currentStage);

    @Query(value = "select count(*) from document d where d.current_stage in (:currentStages)", nativeQuery = true)
    long countByCurrentStageIn(@Param("currentStages") Collection<String> currentStages);
}
