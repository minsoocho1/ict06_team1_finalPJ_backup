/**
 * @FileName : AdRoadItemRequestDto.java
 * @Description : 관리자 온보딩 로드맵 아이템 등록 및 수정 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        최초 생성 및 로드맵 아이템 일정 시작일/마감일 요청값 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdRoadItemRequestDto {

    private Integer contentId;
    private String itemTitle;
    private String categoryName;
    private Integer orderNo;
    private LocalDate startDate;
    private LocalDate dueDate;
}
