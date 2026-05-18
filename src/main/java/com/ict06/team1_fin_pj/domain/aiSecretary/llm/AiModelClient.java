package com.ict06.team1_fin_pj.domain.aiSecretary.llm;

public interface AiModelClient {

    // AI 모델에 프롬프트를 전달하고 응답 텍스트를 반환 (GeminiModelClient)
    String generateAnswer(String prompt);
}