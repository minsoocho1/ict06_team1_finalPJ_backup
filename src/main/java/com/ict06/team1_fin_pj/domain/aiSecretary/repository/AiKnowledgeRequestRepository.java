/**
 * @FileName : AiKnowledgeRequestRepository.java
 * @Description : AI 챗봇 자료 등록 요청 Repository
 *                - 사용자가 요청한 챗봇/RAG 자료 등록 요청 조회 및 저장
 *                - 관리자 자료 등록 요청 상세 조회
 *                - 문서 권한 조건, 요청 사유, 참고 URL 관리에 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.27  송혜진       관리자 RAG 자료 등록 요청 상세 관리 기준 반영
 */

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
