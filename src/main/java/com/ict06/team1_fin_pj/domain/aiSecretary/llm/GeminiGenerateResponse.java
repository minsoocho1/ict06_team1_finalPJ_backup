/**
 * @FileName : GeminiGenerateResponse.java
 * @Description : Gemini API 응답 본문 매핑 DTO
 *                - Gemini generateContent API 응답 데이터 구조 정의
 *                - candidates, content, parts 등 응답 텍스트 추출에 필요한 필드 매핑
 *                - GeminiModelClient에서 AI 응답 결과 파싱 시 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.28  송혜진       Gemini 응답 DTO 구조 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.llm;

import java.util.List;

/**
 * Gemini generateContent API 응답을 매핑하는 DTO.
 *
 * 주요 역할
 * - Gemini API 응답의 candidates/content/parts 구조를 표현한다.
 * - AI가 생성한 최종 텍스트를 추출하기 위한 응답 객체로 사용된다.
 * - GeminiModelClient에서 외부 API 응답을 Java 객체로 역직렬화할 때 사용된다.
 *
 * 주의 사항
 * - 응답 후보가 없거나 parts가 비어 있을 수 있으므로 호출부에서 null/empty 방어가 필요하다.
 * - Gemini API 응답 스펙 변경 시 필드 구조를 함께 점검해야 한다.
 */
public record GeminiGenerateResponse(
        List<Candidate> candidates
) {
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Candidate candidate = candidates.get(0);

        if (candidate.content() == null ||
                candidate.content().parts() == null ||
                candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content().parts().get(0).text();
    }

    public record Candidate(
            Content content
    ) {
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