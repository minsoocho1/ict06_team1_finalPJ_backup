/**
 * @FileName : AiDocumentProcessResponseDto.java
 * @Description : AI 서버 문서 처리 결과 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        문서 처리 결과 응답 구조 정의
 * @ 2026.05.18    김다솜        재처리 응답 문서 식별자 검증용 docId 필드 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiDocumentProcessResponseDto {

    private Integer docId;
    private String status;
    private String sourceType;
    private String extractedTextPreview;
    private Integer chunkCount;
    private Integer vectorCount;
    private String embeddingModel;
    private List<AiDocumentChunkResponseDto> chunks = new ArrayList<>();
}
