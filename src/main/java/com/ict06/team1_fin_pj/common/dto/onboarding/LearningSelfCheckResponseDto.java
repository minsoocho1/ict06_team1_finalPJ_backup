/**
 * @FileName : LearningSelfCheckResponseDto.java
 * @Description : 학습 이해도 자기 평가 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        최초 생성, 자기 평가 제출 여부 및 AI 평가 비교 응답 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSelfCheckResponseDto {

    private Boolean submitted;
    private Integer selfCheckId;
    private Integer contentId;
    private String empNo;
    private Integer understandingScore;
    private Integer confidenceScore;
    private Boolean needMoreExplanation;
    private String memo;
    private LocalDateTime checkedAt;
    private Double selfScoreRate;
    private Double evaluationScoreRate;
    private Double scoreGap;
    private String comparisonType;
    private String feedback;
}
