package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KnowledgeRequestCreateDto {

    @NotBlank(message = "요청자 정보는 필수입니다.")
    private String requesterNo;

    @NotBlank(message = "문서명 또는 요청 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "자료 유형은 필수입니다.")
    private String requestType;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    private String targetDept;

    @NotBlank(message = "요청 사유는 필수입니다.")
    private String reason;

    @NotBlank(message = "챗봇 질문 예시는 필수입니다.")
    private String sampleQuestion;

    private String referenceUrl;

    @NotBlank(message = "기본 열람 권한은 필수입니다.")
    private String accessLevel;
}
