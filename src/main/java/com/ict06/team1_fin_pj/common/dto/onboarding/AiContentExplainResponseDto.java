/**
 * @FileName : AiContentExplainResponseDto.java
 * @Description : 학습 콘텐츠 AI 요약/재설명 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 15
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.15    김다솜        최초 생성
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiContentExplainResponseDto {

    private String mode;
    private String sourceTitle;
    private String answer;
    private Integer usedChunkCount;
    private List<String> usedChunks;
}
