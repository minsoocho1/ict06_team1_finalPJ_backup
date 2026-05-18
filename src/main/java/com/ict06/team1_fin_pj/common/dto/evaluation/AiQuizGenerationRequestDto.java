/**
 * @FileName : AiQuizGenerationRequestDto.java
 * @Description : AI 서버 퀴즈 자동 생성 요청 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 11
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜        콘텐츠 메타데이터 기반 AI 퀴즈 생성 요청 DTO 추가
 */
package com.ict06.team1_fin_pj.common.dto.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiQuizGenerationRequestDto {

    @JsonProperty("content_id")
    private Integer contentId;

    private String title;

    private String category;

    @JsonProperty("sub_category")
    private String subCategory;

    @JsonProperty("content_type")
    private String contentType;

    private String difficulty;

    @JsonProperty("question_count")
    private Integer questionCount;

    private String tags;

    private String path;

    @JsonProperty("rag_summary")
    private String ragSummary;

    @JsonProperty("rag_context")
    private String ragContext;
}
