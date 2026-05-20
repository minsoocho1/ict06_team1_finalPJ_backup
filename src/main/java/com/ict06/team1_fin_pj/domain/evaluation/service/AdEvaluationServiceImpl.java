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
 * @ 2026.05.18    김다솜        이탈 징후 분석, 복합 통계 요약 로직 추가
 * @ 2026.05.18                 다중 문서-콘텐츠 연결 기반 RAG 문서 조회 추가
 * @ 2026.05.19    김다솜        이탈 위험 분석 기본 인사이트 문구를 보고서형 문장으로 정리
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
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminRetentionRiskDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationResponseDto;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
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
import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadmapEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdEvaluationServiceImpl implements AdEvaluationService {

    private static final String AI_GENERATED_PREFIX = "[AI자동생성]";
    private static final String LEGACY_AI_GENERATED_PREFIX = "[AI?먮룞?앹꽦]";

    private final OnContentRepository onContentRepository;
    private final EmpRepository empRepository;
    private final DocumentRepository documentRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final QuizGenerationRuleRepository quizGenerationRuleRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadItemRepository roadItemRepository;
    private final RoadProgressRepository roadProgressRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistProgressRepository checklistProgressRepository;
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

        List<AdminRetentionRiskDto> retentionRiskStats = buildRetentionRiskStats(attempts, ruleMap);
        String retentionAiInsight = buildFallbackRetentionInsight(retentionRiskStats);

        return AdminEvaluationAnalyticsDto.builder()
                .submissionCount(submissionCount)
                .participantCount(participantCount)
                .averageScoreRate(averageScoreRate)
                .weightedAverageScoreRate(weightedAverageScoreRate)
                .passRate(passRate)
                .highRiskEmployeeCount((int) retentionRiskStats.stream()
                        .filter(risk -> "HIGH".equals(risk.getRiskLevel()))
                        .count())
                .mediumRiskEmployeeCount((int) retentionRiskStats.stream()
                        .filter(risk -> "MEDIUM".equals(risk.getRiskLevel()))
                        .count())
                .averageLearningProgressRate(roundOneDecimal(retentionRiskStats.stream()
                        .mapToDouble(AdminRetentionRiskDto::getLearningProgressRate)
                        .average()
                        .orElse(0.0)))
                .averageChecklistProgressRate(roundOneDecimal(retentionRiskStats.stream()
                        .mapToDouble(AdminRetentionRiskDto::getChecklistProgressRate)
                        .average()
                        .orElse(0.0)))
                .retentionAiInsight(retentionAiInsight)
                .categoryStats(categoryStats)
                .questionStats(questionStats)
                .employeeStats(employeeStats)
                .retentionRiskStats(retentionRiskStats)
                .build();
    }

    private Optional<DocumentEntity> findRelatedDocument(OnContentEntity content) {
        if (content.getType() != null && "VIDEO".equalsIgnoreCase(content.getType().name())) {
            return Optional.empty();
        }

        Optional<DocumentEntity> directDocument = documentRepository.findFirstByRelatedContents_ContentIdOrderByCreatedAtDesc(content.getContentId())
                .or(() -> documentRepository.findFirstByRelatedContent_ContentIdOrderByCreatedAtDesc(content.getContentId()));
        if (directDocument.isPresent()) {
            return directDocument;
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

    private List<AdminRetentionRiskDto> buildRetentionRiskStats(
            List<EvaluationAttemptSummary> attempts,
            Map<String, QuizGenerationRuleEntity> ruleMap
    ) {
        Map<String, List<EvaluationAttemptSummary>> attemptsByEmployee = attempts.stream()
                .collect(Collectors.groupingBy(EvaluationAttemptSummary::empNo, LinkedHashMap::new, Collectors.toList()));

        int totalChecklistCount = checklistRepository.findAll().size();
        List<EmpEntity> targetEmployees = empRepository.findAll().stream()
                .filter(emp -> !"Y".equalsIgnoreCase(emp.getIsDeleted()))
                .filter(emp -> emp.getRole() == null || emp.getRole().getRoleId() != 1)
                .toList();

        Map<String, Double> learningRateByEmployee = targetEmployees.stream()
                .collect(Collectors.toMap(EmpEntity::getEmpNo, emp -> calculateLearningProgressRate(emp.getEmpNo())));
        Map<String, Double> checklistRateByEmployee = targetEmployees.stream()
                .collect(Collectors.toMap(EmpEntity::getEmpNo, emp -> calculateChecklistProgressRate(emp.getEmpNo(), totalChecklistCount)));
        Map<String, Double> evaluationRateByEmployee = targetEmployees.stream()
                .collect(Collectors.toMap(
                        EmpEntity::getEmpNo,
                        emp -> attemptsByEmployee.getOrDefault(emp.getEmpNo(), List.of()).stream()
                                .mapToDouble(EvaluationAttemptSummary::scoreRate)
                                .average()
                                .orElse(0.0)
                ));

        double averageLearningRate = learningRateByEmployee.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double averageChecklistRate = checklistRateByEmployee.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double averageEvaluationRate = evaluationRateByEmployee.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return targetEmployees.stream()
                .filter(emp -> hasOnboardingStarted(emp.getEmpNo(), attemptsByEmployee.getOrDefault(emp.getEmpNo(), List.of())))
                .map(emp -> buildRetentionRisk(
                        emp,
                        attemptsByEmployee.getOrDefault(emp.getEmpNo(), List.of()),
                        ruleMap,
                        learningRateByEmployee.getOrDefault(emp.getEmpNo(), 0.0),
                        checklistRateByEmployee.getOrDefault(emp.getEmpNo(), 0.0),
                        evaluationRateByEmployee.getOrDefault(emp.getEmpNo(), 0.0),
                        averageLearningRate,
                        averageChecklistRate,
                        averageEvaluationRate
                ))
                .sorted(Comparator.comparing(AdminRetentionRiskDto::getRiskScore).reversed()
                        .thenComparing(AdminRetentionRiskDto::getEmpNo))
                .limit(10)
                .toList();
    }

    private boolean hasOnboardingStarted(String empNo, List<EvaluationAttemptSummary> attempts) {
        if (attempts != null && !attempts.isEmpty()) {
            return true;
        }

        Optional<RoadmapEntity> roadmap = roadmapRepository.findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(empNo);
        if (roadmap.isEmpty()) {
            return false;
        }

        List<RoadItemEntity> items = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.get().getRoadmapId());
        if (items.isEmpty()) {
            return false;
        }

        boolean hasProgress = roadProgressRepository.findByEmployee_EmpNo(empNo).stream()
                .anyMatch(progress -> progress.getItem() != null);
        if (hasProgress) {
            return true;
        }

        return items.stream()
                .map(RoadItemEntity::getStartDate)
                .filter(startDate -> startDate != null)
                .min(LocalDate::compareTo)
                .map(firstStartDate -> !firstStartDate.isAfter(LocalDate.now()))
                .orElse(false);
    }

    private AdminRetentionRiskDto buildRetentionRisk(
            EmpEntity emp,
            List<EvaluationAttemptSummary> attempts,
            Map<String, QuizGenerationRuleEntity> ruleMap,
            double learningProgressRate,
            double checklistProgressRate,
            double evaluationAverageScoreRate,
            double averageLearningRate,
            double averageChecklistRate,
            double averageEvaluationRate
    ) {
        evaluationAverageScoreRate = roundOneDecimal(evaluationAverageScoreRate);
        double evaluationPassRate = attempts.isEmpty()
                ? 0.0
                : roundOneDecimal(attempts.stream().filter(EvaluationAttemptSummary::passed).count() * 100.0 / attempts.size());
        LocalDateTime latestSubmittedAt = attempts.stream()
                .map(EvaluationAttemptSummary::submittedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<String> reasons = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int riskScore = 0;

        if (learningProgressRate < 50.0) {
            riskScore += 25;
            reasons.add("학습 완료율이 50% 미만입니다.");
            recommendations.add("미완료 학습 콘텐츠를 우선순위로 재배치하고 1:1 학습 리마인드를 발송하세요.");
        } else if (learningProgressRate < 80.0) {
            riskScore += 12;
            reasons.add("학습 완료율이 목표치보다 낮습니다.");
            recommendations.add("남은 학습 항목 중 필수 콘텐츠부터 완료하도록 안내하세요.");
        }

        if (checklistProgressRate < 50.0) {
            riskScore += 20;
            reasons.add("체크리스트 완료율이 50% 미만입니다.");
            recommendations.add("온보딩 체크리스트 중 미완료 항목을 확인하고 담당자가 진행 여부를 점검하세요.");
        } else if (checklistProgressRate < 80.0) {
            riskScore += 10;
            reasons.add("체크리스트 진행이 다소 지연되고 있습니다.");
            recommendations.add("체크리스트 마감 항목을 홈 To-Do 또는 알림으로 재노출하세요.");
        }

        if (attempts.isEmpty()) {
            riskScore += 25;
            reasons.add("아직 평가에 응시하지 않았습니다.");
            recommendations.add("학습 완료 후 평가 응시 일정을 별도로 안내하세요.");
        } else {
            if (evaluationAverageScoreRate < 70.0) {
                riskScore += 15;
                reasons.add("평가 평균 점수가 70% 미만입니다.");
                recommendations.add("오답이 많은 카테고리의 콘텐츠 요약/재설명 기능을 활용하도록 안내하세요.");
            }
            if (evaluationPassRate < 50.0) {
                riskScore += 20;
                reasons.add("평가 통과율이 50% 미만입니다.");
                recommendations.add("불합격 카테고리에 대한 보충 학습 후 재응시를 권장하세요.");
            }
        }

        if (latestSubmittedAt != null && ChronoUnit.DAYS.between(latestSubmittedAt, LocalDateTime.now()) >= 7) {
            riskScore += 10;
            reasons.add("최근 평가 제출 이후 7일 이상 추가 활동이 없습니다.");
            recommendations.add("최근 활동 공백이 길어졌으므로 담당자 확인 또는 자동 알림을 발송하세요.");
        }

        if (averageLearningRate > 0 && learningProgressRate <= averageLearningRate - 25.0) {
            riskScore += 12;
            reasons.add("전체 직원 평균 대비 학습 완료율이 크게 낮습니다.");
            recommendations.add("동료군 평균보다 뒤처진 학습 항목을 우선순위로 재배치하세요.");
        }

        if (averageChecklistRate > 0 && checklistProgressRate <= averageChecklistRate - 25.0) {
            riskScore += 10;
            reasons.add("전체 직원 평균 대비 체크리스트 완료율이 낮은 편입니다.");
            recommendations.add("체크리스트 미완료 항목을 관리자 확인 대상으로 표시하세요.");
        }

        if (!attempts.isEmpty() && averageEvaluationRate > 0 && evaluationAverageScoreRate <= averageEvaluationRate - 20.0) {
            riskScore += 12;
            reasons.add("전체 직원 평균 대비 평가 이해도 점수가 낮습니다.");
            recommendations.add("평균 대비 낮은 평가 카테고리를 기준으로 보충 학습을 추천하세요.");
        }

        List<String> weakCategories = findWeakCategories(attempts, ruleMap);
        if (!weakCategories.isEmpty()) {
            recommendations.add("취약 카테고리(" + String.join(", ", weakCategories) + ")의 추천 학습 가이드를 우선 배정하세요.");
        }

        if (reasons.isEmpty()) {
            reasons.add("현재 뚜렷한 이탈 위험 신호는 낮습니다.");
            recommendations.add("현재 진행 흐름을 유지하되 다음 평가 일정만 사전 안내하세요.");
        }

        int normalizedRiskScore = Math.min(100, riskScore);
        return AdminRetentionRiskDto.builder()
                .empNo(emp.getEmpNo())
                .employeeName(emp.getName())
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getDeptName() : "미지정")
                .riskLevel(resolveRiskLevel(normalizedRiskScore))
                .riskScore(normalizedRiskScore)
                .learningProgressRate(learningProgressRate)
                .checklistProgressRate(checklistProgressRate)
                .evaluationAverageScoreRate(evaluationAverageScoreRate)
                .evaluationPassRate(evaluationPassRate)
                .evaluationCount(attempts.size())
                .latestSubmittedAt(latestSubmittedAt)
                .riskReasons(reasons.stream().distinct().limit(4).toList())
                .recommendations(recommendations.stream().distinct().limit(4).toList())
                .build();
    }

    private double calculateLearningProgressRate(String empNo) {
        Optional<RoadmapEntity> roadmap = roadmapRepository.findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(empNo);
        if (roadmap.isEmpty()) {
            return 0.0;
        }

        List<RoadItemEntity> items = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.get().getRoadmapId());
        if (items.isEmpty()) {
            return 0.0;
        }

        Set<Integer> latestItemIds = items.stream()
                .map(RoadItemEntity::getItemId)
                .collect(Collectors.toSet());
        long completedCount = roadProgressRepository.findByEmployee_EmpNo(empNo).stream()
                .filter(progress -> progress.getItem() != null && latestItemIds.contains(progress.getItem().getItemId()))
                .filter(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .count();

        return roundOneDecimal(completedCount * 100.0 / items.size());
    }

    private double calculateChecklistProgressRate(String empNo, int totalChecklistCount) {
        if (totalChecklistCount <= 0) {
            return 0.0;
        }

        long completedCount = checklistProgressRepository.countByEmployee_EmpNoAndStatus(empNo, ProgressStatus.COMPLETED);
        return roundOneDecimal(completedCount * 100.0 / totalChecklistCount);
    }

    private List<String> findWeakCategories(
            List<EvaluationAttemptSummary> attempts,
            Map<String, QuizGenerationRuleEntity> ruleMap
    ) {
        Set<String> weakCategories = new HashSet<>();
        for (EvaluationAttemptSummary attempt : attempts) {
            QuizGenerationRuleEntity rule = ruleMap.get(attempt.categoryName());
            int passScore = rule != null && rule.getPassScore() != null ? rule.getPassScore() : 80;
            if (attempt.scoreRate() < passScore) {
                weakCategories.add(attempt.categoryName());
            }
        }
        return weakCategories.stream().sorted().limit(3).toList();
    }

    private String resolveRiskLevel(int riskScore) {
        if (riskScore >= 70) {
            return "HIGH";
        }
        if (riskScore >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    @Override
    public String getAiRetentionRiskAnalysis(List<AdminRetentionRiskDto> retentionRiskStats) {
        if (retentionRiskStats == null || retentionRiskStats.isEmpty()) {
            return "이탈 징후를 분석할 직원 데이터가 충분하지 않습니다.";
        }

        try {
            List<Map<String, Object>> topRisks = retentionRiskStats.stream()
                    .limit(5)
                    .map(risk -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("empNo", risk.getEmpNo());
                        row.put("employeeName", risk.getEmployeeName());
                        row.put("departmentName", risk.getDepartmentName());
                        row.put("riskLevel", risk.getRiskLevel());
                        row.put("riskScore", risk.getRiskScore());
                        row.put("learningProgressRate", risk.getLearningProgressRate());
                        row.put("checklistProgressRate", risk.getChecklistProgressRate());
                        row.put("evaluationAverageScoreRate", risk.getEvaluationAverageScoreRate());
                        row.put("evaluationPassRate", risk.getEvaluationPassRate());
                        row.put("riskReasons", risk.getRiskReasons());
                        row.put("recommendations", risk.getRecommendations());
                        return row;
                    })
                    .toList();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("highRiskCount", retentionRiskStats.stream().filter(risk -> "HIGH".equals(risk.getRiskLevel())).count());
            payload.put("mediumRiskCount", retentionRiskStats.stream().filter(risk -> "MEDIUM".equals(risk.getRiskLevel())).count());
            payload.put("topRisks", topRisks);

            String url = aiServerBaseUrl + "/api/ai/evaluation/analyze-retention";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
            if (response.getBody() != null && response.getBody().get("analysis") != null) {
                return (String) response.getBody().get("analysis");
            }
        } catch (Exception e) {
            System.err.println("AI 이탈 징후 분석 코멘트 생성 실패: " + e.getMessage());
        }

        return buildFallbackRetentionInsight(retentionRiskStats);
    }

    private String buildFallbackRetentionInsight(List<AdminRetentionRiskDto> retentionRiskStats) {
        if (retentionRiskStats == null || retentionRiskStats.isEmpty()) {
            return "이탈 징후를 분석할 직원 데이터가 충분하지 않습니다.";
        }

        long highRiskCount = retentionRiskStats.stream().filter(risk -> "HIGH".equals(risk.getRiskLevel())).count();
        long mediumRiskCount = retentionRiskStats.stream().filter(risk -> "MEDIUM".equals(risk.getRiskLevel())).count();
        AdminRetentionRiskDto topRisk = retentionRiskStats.get(0);
        String riskReasons = topRisk.getRiskReasons() == null || topRisk.getRiskReasons().isEmpty()
                ? "- 주요 위험 신호가 아직 충분히 수집되지 않았습니다."
                : topRisk.getRiskReasons().stream()
                .limit(3)
                .map(reason -> "- " + normalizeSentence(reason))
                .collect(Collectors.joining("\n"));

        return """
                [규칙 기반 요약]
                현재 고위험 직원은 %d명, 주의 필요 직원은 %d명입니다.

                [주요 위험 신호]
                %s님의 위험도가 가장 높습니다.
                %s

                [우선 조치]
                1. 미완료 학습을 다시 안내합니다.
                2. 체크리스트 진행 상태를 점검합니다.
                3. 취약 카테고리 보충 학습을 배정합니다.
                """.formatted(
                highRiskCount,
                mediumRiskCount,
                topRisk.getEmployeeName(),
                riskReasons
        ).trim();
    }

    private String normalizeSentence(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        String trimmed = value.trim();
        return trimmed.endsWith(".") ? trimmed : trimmed + ".";
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
