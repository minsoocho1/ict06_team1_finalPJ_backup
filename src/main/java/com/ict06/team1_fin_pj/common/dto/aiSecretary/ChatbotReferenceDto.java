package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotReferenceDto {

    private Integer docId;

    private String title;

    private String url;
}
