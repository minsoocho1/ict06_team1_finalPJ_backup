/**
 * @FileName : AdEvaluationServiceImpl.java
 * @Description : 관리자 온보딩 평가 관리 서비스 구현체
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.11    김다솜        AI 서버 퀴즈 초안 생성 연동 및 생성 문제 저장 로직 추가
 * @ 2026.05.13    김다솜        PDF 기반 RAG 문서 요약/청크 문맥 반영 및 문서 연계 퀴즈 자동 생성 저장 로직 추가
 * @ 2026.05.15    김다솜        출제 기준 가중치 수정 시 updateRule 호출로 컴파일 오류 수정
 */
package com.ict06.team1_fin_pj.domain.evaluation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminEvaluationAnalyticsDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminEvaluationCategoryStatsDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminEvaluationEmployeeStatsDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminEvaluationQuestionStatsDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizDraftDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizGenerationRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizSaveRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationResponseDto;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuestionType;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizGenerationRuleEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizQuestionEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizResultEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationQuestionRepository;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationResultRepository;
import com.ict06.team1_fin_pj.domain.evaluation.repository.QuizGenerationRuleRepository;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdEvaluationServiceImpl implements AdEvaluationService {

    private static final String AI_GENERATED_PREFIX = "[AI자동생성]";
    private static final String LEGACY_AI_GENERATED_PREFIX = "[AI?먮룞?앹꽦]";

    private final OnContentRepository onContentRepository;
    private final DocumentRepository documentRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final QuizGenerationRuleRepository quizGenerationRuleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    @Override
    public AiQuizGenerationResponseDto generateQuizDrafts(AdminQuizGenerationRequestDto requestDto) {
        OnContentEntity content = onContentRepository.findById(requestDto.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("선택한 콘텐츠를 찾을 수 없습니다."));
        Optional<DocumentEntity> relatedDocument = findRelatedDocument(content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiQuizGenerationRequestDto body = AiQuizGenerationRequestDto.builder()
                .contentId(content.getContentId())
                .title(content.getTitle())
                .category(content.getCategory())
                .subCategory(content.getSubCategory())
                .contentType(content.getType() != null ? content.getType().name() : "LINK")
                .difficulty((requestDto.getDifficulty() != null ? requestDto.getDifficulty() : content.getDifficulty()).name())
                .questionCount(requestDto.getQuestionCount() != null ? requestDto.getQuestionCount() : 3)
                .tags(relatedDocument.isPresent() ? null : content.getTags())
                .path(relatedDocument.isPresent() ? null : content.getPath())
                .ragSummary(relatedDocument.map(DocumentEntity::getSummaryPreview).orElse(null))
                .ragContext(relatedDocument.map(this::buildRagContext).orElse(null))
                .build();

        String url = aiServerBaseUrl + "/api/ai/evaluation/generate";
        ResponseEntity<AiQuizGenerationResponseDto> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                AiQuizGenerationResponseDto.class
        );

        AiQuizGenerationResponseDto payload = response.getBody();
        if (payload == null || payload.getQuestions() == null || payload.getQuestions().isEmpty()) {
            throw new IllegalStateException("AI가 생성한 문제 초안이 없습니다.");
        }

        return payload;
    }

    @Override
    @Transactional
    public int saveGeneratedQuestions(AdminQuizSaveRequestDto requestDto) {
        OnContentEntity content = onContentRepository.findById(requestDto.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("선택한 콘텐츠를 찾을 수 없습니다."));

        try {
            List<AdminQuizDraftDto> drafts = objectMapper.readValue(
                    requestDto.getGeneratedQuestionsJson(),
                    new TypeReference<>() {
                    }
            );
            List<Integer> distributedScores = distributeScores(drafts.size());
            List<QuizQuestionEntity> questions = new java.util.ArrayList<>();
            for (int i = 0; i < drafts.size(); i++) {
                AdminQuizDraftDto draft = drafts.get(i);
                questions.add(QuizQuestionEntity.builder()
                        .content(content)
                        .categoryName(content.getCategory())
                        .questionType(resolveQuestionType(draft.getQuestionType()))
                        .questionText(draft.getQuestionText())
                        .option1(draft.getOption1())
                        .option2(draft.getOption2())
                        .option3(draft.getOption3())
                        .option4(draft.getOption4())
                        .answerNo(draft.getAnswerNo())
                        .sampleAnswer(draft.getSampleAnswer())
                        .keywordAnswer(toKeywordAnswerJson(draft))
                        .rubric(draft.getRubric())
                        .score(distributedScores.get(i))
                        .explanation(markGeneratedExplanation(draft.getExplanation()))
                        .build());
            }

            evaluationQuestionRepository.saveAll(questions);
            return questions.size();
        } catch (Exception e) {
            throw new IllegalStateException("생성된 문제 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int generateAndSaveQuestionsForContent(Integer contentId) {
        OnContentEntity content = onContentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("연결된 콘텐츠를 찾을 수 없습니다."));

        QuizGenerationRuleEntity rule = quizGenerationRuleRepository
                .findFirstByCategoryNameAndIsActiveTrueOrderByRuleIdDesc(content.getCategory())
                .orElse(null);

        AdminQuizGenerationRequestDto draftRequest = new AdminQuizGenerationRequestDto();
        draftRequest.setContentId(contentId);
        draftRequest.setQuestionCount(rule != null && rule.getQuestionCount() != null ? rule.getQuestionCount() : 3);
        draftRequest.setDifficulty(rule != null && rule.getDifficulty() != null ? rule.getDifficulty() : content.getDifficulty());

        AiQuizGenerationResponseDto generated = generateQuizDrafts(draftRequest);

        try {
            String generatedQuestionsJson = objectMapper.writeValueAsString(generated.getQuestions());
            evaluationQuestionRepository.deleteByContent_ContentId(contentId);

            AdminQuizSaveRequestDto saveRequest = new AdminQuizSaveRequestDto();
            saveRequest.setContentId(contentId);
            saveRequest.setGeneratedQuestionsJson(generatedQuestionsJson);
            return saveGeneratedQuestions(saveRequest);
        } catch (Exception e) {
            throw new IllegalStateException("문서 연계 퀴즈 자동 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEvaluationAnalyticsDto getEvaluationAnalytics() {
        List<QuizResultEntity> results = evaluationResultRepository.findAll();
        Map<String, QuizGenerationRuleEntity> ruleMap = loadActiveRuleMap();
        List<EvaluationAttemptSummary> attempts = summarizeAttempts(results, ruleMap);

        int submissionCount = attempts.size();
        int participantCount = (int) attempts.stream()
                .map(EvaluationAttemptSummary::empNo)
                .distinct()
                .count();
        double averageScoreRate = roundOneDecimal(attempts.stream()
                .mapToDouble(EvaluationAttemptSummary::scoreRate)
                .average()
                .orElse(0.0));
        double weightedAverageScoreRate = roundOneDecimal(calculateWeightedAverageScore(attempts, ruleMap));
        double passRate = submissionCount == 0
                ? 0.0
                : roundOneDecimal(attempts.stream().filter(EvaluationAttemptSummary::passed).count() * 100.0 / submissionCount);

        List<AdminEvaluationCategoryStatsDto> categoryStats = attempts.stream()
                .collect(Collectors.groupingBy(EvaluationAttemptSummary::categoryName, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<EvaluationAttemptSummary> grouped = entry.getValue();
                    QuizGenerationRuleEntity rule = ruleMap.get(entry.getKey());
                    int questionCount = rule != null && rule.getQuestionCount() != null ? rule.getQuestionCount() : 0;
                    int totalScore = 100;
                    int passScore = rule != null && rule.getPassScore() != null ? rule.getPassScore() : 80;
                    int requiredScore = passScore;
                    int weightPercent = rule != null && rule.getWeightPercent() != null ? rule.getWeightPercent() : 100;
                    double categoryAverageScore = roundOneDecimal(grouped.stream()
                            .mapToDouble(EvaluationAttemptSummary::scoreRate)
                            .average()
                            .orElse(0.0));

                    return AdminEvaluationCategoryStatsDto.builder()
                            .categoryName(entry.getKey())
                            .submissionCount(grouped.size())
                            .questionCount(questionCount)
                            .totalScore(totalScore)
                            .passScore(passScore)
                            .requiredScore(requiredScore)
                            .weightPercent(weightPercent)
                            .averageScoreRate(categoryAverageScore)
                            .weightedAverageScoreRate(roundOneDecimal(categoryAverageScore * weightPercent / 100.0))
                            .passRate(roundOneDecimal(grouped.stream()
                                    .filter(EvaluationAttemptSummary::passed)
                                    .count() * 100.0 / Math.max(1, grouped.size())))
                            .scoreGap(roundOneDecimal(categoryAverageScore - passScore))
                            .build();
                })
                .sorted(Comparator.comparing(AdminEvaluationCategoryStatsDto::getCategoryName))
                .toList();

        List<AdminEvaluationQuestionStatsDto> questionStats = results.stream()
                .filter(result -> result.getQuestion() != null)
                .collect(Collectors.groupingBy(result -> result.getQuestion().getQuestionId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(grouped -> {
                    QuizQuestionEntity question = grouped.get(0).getQuestion();
                    return AdminEvaluationQuestionStatsDto.builder()
                            .questionId(question.getQuestionId())
                            .categoryName(question.getCategoryName())
                            .questionText(question.getQuestionText())
                            .responseCount(grouped.size())
                            .averageScoreRate(roundOneDecimal(grouped.stream()
                                    .mapToDouble(result -> toScoreRate(result.getScore(), question.getScore()))
                                    .average()
                                    .orElse(0.0)))
                            .build();
                })
                .sorted(Comparator.comparing(AdminEvaluationQuestionStatsDto::getAverageScoreRate)
                        .thenComparing(AdminEvaluationQuestionStatsDto::getQuestionId))
                .limit(8)
                .toList();

        List<AdminEvaluationEmployeeStatsDto> employeeStats = attempts.stream()
                .collect(Collectors.groupingBy(EvaluationAttemptSummary::empNo, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(grouped -> {
                    EvaluationAttemptSummary first = grouped.get(0);
                    return AdminEvaluationEmployeeStatsDto.builder()
                            .empNo(first.empNo())
                            .employeeName(first.employeeName())
                            .departmentName(first.departmentName())
                            .evaluationCount(grouped.size())
                            .passCount((int) grouped.stream().filter(EvaluationAttemptSummary::passed).count())
                            .averageScoreRate(roundOneDecimal(grouped.stream()
                                    .mapToDouble(EvaluationAttemptSummary::scoreRate)
                                    .average()
                                    .orElse(0.0)))
                            .latestSubmittedAt(grouped.stream()
                                    .map(EvaluationAttemptSummary::submittedAt)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null))
                            .build();
                })
                .sorted(Comparator.comparing(AdminEvaluationEmployeeStatsDto::getAverageScoreRate).reversed()
                        .thenComparing(AdminEvaluationEmployeeStatsDto::getEmpNo))
                .toList();

        return AdminEvaluationAnalyticsDto.builder()
                .submissionCount(submissionCount)
                .participantCount(participantCount)
                .averageScoreRate(averageScoreRate)
                .weightedAverageScoreRate(weightedAverageScoreRate)
                .passRate(passRate)
                .categoryStats(categoryStats)
                .questionStats(questionStats)
                .employeeStats(employeeStats)
                .build();
    }

    private Optional<DocumentEntity> findRelatedDocument(OnContentEntity content) {
        if (content.getType() != null && "VIDEO".equalsIgnoreCase(content.getType().name())) {
            return Optional.empty();
        }

        if (content.getPath() != null && !content.getPath().isBlank()) {
            Optional<DocumentEntity> byPath = documentRepository.findFirstByFilePathOrderByCreatedAtDesc(content.getPath());
            if (byPath.isPresent()) {
                return byPath;
            }
        }

        if (content.getTitle() != null && !content.getTitle().isBlank()) {
            return documentRepository.findFirstByTitleIgnoreCaseOrderByCreatedAtDesc(content.getTitle());
        }

        return Optional.empty();
    }

    private String buildRagContext(DocumentEntity document) {
        return document.getChunks().stream()
                .sorted(Comparator.comparing(chunk -> chunk.getChunkNo() != null ? chunk.getChunkNo() : Integer.MAX_VALUE))
                .limit(5)
                .map(this::toChunkSnippet)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse(document.getSummaryPreview());
    }

    private String toChunkSnippet(DocChunkEntity chunk) {
        String title = chunk.getSectionTitle() != null && !chunk.getSectionTitle().isBlank()
                ? chunk.getSectionTitle()
                : "Chunk " + chunk.getChunkNo();
        String content = chunk.getContent() != null ? chunk.getContent().trim() : "";

        if (content.length() > 500) {
            content = content.substring(0, 500);
        }

        return "[" + title + "]\n" + content;
    }

    private QuestionType resolveQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return QuestionType.MULTIPLE_CHOICE;
        }

        try {
            return QuestionType.valueOf(questionType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return QuestionType.MULTIPLE_CHOICE;
        }
    }

    private String toKeywordAnswerJson(AdminQuizDraftDto draft) {
        if (draft.getKeywordAnswer() == null || draft.getKeywordAnswer().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(draft.getKeywordAnswer());
        } catch (Exception e) {
            return null;
        }
    }

    private String markGeneratedExplanation(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return AI_GENERATED_PREFIX;
        }

        if (explanation.startsWith(AI_GENERATED_PREFIX)) {
            return explanation;
        }

        if (explanation.startsWith(LEGACY_AI_GENERATED_PREFIX)) {
            return explanation.replace(LEGACY_AI_GENERATED_PREFIX, AI_GENERATED_PREFIX);
        }

        return AI_GENERATED_PREFIX + " " + explanation;
    }

    private List<Integer> distributeScores(int questionCount) {
        if (questionCount <= 0) {
            return List.of();
        }

        int baseScore = 100 / questionCount;
        int remainder = 100 % questionCount;
        List<Integer> scores = new java.util.ArrayList<>();

        for (int i = 0; i < questionCount; i++) {
            scores.add(baseScore + (i < remainder ? 1 : 0));
        }

        return scores;
    }

    private Map<String, QuizGenerationRuleEntity> loadActiveRuleMap() {
        return quizGenerationRuleRepository.findAllByOrderByCategoryNameAscRuleIdAsc().stream()
                .filter(rule -> rule.getIsActive() != null && rule.getIsActive())
                .collect(Collectors.toMap(
                        QuizGenerationRuleEntity::getCategoryName,
                        rule -> rule,
                        (previous, current) -> current,
                        LinkedHashMap::new
                ));
    }

    private double calculateWeightedAverageScore(
            List<EvaluationAttemptSummary> attempts,
            Map<String, QuizGenerationRuleEntity> ruleMap
    ) {
        double totalWeightedScore = attempts.stream()
                .mapToDouble(attempt -> {
                    QuizGenerationRuleEntity rule = ruleMap.get(attempt.categoryName());
                    int weightPercent = rule != null && rule.getWeightPercent() != null ? rule.getWeightPercent() : 100;
                    return attempt.scoreRate() * weightPercent;
                })
                .sum();

        int totalWeight = attempts.stream()
                .mapToInt(attempt -> {
                    QuizGenerationRuleEntity rule = ruleMap.get(attempt.categoryName());
                    return rule != null && rule.getWeightPercent() != null ? rule.getWeightPercent() : 100;
                })
                .sum();

        if (totalWeight <= 0) {
            return 0.0;
        }

        return totalWeightedScore / totalWeight;
    }

    private List<EvaluationAttemptSummary> summarizeAttempts(
            List<QuizResultEntity> results,
            Map<String, QuizGenerationRuleEntity> ruleMap
    ) {
        return summarizeAttempts(results).stream()
                .map(attempt -> {
                    QuizGenerationRuleEntity rule = ruleMap.get(attempt.categoryName());
                    int passScore = rule != null && rule.getPassScore() != null ? rule.getPassScore() : 80;
                    boolean passed = attempt.maxScore() > 0 && attempt.scoreRate() >= passScore;
                    return new EvaluationAttemptSummary(
                            attempt.empNo(),
                            attempt.employeeName(),
                            attempt.departmentName(),
                            attempt.categoryName(),
                            attempt.totalScore(),
                            attempt.maxScore(),
                            attempt.scoreRate(),
                            passed,
                            attempt.submittedAt()
                    );
                })
                .toList();
    }

    private List<EvaluationAttemptSummary> summarizeAttempts(List<QuizResultEntity> results) {
        Map<String, List<QuizResultEntity>> grouped = results.stream()
                .filter(result -> result.getEmployee() != null)
                .filter(result -> result.getQuestion() != null)
                .collect(Collectors.groupingBy(
                        result -> result.getEmployee().getEmpNo() + "|" + result.getQuestion().getCategoryName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .map(group -> {
                    QuizResultEntity first = group.get(0);
                    int totalScore = group.stream()
                            .mapToInt(result -> result.getScore() == null ? 0 : result.getScore())
                            .sum();
                    int maxScore = group.stream()
                            .mapToInt(result -> result.getQuestion().getScore() == null ? 0 : result.getQuestion().getScore())
                            .sum();
                    double scoreRate = toScoreRate(totalScore, maxScore);
                    LocalDateTime submittedAt = group.stream()
                            .map(QuizResultEntity::getSubmittedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return new EvaluationAttemptSummary(
                            first.getEmployee().getEmpNo(),
                            first.getEmployee().getName(),
                            first.getEmployee().getDepartment() != null ? first.getEmployee().getDepartment().getDeptName() : "미지정",
                            first.getQuestion().getCategoryName(),
                            totalScore,
                            maxScore,
                            scoreRate,
                            false, // 실제 통과 여부는 호출 측(summarizeAttempts with ruleMap)에서 재계산됨
                            submittedAt
                    );
                })
                .toList();
    }

    private double toScoreRate(Integer score, Integer maxScore) {
        int safeScore = score == null ? 0 : score;
        int safeMaxScore = maxScore == null ? 0 : maxScore;
        if (safeMaxScore <= 0) {
            return 0.0;
        }
        return safeScore * 100.0 / safeMaxScore;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record EvaluationAttemptSummary(
            String empNo,
            String employeeName,
            String departmentName,
            String categoryName,
            int totalScore,
            int maxScore,
            double scoreRate,
            boolean passed,
            LocalDateTime submittedAt
    ) {
    }

    @Override
    @Transactional
    public int updateCategoryCriteria(String categoryName, Integer passScore, Double weight) {
        // 1. 해당 카테고리의 활성화된 규칙을 찾거나 없으면 새로 생성
        QuizGenerationRuleEntity rule = quizGenerationRuleRepository
                .findFirstByCategoryNameAndIsActiveTrueOrderByRuleIdDesc(categoryName)
                .orElseGet(() -> QuizGenerationRuleEntity.builder()
                        .categoryName(categoryName)
                        .isActive(true)
                        .build());

        // 2. 가중치 변환 (Double 1.0 -> Integer 100)
        int weightPercent = (int) (weight * 100);

        // 3. 기존 규칙 값은 유지하고 통계 화면에서 조정하는 통과점수/가중치만 갱신
        rule.updateRule(
                rule.getCategoryName(),
                rule.getQuestionCount(),
                passScore,
                weightPercent,
                rule.getDifficulty(),
                rule.getQuestionType(),
                rule.getIsActive()
        );
        
        quizGenerationRuleRepository.save(rule);
        return 1;
    }

    @Override
    public String getAiEvaluationAnalysis(Object stats) {
        try {
            String url = aiServerBaseUrl + "/api/ai/evaluation/analyze-stats";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, stats, Map.class);
            if (response.getBody() != null && response.getBody().get("analysis") != null) {
                return (String) response.getBody().get("analysis");
            }
        } catch (Exception e) {
            System.err.println("AI 분석 코멘트 생성 실패: " + e.getMessage());
        }
        return "현재 AI 분석 기능을 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
    }
}
