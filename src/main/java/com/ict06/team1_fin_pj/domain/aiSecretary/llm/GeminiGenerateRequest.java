/**
 * @FileName : GeminiGenerateRequest.java
 * @Description : Gemini API 요청 본문 매핑 DTO
 *                - Gemini generateContent API 호출 시 전달할 요청 데이터 구조 정의
 *                - 사용자 prompt, contents, parts 등 Gemini 요청 형식 매핑
 *                - GeminiModelClient에서 외부 AI 모델 호출 시 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.28  송혜진       Gemini 요청 DTO 구조 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.llm;

import java.util.List;

/**
 * Gemini generateContent API 호출에 사용되는 요청 DTO.
 *
 * 주요 역할
 * - Gemini API가 요구하는 contents/parts 기반 요청 구조를 표현한다.
 * - AI 비서, 챗봇, 문장 다듬기 등에서 생성한 prompt를 Gemini 요청 형식으로 변환한다.
 * - 외부 API 요청 본문 직렬화에 사용된다.
 *
 * 주의 사항
 * - 비즈니스 로직은 포함하지 않는다.
 * - Gemini API 스펙 변경 시 이 DTO 구조도 함께 점검해야 한다.
 */
public record GeminiGenerateRequest(
        List<Content> contents
) {
    public static GeminiGenerateRequest of(String prompt) {
        return new GeminiGenerateRequest(
                List.of(
                        new Content(
                                List.of(new Part(prompt))
                        )
                )
        );
    }

    public record Content(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }
}

/* Gemini REST 요청 구조
{
  "contents": [
    {
      "parts": [
        {
          "text": "질문"
        }
      ]
    }
  ]
}
* */