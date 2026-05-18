package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AssistantDraftRequestDto {

    @NotBlank(message = "사원번호는 필수입니다.")
    private String empNo;

    @NotBlank(message = "문서 유형은 필수입니다.")
    private String type; // REPORT / MINUTES / approval

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String purpose;

    private String audience;

    private List<String> targets;

    private String detail;

    private String amount;

    private String tone;
}