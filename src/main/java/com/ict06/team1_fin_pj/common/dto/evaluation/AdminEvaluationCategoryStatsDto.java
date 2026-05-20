/**
 * @FileName : AdminEvaluationCategoryStatsDto.java
 * @Description : 관리자 평가 카테고리별 통계 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 14
 * @Modification_History
 *
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    김다솜        카테고리별 평균 점수, 기준점, 가중치 비교 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminEvaluationCategoryStatsDto {

    private String categoryName;
    private Integer submissionCount;
    private Integer questionCount;
    private Integer totalScore;
    private Integer passScore;
    private Integer requiredScore;
    private Integer weightPercent;
    private Double averageScoreRate;
    private Double weightedAverageScoreRate;
    private Double passRate;
    private Double scoreGap;
}
