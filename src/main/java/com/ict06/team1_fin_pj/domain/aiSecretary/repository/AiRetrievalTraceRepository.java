package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiRetrievalTraceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRetrievalTraceRepository extends JpaRepository<AiRetrievalTraceEntity, Integer> {

    @EntityGraph(attributePaths = {"document", "log", "log.message"})
    List<AiRetrievalTraceEntity> findByLog_LogIdInAndUsedInAnswerTrueOrderByTraceIdAsc(List<Integer> logIds);
}
