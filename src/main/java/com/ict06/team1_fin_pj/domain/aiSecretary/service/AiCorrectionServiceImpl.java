/**
 * @FileName : AiCorrectionServiceImpl.java
 * @Description : AI 비서 문장 다듬기 Service 구현체
 *                - 사용자가 입력한 문장을 목적과 톤에 맞게 교정
 *                - 공손함, 논리성, 간결성 등 선택 조건 기반 문장 개선
 *                - Gemini 모델 호출을 통한 교정 결과 생성
 *                - 교정 전/후 문장 비교 및 응답 DTO 반환 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.12    송혜진       문장 다듬기 API 및 Gemini 호출 흐름 정리
 * @ 2026.05.28    송혜진       AI 비서 문장 교정 응답 구조 및 fallback 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.CorrectionResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogType;
import com.ict06.team1_fin_pj.domain.aiSecretary.llm.AiModelClient;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiLogRepository;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCorrectionServiceImpl implements AiCorrectionService {

    private final AiModelClient aiModelClient;
    private final AiLogRepository aiLogRepository;
    private final EmpRepository empRepository;

    @Override
    @Transactional
    public CorrectionResponseDto correct(String empNo, String text, String mode) {

        long startTime = System.currentTimeMillis();

        String normalizedMode = normalizeMode(mode);
        String prompt = buildPrompt(text, normalizedMode);

        boolean fallback = false;
        boolean providerSuccess = false;
        String errorMessage = null;
        String correctedText;

        try {
            correctedText = aiModelClient.generateAnswer(prompt);
            providerSuccess = true;
        } catch (Exception e) {
            log.warn("[CORRECTION] Gemini 문장 다듬기 실패. fallback으로 대체합니다. reason={}", e.getMessage());

            correctedText = buildFallbackText(text);
            fallback = true;
            errorMessage = e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        saveCorrectionLog(
                empNo,
                normalizedMode,
                text,
                correctedText,
                providerSuccess,
                fallback,
                durationMs,
                errorMessage
        );

        return CorrectionResponseDto.builder()
                .mode(normalizedMode)
                .correctedText(correctedText)
                .fallback(fallback)
                .build();
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "BASIC";
        }

        return mode.trim().toUpperCase();
    }

    private String buildPrompt(String text, String mode) {
        String instruction = switch (mode) {
            case "BUSINESS_POLITE" -> "아래 문장을 업무 메신저나 이메일에 적합한 공손한 말투로 다듬어 주세요.";
            case "CONCISE" -> "아래 문장을 의미는 유지하되 더 간결하게 정리해 주세요.";
            case "LOGICAL" -> "아래 문장을 논리적이고 명확한 구조로 정리해 주세요.";
            case "FRIENDLY" -> "아래 문장을 부드럽고 친근한 말투로 다듬어 주세요.";
            default -> "아래 문장의 맞춤법, 띄어쓰기, 어색한 표현을 자연스럽게 교정해 주세요.";
        };

        return """
                당신은 사내 그룹웨어의 AI 문장 다듬기 도우미입니다.

                작업 지시:
                %s

                규칙:
                1. 한국어로만 답변하세요.
                2. 원문의 의미를 왜곡하지 마세요.
                3. 불필요한 설명 없이 다듬은 문장만 출력하세요.
                4. 개인정보, 계정, 비밀번호, API Key처럼 보이는 내용은 임의로 보완하거나 추측하지 마세요.
                5. 너무 과장된 표현은 피하고 업무 상황에 맞게 자연스럽게 작성하세요.

                원문:
                %s
                """.formatted(instruction, text);
    }

    private String buildFallbackText(String text) {
        return """
                현재 AI 문장 다듬기 요청이 일시적으로 원활하지 않습니다.
                아래 원문을 다시 확인해 주세요.

                %s
                """.formatted(text);
    }

    private void saveCorrectionLog(
            String empNo,
            String mode,
            String originalText,
            String correctedText,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    ) {
        try {
            EmpEntity employee = empRepository.findByEmpNo(empNo).orElse(null);

            /**
             * AI_LOG에는 원문 전체를 장기 저장하지 않고 메타 정보 위주로 저장한다.
             * 단, 현재 ai_log 구조상 query/response가 text이므로
             * 기능 구분과 길이 정보만 남긴다.
             */
            String queryMeta = "feature=CORRECTION, mode=%s, inputLength=%d"
                    .formatted(mode, originalText == null ? 0 : originalText.length());

            String responseMeta = "outputLength=%d, providerSuccess=%s, fallback=%s"
                    .formatted(correctedText == null ? 0 : correctedText.length(), providerSuccess, fallback);

            AiLogEntity log = AiLogEntity.builder()
                    .employee(employee)
                    .type(AiLogType.ASSISTANT)
                    .query(queryMeta)
                    .response(responseMeta)
                    .durationMs((int) durationMs)
                    .success(true)
                    .errorMessage(trimErrorMessage(errorMessage))
                    .build();

            aiLogRepository.save(log);
        } catch (Exception e) {
            log.warn("[AI_LOG] 문장 다듬기 로그 저장 실패. reason={}", e.getMessage());
        }
    }

    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        if (errorMessage.length() <= 1000) {
            return errorMessage;
        }

        return errorMessage.substring(0, 1000);
    }
}