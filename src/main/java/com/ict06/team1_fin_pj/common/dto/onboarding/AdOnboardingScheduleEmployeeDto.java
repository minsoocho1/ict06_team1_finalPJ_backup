/**
 * @FileName : AdOnboardingScheduleEmployeeDto.java
 * @Description : 관리자 온보딩 일정 직원별 요약 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        최초 생성 및 직원별 온보딩 일정 요약 표시값 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdOnboardingScheduleEmployeeDto {

    private String empNo;
    private String employeeName;
    private String roadmapTitle;
    private int totalCount;
    private int completedCount;
    private int inProgressCount;
    private int notStartedCount;
    private int progressRate;
}
