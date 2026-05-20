/**
 * @FileName : OnContentRepository.java
 * @Description : 온보딩 콘텐츠 Repository
 * @Author : 김다솜
 * @Date : 2026. 05. 08
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        관리자 콘텐츠 목록/상세 조회 시 대상 직급/부서 즉시 조회 처리 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnContentRepository extends JpaRepository<OnContentEntity, Integer> {

    @Override
    @EntityGraph(attributePaths = {"targetPositions", "targetDepartments"})
    List<OnContentEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"targetPositions", "targetDepartments"})
    Optional<OnContentEntity> findById(Integer contentId);

    @EntityGraph(attributePaths = {"targetPositions", "targetDepartments"})
    Optional<OnContentEntity> findFirstByPath(String path);

    @EntityGraph(attributePaths = {"targetPositions", "targetDepartments"})
    Optional<OnContentEntity> findFirstByTitleIgnoreCase(String title);
}
