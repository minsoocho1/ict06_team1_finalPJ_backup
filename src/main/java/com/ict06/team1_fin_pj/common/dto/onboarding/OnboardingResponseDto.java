/**
 * @FileName : OnboardingResponseDto.java
 * @Description : 사용자 온보딩 로드맵 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        온보딩 응답 DTO를 common/dto/onboarding 패키지로 이동
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import com.ict06.team1_fin_pj.common.dto.onboarding.RoadmapItemDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingResponseDto {
    private Integer roadmapId;
    private String title;
    private List<RoadmapItemDto> items;
}
