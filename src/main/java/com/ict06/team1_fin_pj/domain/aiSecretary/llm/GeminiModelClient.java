/**
 * @FileName : GeminiModelClient.java
 * @Description : Gemini API 연동 클라이언트
 *                - AI 비서, 챗봇, 문장 다듬기 기능에서 사용하는 외부 LLM 호출 처리
 *                - Gemini generateContent API 요청/응답 처리
 *                - prompt 전달, 응답 텍스트 추출, API 오류 처리 담당
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.04.28  송혜진       최초 생성
 * @ 2026.05.12  송혜진       Gemini API 호출 및 응답 로그 보강
 * @ 2026.05.28  송혜진       AI 비서 문서 작성 및 챗봇 호출 흐름 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiModelClient implements AiModelClient {

    private final GeminiProperties geminiProperties;

    private final RestClient restClient = RestClient.create();

    @Override
    public String generateAnswer(String prompt) {

        String apiKey = geminiProperties.getApiKey();
        String model = geminiProperties.getModel();
        String baseUrl = geminiProperties.getBaseUrl();

        validateGeminiConfig(apiKey, model, baseUrl);

        String url = "%s/models/%s:generateContent?key=%s".formatted(
                baseUrl,
                model,
                apiKey
        );

        log.info("[GEMINI] request model={}", model);
        log.debug("[GEMINI] apiKeyFingerprint={}", maskApiKey(apiKey));

        GeminiGenerateRequest request = GeminiGenerateRequest.of(prompt);

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            if (response == null) {
                throw new IllegalStateException("Gemini 응답이 비어 있습니다.");
            }

            String answer = response.extractText();

            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("Gemini 응답에서 텍스트를 추출하지 못했습니다.");
            }

            return answer.trim();

        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();

            log.warn(
                    "[GEMINI] API 호출 실패. status={}, model={}, responseBody={}",
                    statusCode,
                    model,
                    abbreviate(responseBody, 1000)
            );

            if (statusCode == 429) {
                throw new IllegalStateException(
                        "Gemini API 할당량을 초과했습니다. 잠시 후 다시 시도하거나 다른 Gemini API Key를 사용하세요."
                );
            }

            if (statusCode == 401 || statusCode == 403) {
                throw new IllegalStateException(
                        "Gemini API 인증에 실패했습니다. API Key 또는 프로젝트 권한을 확인하세요."
                );
            }

            if (statusCode == 503) {
                throw new IllegalStateException(
                        "Gemini API가 일시적으로 응답하지 않습니다. 다른 모델로 전환하거나 잠시 후 다시 시도하세요. status=503"
                );
            }

            throw new IllegalStateException("Gemini API 호출 실패. status=" + statusCode);

        } catch (Exception e) {
            log.warn("[GEMINI] 응답 생성 중 예외 발생. reason={}", e.getMessage());
            throw new IllegalStateException("Gemini 응답 생성 실패: " + e.getMessage());
        }
    }

    private void validateGeminiConfig(String apiKey, String model, String baseUrl) {

        if (apiKey == null || apiKey.isBlank() || apiKey.contains("${")) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY가 설정되지 않았습니다. ai_server/env 또는 spring.config.import 설정을 확인하세요."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException("gemini.model 설정이 비어 있습니다.");
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("gemini.base-url 설정이 비어 있습니다.");
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return "INVALID_KEY";
        }

        return apiKey.substring(0, 6) + "..." + apiKey.substring(apiKey.length() - 6);
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}