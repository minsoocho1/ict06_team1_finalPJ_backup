/**
 * @FileName : GeminiProperties.java
 * @Description : Gemini API 설정 프로퍼티 클래스
 *                - Gemini API baseUrl, model, apiKey 등 외부 AI 연동 설정 관리
 *                - application.yml 및 외부 환경변수 설정값 바인딩
 *                - GeminiModelClient에서 API 호출 설정값으로 사용
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.12  송혜진       Gemini API 설정값 외부 환경변수 연동
 * @ 2026.05.28  송혜진       Gemini 모델 설정 구조 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;

    private String model;

    private String baseUrl;
}