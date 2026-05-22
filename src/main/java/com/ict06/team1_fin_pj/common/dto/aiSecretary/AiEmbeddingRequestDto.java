package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEmbeddingRequestDto {

    @NotBlank(message = "text is required")
    private String text;
}
