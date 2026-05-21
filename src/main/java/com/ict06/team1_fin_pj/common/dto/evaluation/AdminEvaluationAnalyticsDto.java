/**
 * @FileName : AdminEvaluationAnalyticsDto.java
 * @Description : 관리자 평가 통계 화면 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 14
 * @Modification_History
 *
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    김다솜        평가 점수 통계, 이해도 시각화 및 가중 평균 이해도 점수 필드 추가
 * @ 2026.05.18    김다솜        이탈 징후 분석, LLM 개선 인사이트 및 복합 통계 요약 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminEvaluationAnalyticsDto {

    private Integer submissionCount;
    private Integer participantCount;
    private Double averageScoreRate;
    private Double weightedAverageScoreRate;
    private Double passRate;
    private Integer highRiskEmployeeCount;
    private Integer mediumRiskEmployeeCount;
    private Double averageLearningProgressRate;
    private Double averageChecklistProgressRate;
    private String retentionAiInsight;
    private List<AdminEvaluationCategoryStatsDto> categoryStats;
    private List<AdminEvaluationQuestionStatsDto> questionStats;
    private List<AdminEvaluationEmployeeStatsDto> employeeStats;
    private List<AdminRetentionRiskDto> retentionRiskStats;
}
