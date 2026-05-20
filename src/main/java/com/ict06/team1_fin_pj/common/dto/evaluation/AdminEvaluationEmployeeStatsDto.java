/**
 * @FileName : AdminEvaluationEmployeeStatsDto.java
 * @Description : 관리자 평가 직원별 성과 통계 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 14
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    김다솜        직원별 평균 점수 및 통과 횟수 집계 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminEvaluationEmployeeStatsDto {

    private String empNo;
    private String employeeName;
    private String departmentName;
    private Integer evaluationCount;
    private Integer passCount;
    private Double averageScoreRate;
    private LocalDateTime latestSubmittedAt;
}
