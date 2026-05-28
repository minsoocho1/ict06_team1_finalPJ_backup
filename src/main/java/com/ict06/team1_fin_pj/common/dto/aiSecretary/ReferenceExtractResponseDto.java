package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReferenceExtractResponseDto {

    // 업로드한 원본 파일명
    private String fileName;

    // 브라우저가 전달한 content-type
    private String contentType;

    // AI 프롬프트에 넣을 추출 본문
    private String extractedText;

    // 실제 반환한 본문 길이
    private int textLength;

    // 길이 제한으로 잘렸는지 여부
    private boolean truncated;

    // 화면에 보여줄 처리 결과 메시지
    private String message;
}
