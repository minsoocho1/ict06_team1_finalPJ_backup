package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentQuestionRequestDto {

    private String title;
    private String question;
    private String summaryPreview;

    @Builder.Default
    private List<String> chunks = new ArrayList<>();
}
