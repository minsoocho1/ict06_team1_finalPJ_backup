package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEmbeddingResponseDto {

    @Builder.Default
    private List<Double> embedding = new ArrayList<>();

    private String modelName;

    private Integer dimension;
}
