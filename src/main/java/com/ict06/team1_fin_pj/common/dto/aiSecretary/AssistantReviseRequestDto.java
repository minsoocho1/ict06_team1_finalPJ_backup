package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AssistantReviseRequestDto {

    @NotNull(message = "sessionId는 필수입니다.")
    private Integer sessionId;

    // REPORT / minutes / approval
    @NotBlank(message = "문서 유형은 필수입니다.")
    private String type;

    @NotBlank(message = "문서 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "현재 문서 내용은 필수입니다.")
    private String currentContent;

    @NotBlank(message = "수정 요청 내용은 필수입니다.")
    private String instruction;
}