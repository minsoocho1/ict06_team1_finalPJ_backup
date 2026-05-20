package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnContentDetailResponseDto {

    private Integer contentId;
    private String title;
    private String type;
    private String category;
    private String subCategory;
    private String targetPosition;
    private String difficulty;
    private Integer estimatedTime;
    private Object tags;
    private Boolean isMandatory;
    private String path;
}
