/**
 * @FileName : AdminEvaluationQuestionStatsDto.java
 * @Description : 관리자 평가 문항별 이해도 통계 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 14
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    김다솜        문항별 평균 점수율 집계 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminEvaluationQuestionStatsDto {

    private Integer questionId;
    private String categoryName;
    private String questionText;
    private Integer responseCount;
    private Double averageScoreRate;
}
