/**
 * @FileName : LearningSelfCheckRepository.java
 * @Description : 학습 이해도 자기 평가 조회 및 저장 Repository
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정내용
 * @ ----------    -----------------------------------------------
 * @ 2026.05.18    직원과 콘텐츠 기준 자기 평가 조회 기능 추가
 */
/*
 * 2026-05-19
 * 평가 결과 화면 자기평가 AI 피드백 조회를 위한 카테고리별 조회 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.repository;

import com.ict06.team1_fin_pj.domain.onboarding.entity.LearningSelfCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningSelfCheckRepository extends JpaRepository<LearningSelfCheckEntity, Integer> {

    Optional<LearningSelfCheckEntity> findByEmployee_EmpNoAndContent_ContentId(String empNo, Integer contentId);

    List<LearningSelfCheckEntity> findByEmployee_EmpNoAndContent_CategoryOrderByCheckedAtDesc(String empNo, String category);
}
