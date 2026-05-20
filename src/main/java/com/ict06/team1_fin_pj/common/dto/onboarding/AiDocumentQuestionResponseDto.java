package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiDocumentQuestionResponseDto {

    private String answer;
    private Integer usedChunkCount;
    private List<String> usedChunks = new ArrayList<>();
}
