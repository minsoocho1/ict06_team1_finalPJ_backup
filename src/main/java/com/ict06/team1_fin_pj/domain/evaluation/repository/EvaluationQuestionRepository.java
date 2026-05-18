/**
 * @FileName : EvaluationQuestionRepository.java
 * @Description : 온보딩 평가 문제 Repository
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자         수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.13    김다솜         카테고리/콘텐츠 기준 문제 조회 및 콘텐츠별 문제 일괄 삭제 메서드 정리
 */
package com.ict06.team1_fin_pj.domain.evaluation.repository;

import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationQuestionRepository extends JpaRepository<QuizQuestionEntity, Integer> {

    // 카테고리 기준 문제 조회
    List<QuizQuestionEntity> findByCategoryName(String categoryName);

    // 콘텐츠 기준 문제 조회
    List<QuizQuestionEntity> findByContent_ContentId(Integer contentId);

    // 콘텐츠 기준 기존 문제 삭제
    void deleteByContent_ContentId(Integer contentId);
}
