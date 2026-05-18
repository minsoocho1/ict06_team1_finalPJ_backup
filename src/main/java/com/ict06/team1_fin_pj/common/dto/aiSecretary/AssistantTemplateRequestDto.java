package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 기본 생성자
public class AssistantTemplateRequestDto {

    @NotBlank(message = "사원번호는 필수입니다") // null 및 빈값 미허용
    private String empNo;

    private String type; // REPORT / MINUTES / APPROVAL

    private String category;

    private String dept;

    private String situation;

    private String tone; // 톤앤매너

    private Boolean includeTitle; // 제목 포함 여부

    private Boolean includeParagraphs; // 기본 문단 포함여부

    private Boolean includeSignature; // 서명 포함여부


}
