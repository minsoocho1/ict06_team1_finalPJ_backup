/**
 * @FileName : LearningSelfCheckRequestDto.java
 * @Description : 학습 이해도 자기 평가 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        최초 생성
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearningSelfCheckRequestDto {

    private String empNo;
    private Integer understandingScore;
    private Integer confidenceScore;
    private Boolean needMoreExplanation;
    private String memo;
}
