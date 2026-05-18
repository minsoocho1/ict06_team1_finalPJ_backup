package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TemplateRequestCreateRequestDto {

    @NotBlank(message = "사원번호는 필수입니다.")
    private String empNo;

    @NotBlank(message = "문서 유형은 필수입니다.")
    private String type; // REPORT / MINUTES / APPROVAL

    private String category;

    private String dept;

    private String situation;

    private String tone;

    @NotBlank(message = "템플릿 제목은 필수입니다.")
    private String title;

    private String description;

    @NotBlank(message = "템플릿 내용은 필수입니다.")
    private String content;

    private List<String> preview;

    private Boolean includeTitle;

    private Boolean includeParagraphs;

    private Boolean includeSignature;
}