package com.ict06.team1_fin_pj.domain.aiSecretary.repository;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface AiKnowledgeRequestRepository extends JpaRepository<AiKnowledgeRequestEntity, Long> {

    @EntityGraph(attributePaths = {"requester", "reviewer", "targetDoc"})
    List<AiKnowledgeRequestEntity> findByRequester_EmpNoOrderByCreatedAtDesc(String empNo);

    @EntityGraph(attributePaths = {"requester", "reviewer", "targetDoc"})
    List<AiKnowledgeRequestEntity> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"requester", "reviewer", "targetDoc"})
    @Query("select k from AiKnowledgeRequestEntity k left join fetch k.targetDoc where k.requestId = :requestId")
    java.util.Optional<AiKnowledgeRequestEntity> findByIdForUpdate(@Param("requestId") Long requestId);

    @EntityGraph(attributePaths = {"targetDoc"})
    List<AiKnowledgeRequestEntity> findByTargetDoc_DocIdInOrderByCreatedAtDesc(List<Integer> docIds);

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
