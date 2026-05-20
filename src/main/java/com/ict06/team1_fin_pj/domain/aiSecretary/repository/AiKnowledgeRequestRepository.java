package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiKnowledgeRequestRepository extends JpaRepository<AiKnowledgeRequestEntity, Long> {

    @EntityGraph(attributePaths = {"requester", "reviewer", "targetDoc"})
    List<AiKnowledgeRequestEntity> findByRequester_EmpNoOrderByCreatedAtDesc(String empNo);

    @EntityGraph(attributePaths = {"requester", "reviewer", "targetDoc"})
    List<AiKnowledgeRequestEntity> findAllByOrderByCreatedAtDesc();

    @Query("""
            select distinct trim(k.requestType)
            from AiKnowledgeRequestEntity k
            where k.requestType is not null
              and trim(k.requestType) <> ''
            order by trim(k.requestType)
            """)
    List<String> findDistinctRequestTypes();

    @Query("""
            select distinct trim(k.category)
            from AiKnowledgeRequestEntity k
            where k.category is not null
              and trim(k.category) <> ''
            order by trim(k.category)
            """)
    List<String> findDistinctCategories();
}
