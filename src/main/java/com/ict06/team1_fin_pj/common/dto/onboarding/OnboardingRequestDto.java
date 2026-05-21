/**
 * @FileName : OnboardingRequestDto.java
 * @Description : 사용자 온보딩 로드맵 생성 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        온보딩 요청 DTO를 common/dto/onboarding 패키지로 이동
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingRequestDto {

    private String empNo;
    private String empId;
    private String deptName;
    private String positionName;
}
