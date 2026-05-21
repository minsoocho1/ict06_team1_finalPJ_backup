/**
 * @FileName : AdminRetentionRiskDto.java
 * @Description : 관리자 이탈 징후 분석 및 개선 추천 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        이탈 위험도, 위험 사유, 개선 추천 응답 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminRetentionRiskDto {

    private String empNo;
    private String employeeName;
    private String departmentName;
    private String riskLevel;
    private Integer riskScore;
    private Double learningProgressRate;
    private Double checklistProgressRate;
    private Double evaluationAverageScoreRate;
    private Double evaluationPassRate;
    private Integer evaluationCount;
    private LocalDateTime latestSubmittedAt;
    private List<String> riskReasons;
    private List<String> recommendations;
}
