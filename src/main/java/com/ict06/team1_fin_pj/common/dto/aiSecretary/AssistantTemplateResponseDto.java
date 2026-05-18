package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssistantTemplateResponseDto {

    private String type;

    private String category;

    private String dept;

    private String situation;

    private String tone;

    private String title;

    private String description;

    private List<String> preview;

    private String content;

    private String modelName;

    private Boolean fallback;
}
