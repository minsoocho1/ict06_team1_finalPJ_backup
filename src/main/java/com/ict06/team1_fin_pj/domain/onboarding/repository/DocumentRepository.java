/**
 * @FileName : DocumentRepository.java
 * @Description : 관리자 온보딩 문서/RAG 데이터 조회 Repository
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        최초 생성 및 문서 목록/상세 조회 시 부서, 등록자, 청크, 벡터 즉시 조회 처리 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Integer> {

    @EntityGraph(attributePaths = {"department", "createdBy", "chunks", "chunks.vector"})
    List<DocumentEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"department", "createdBy", "chunks", "chunks.vector"})
    Optional<DocumentEntity> findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(String title);

    @EntityGraph(attributePaths = {"department", "createdBy", "chunks", "chunks.vector"})
    Optional<DocumentEntity> findFirstByFilePathOrderByCreatedAtDesc(String filePath);

    @Override
    @EntityGraph(attributePaths = {"department", "createdBy", "chunks", "chunks.vector"})
    Optional<DocumentEntity> findById(Integer docId);
}
