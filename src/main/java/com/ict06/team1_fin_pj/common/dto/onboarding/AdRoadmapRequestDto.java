/**
 * @FileName : AdRoadmapRequestDto.java
 * @Description : 관리자 온보딩 로드맵 등록 및 수정 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        최초 생성 및 로드맵 제목 자동 생성 전환에 따른 요청값 정리
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import com.ict06.team1_fin_pj.domain.onboarding.entity.GeneratedType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdRoadmapRequestDto {

    private String empNo;
    private GeneratedType generatedType;
}
