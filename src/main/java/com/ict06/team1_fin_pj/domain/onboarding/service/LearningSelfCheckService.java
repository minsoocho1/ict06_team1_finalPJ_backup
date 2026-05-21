/**
 * @FileName : LearningSelfCheckService.java
 * @Description : 학습 이해도 자기 평가 저장 및 AI 평가 점수 비교 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.18    김다솜        학습 이해도 자기 평가 저장 및 평가 결과 비교 피드백 추가
 * @ 2026.05.19    김다솜        평가 완료 직후와 평가 상세 화면에서 자기평가 비교 피드백 조회 추가
 * @ 2026.05.19    김다솜        콘텐츠명과 오답/감점 문항 기반 재학습 피드백 생성 추가
 * @ 2026.05.19    김다솜        서비스 용어 중심의 자기 평가 피드백 문구로 변경
 * @ 2026.05.19    김다솜        AI 서버 Groq 기반 자기 평가 피드백 생성 연동
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.LearningSelfCheckRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.LearningSelfCheckResponseDto;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizResultEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationResultRepository;
import com.ict06.team1_fin_pj.domain.onboarding.entity.LearningSelfCheckEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.LearningSelfCheckRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningSelfCheckService {

    private final LearningSelfCheckRepository learningSelfCheckRepository;
    private final OnContentRepository onContentRepository;
    private final EmpRepository empRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    @Transactional(readOnly = true)
    public LearningSelfCheckResponseDto getSelfCheck(Integer contentId, String empNo) {
        if (empNo == null || empNo.isBlank()) {
            throw new IllegalArgumentException("사번 정보가 필요합니다.");
        }

        OnContentEntity content = getContent(contentId);
        return learningSelfCheckRepository.findByEmployee_EmpNoAndContent_ContentId(empNo, contentId)
                .map(check -> buildResponse(check, content))
                .orElseGet(() -> buildEmptyResponse(contentId, empNo, content));
    }

    @Transactional
    public LearningSelfCheckResponseDto saveSelfCheck(Integer contentId, LearningSelfCheckRequestDto requestDto) {
        if (requestDto.getEmpNo() == null || requestDto.getEmpNo().isBlank()) {
            throw new IllegalArgumentException("사번 정보가 필요합니다.");
        }

        validateScore(requestDto.getUnderstandingScore(), "이해도 점수");
        validateScore(requestDto.getConfidenceScore(), "자신감 점수");

        OnContentEntity content = getContent(contentId);
        EmpEntity employee = empRepository.findByEmpNo(requestDto.getEmpNo())
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

        LearningSelfCheckEntity selfCheck = learningSelfCheckRepository
                .findByEmployee_EmpNoAndContent_ContentId(requestDto.getEmpNo(), contentId)
                .orElseGet(() -> LearningSelfCheckEntity.builder()
                        .employee(employee)
                        .content(content)
                        .understandingScore(requestDto.getUnderstandingScore())
                        .confidenceScore(requestDto.getConfidenceScore())
                        .needMoreExplanation(Boolean.TRUE.equals(requestDto.getNeedMoreExplanation()))
                        .memo(normalizeMemo(requestDto.getMemo()))
                        .build());

        selfCheck.update(
                requestDto.getUnderstandingScore(),
                requestDto.getConfidenceScore(),
                Boolean.TRUE.equals(requestDto.getNeedMoreExplanation()),
                normalizeMemo(requestDto.getMemo())
        );

        return buildResponse(learningSelfCheckRepository.save(selfCheck), content);
    }

    @Transactional(readOnly = true)
    public LearningSelfCheckResponseDto getLatestSelfCheckFeedbackByCategory(String empNo, String categoryName) {
        if (empNo == null || empNo.isBlank() || categoryName == null || categoryName.isBlank()) {
            return null;
        }

        return learningSelfCheckRepository
                .findByEmployee_EmpNoAndContent_CategoryOrderByCheckedAtDesc(empNo, categoryName)
                .stream()
                .findFirst()
                .map(check -> buildResponse(check, check.getContent()))
                .orElse(null);
    }

    private OnContentEntity getContent(Integer contentId) {
        return onContentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("학습 콘텐츠를 찾을 수 없습니다."));
    }

    private void validateScore(Integer score, String fieldName) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException(fieldName + "는 1에서 5 사이로 입력해야 합니다.");
        }
    }

    private LearningSelfCheckResponseDto buildEmptyResponse(Integer contentId, String empNo, OnContentEntity content) {
        Double evaluationScoreRate = calculateEvaluationScoreRate(empNo, content.getCategory());
        return LearningSelfCheckResponseDto.builder()
                .submitted(false)
                .contentId(contentId)
                .empNo(empNo)
                .evaluationScoreRate(evaluationScoreRate)
                .feedback(evaluationScoreRate == null
                        ? "배운 뒤 얼마나 알겠는지와 얼마나 자신 있는지 남기면, AI 평가와 함께 다시 볼 곳을 알려드립니다."
                        : "아직 자기 평가가 없습니다. 체크를 남기면 지금 AI 평가와 함께 다시 볼 곳을 알려드립니다.")
                .build();
    }

    private LearningSelfCheckResponseDto buildResponse(LearningSelfCheckEntity check, OnContentEntity content) {
        double selfScoreRate = roundOneDecimal(((check.getUnderstandingScore() + check.getConfidenceScore()) / 10.0) * 100.0);
        Double evaluationScoreRate = calculateEvaluationScoreRate(check.getEmployee().getEmpNo(), content.getCategory());
        Double scoreGap = evaluationScoreRate != null ? roundOneDecimal(evaluationScoreRate - selfScoreRate) : null;
        String comparisonType = resolveComparisonType(selfScoreRate, evaluationScoreRate);

        return LearningSelfCheckResponseDto.builder()
                .submitted(true)
                .selfCheckId(check.getSelfCheckId())
                .contentId(content.getContentId())
                .empNo(check.getEmployee().getEmpNo())
                .understandingScore(check.getUnderstandingScore())
                .confidenceScore(check.getConfidenceScore())
                .needMoreExplanation(Boolean.TRUE.equals(check.getNeedMoreExplanation()))
                .memo(check.getMemo())
                .checkedAt(check.getCheckedAt())
                .selfScoreRate(selfScoreRate)
                .evaluationScoreRate(evaluationScoreRate)
                .scoreGap(scoreGap)
                .comparisonType(comparisonType)
                .feedback(buildFeedback(comparisonType, check, content, selfScoreRate, evaluationScoreRate, scoreGap))
                .build();
    }

    private Double calculateEvaluationScoreRate(String empNo, String categoryName) {
        List<QuizResultEntity> results = findEvaluationResults(empNo, categoryName);
        if (results.isEmpty()) {
            return null;
        }

        int totalScore = results.stream().mapToInt(result -> result.getScore() != null ? result.getScore() : 0).sum();
        int maxScore = results.stream()
                .filter(result -> result.getQuestion() != null)
                .mapToInt(result -> result.getQuestion().getScore() != null ? result.getQuestion().getScore() : 0)
                .sum();

        if (maxScore <= 0) {
            return null;
        }

        return roundOneDecimal(totalScore * 100.0 / maxScore);
    }

    private String resolveComparisonType(double selfScoreRate, Double evaluationScoreRate) {
        if (evaluationScoreRate == null) {
            return "SELF_ONLY";
        }

        double gap = evaluationScoreRate - selfScoreRate;
        if (selfScoreRate >= 80.0 && gap <= -20.0) {
            return "OVERCONFIDENT";
        }
        if (selfScoreRate <= 60.0 && gap >= 20.0) {
            return "LOW_CONFIDENCE";
        }
        if (selfScoreRate < 60.0 && evaluationScoreRate < 70.0) {
            return "NEEDS_REVIEW";
        }
        if (selfScoreRate >= 80.0 && evaluationScoreRate >= 80.0) {
            return "READY";
        }
        return "BALANCED";
    }

    private String buildFeedback(
            String comparisonType,
            LearningSelfCheckEntity check,
            OnContentEntity content,
            double selfScoreRate,
            Double evaluationScoreRate,
            Double scoreGap
    ) {
        String contentTitle = content.getTitle() != null && !content.getTitle().isBlank()
                ? content.getTitle()
                : "해당 콘텐츠";
        List<QuizResultEntity> reviewTargets = findReviewTargets(check.getEmployee().getEmpNo(), content.getCategory());
        String reviewGuide = buildReviewGuide(reviewTargets, contentTitle);
        String scoreSummary = buildScoreSummary(selfScoreRate, evaluationScoreRate, scoreGap);

        String aiFeedback = generateAiFeedback(
                check,
                content,
                reviewTargets,
                selfScoreRate,
                evaluationScoreRate,
                scoreGap
        );
        if (aiFeedback != null && !aiFeedback.isBlank()) {
            return aiFeedback;
        }

        return buildRuleBasedFeedback(comparisonType, check, contentTitle, reviewGuide, scoreSummary);
    }

    private String buildRuleBasedFeedback(
            String comparisonType,
            LearningSelfCheckEntity check,
            String contentTitle,
            String reviewGuide,
            String scoreSummary
    ) {
        String feedback = switch (comparisonType) {
            case "OVERCONFIDENT" -> scoreSummary + " 자기 평가보다 AI 평가가 낮게 나왔습니다. " + contentTitle
                    + "에서 왜 그 답이 맞는지 말로 풀어 봐야 하는 곳을 다시 보세요. " + reviewGuide;
            case "LOW_CONFIDENCE" -> scoreSummary + " AI 평가는 괜찮지만 스스로 덜 믿고 있습니다. " + contentTitle
                    + "에서 핵심 개념을 본인 말로 세 줄 정리하고, 아래 내용을 다시 보면 자신감을 올릴 수 있습니다. " + reviewGuide;
            case "NEEDS_REVIEW" -> scoreSummary + " 자기 평가와 AI 평가가 둘 다 낮은 편입니다. " + contentTitle
                    + "를 바로 다시 보고, 틀렸거나 덜 맞은 문제부터 차근차근 살펴보세요. " + reviewGuide;
            case "READY" -> scoreSummary + " 자기 평가와 AI 평가가 둘 다 좋습니다. " + contentTitle
                    + "는 마무리해도 좋고, 아래 내용만 빠르게 다시 보면 다음 온보딩으로 넘어가기 좋습니다. " + reviewGuide;
            case "SELF_ONLY" -> contentTitle + " 자기 평가가 저장되었습니다. 퀴즈를 풀면 어느 부분을 다시 볼지 알려드립니다.";
            default -> Boolean.TRUE.equals(check.getNeedMoreExplanation())
                    ? scoreSummary + " 자기 평가와 AI 평가가 크게 다르지는 않지만 더 알고 싶은 곳이 있다고 남겼습니다. "
                    + contentTitle + "에서 헷갈린 곳을 AI 학습 도우미에 물어보고, " + reviewGuide
                    : scoreSummary + " 자기 평가와 AI 평가가 크게 다르지 않습니다. " + contentTitle
                    + "를 지금 흐름대로 이어가되, " + reviewGuide;
        };

        if (Boolean.TRUE.equals(check.getNeedMoreExplanation()) && !"BALANCED".equals(comparisonType)) {
            feedback += " 추가 설명이 필요하다고 표시했으니, 위 내용을 그대로 AI 학습 도우미에 질문해 보세요.";
        }

        return feedback;
    }

    private String generateAiFeedback(
            LearningSelfCheckEntity check,
            OnContentEntity content,
            List<QuizResultEntity> reviewTargets,
            double selfScoreRate,
            Double evaluationScoreRate,
            Double scoreGap
    ) {
        if (evaluationScoreRate == null) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content_title", content.getTitle() != null ? content.getTitle() : "해당 콘텐츠");
            body.put("category_name", content.getCategory());
            body.put("self_score_rate", selfScoreRate);
            body.put("evaluation_score_rate", evaluationScoreRate);
            body.put("score_gap", scoreGap);
            body.put("need_more_explanation", Boolean.TRUE.equals(check.getNeedMoreExplanation()));
            body.put("memo", check.getMemo());
            body.put("review_items", buildReviewItemPayload(reviewTargets));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            Map response = restTemplate.postForObject(
                    aiServerBaseUrl + "/api/ai/evaluation/self-check-feedback",
                    requestEntity,
                    Map.class
            );

            if (response == null || response.get("feedback") == null) {
                return null;
            }

            return String.valueOf(response.get("feedback")).trim();
        } catch (Exception e) {
            System.out.println("AI 자기 평가 피드백 생성 실패: " + e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> buildReviewItemPayload(List<QuizResultEntity> reviewTargets) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (QuizResultEntity result : reviewTargets) {
            if (result.getQuestion() == null) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", result.getQuestion().getQuestionText());
            item.put("user_answer", resolveUserAnswer(result));
            item.put("correct_answer", resolveCorrectAnswer(result));
            item.put("explanation", result.getQuestion().getExplanation());
            item.put("score", result.getScore());
            item.put("max_score", result.getQuestion().getScore());
            item.put("is_correct", result.getIsCorrect());
            items.add(item);
        }
        return items;
    }

    private String resolveUserAnswer(QuizResultEntity result) {
        if (result.getAnswerText() != null && !result.getAnswerText().isBlank()) {
            return result.getAnswerText();
        }
        if (result.getSelectedNo() == null || result.getQuestion() == null) {
            return null;
        }
        return resolveOptionText(result, result.getSelectedNo());
    }

    private String resolveCorrectAnswer(QuizResultEntity result) {
        if (result.getQuestion() == null) {
            return null;
        }
        if (result.getQuestion().getSampleAnswer() != null && !result.getQuestion().getSampleAnswer().isBlank()) {
            return result.getQuestion().getSampleAnswer();
        }
        if (result.getQuestion().getAnswerNo() == null) {
            return null;
        }
        return resolveOptionText(result, result.getQuestion().getAnswerNo());
    }

    private String resolveOptionText(QuizResultEntity result, Integer optionNo) {
        if (result.getQuestion() == null || optionNo == null) {
            return null;
        }
        return switch (optionNo) {
            case 1 -> result.getQuestion().getOption1();
            case 2 -> result.getQuestion().getOption2();
            case 3 -> result.getQuestion().getOption3();
            case 4 -> result.getQuestion().getOption4();
            default -> null;
        };
    }

    private List<QuizResultEntity> findEvaluationResults(String empNo, String categoryName) {
        if (empNo == null || empNo.isBlank() || categoryName == null || categoryName.isBlank()) {
            return List.of();
        }

        return evaluationResultRepository.findByEmployee_EmpNoAndQuestion_CategoryName(empNo, categoryName);
    }

    private List<QuizResultEntity> findReviewTargets(String empNo, String categoryName) {
        return findEvaluationResults(empNo, categoryName).stream()
                .filter(this::isReviewTarget)
                .limit(3)
                .toList();
    }

    private boolean isReviewTarget(QuizResultEntity result) {
        if (Boolean.FALSE.equals(result.getIsCorrect())) {
            return true;
        }
        if (result.getQuestion() == null || result.getQuestion().getScore() == null) {
            return false;
        }

        int score = result.getScore() != null ? result.getScore() : 0;
        return score < result.getQuestion().getScore();
    }

    private String buildReviewGuide(List<QuizResultEntity> reviewTargets, String contentTitle) {
        if (reviewTargets.isEmpty()) {
            return "틀린 문제는 많지 않으니 '" + contentTitle + "'에서 핵심 개념과 적용 순서를 한 번 더 정리해 보세요.";
        }

        String guide = reviewTargets.stream()
                .map(this::toReviewPoint)
                .filter(point -> point != null && !point.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("틀린 문제에서 왜 그 답이 맞는지 다시 보세요.");

        return "다시 볼 곳: " + guide;
    }

    private String toReviewPoint(QuizResultEntity result) {
        if (result.getQuestion() == null) {
            return null;
        }

        String questionText = compact(result.getQuestion().getQuestionText(), 60);
        String explanation = compact(result.getQuestion().getExplanation(), 70);
        String sampleAnswer = compact(result.getQuestion().getSampleAnswer(), 70);

        if (explanation != null) {
            return "'" + questionText + "' 풀이 도움말: " + explanation;
        }
        if (sampleAnswer != null) {
            return "'" + questionText + "' 좋은 답 예시: " + sampleAnswer;
        }
        return "'" + questionText + "'";
    }

    private String buildScoreSummary(double selfScoreRate, Double evaluationScoreRate, Double scoreGap) {
        if (evaluationScoreRate == null) {
            return String.format("자기 평가 %.1f%%가 저장되었습니다.", selfScoreRate);
        }

        return String.format(
                "자기 평가 %.1f%%, AI 평가 %.1f%%, 차이 %+.1f%%입니다.",
                selfScoreRate,
                evaluationScoreRate,
                scoreGap != null ? scoreGap : 0.0
        );
    }

    private String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }

        return memo.trim();
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
