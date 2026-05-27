package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentActivationResultDto {

    private boolean success;
    private String message;
    private String stage;
    private String stageLabel;
}
