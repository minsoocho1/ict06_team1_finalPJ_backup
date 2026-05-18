/**
 * @FileName : EvaluationServiceImpl.java
 * @Description : 온보딩 퀴즈 평가 Service 구현 클래스
 *                - 학습 카테고리별 퀴즈 문항 조회
 *                - 객관식 자동 채점 및 주관식(단답형/서술형) AI 채점 확장 기반 처리
 *                - 문항별 응답 결과를 QUIZ_RESULT 테이블에 저장
 *                - 카테고리별 총점, 만점, 통과 여부 계산
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 퀴즈 조회/제출/채점 로직 구현
 * @ 2026.05.01    김다솜        콘텐츠별 평가에서 카테고리별 평가 구조로 수정
 * @ 2026.05.02    김다솜        카테고리별 평가 결과 집계 로직 추가
 * @ 2026.05.04    김다솜        주관식 AI 채점 점수 totalScore 반영(aiScore → score),
 * @ 2026.05.04    김다솜        RestTemplate Bean 주입으로 변경(매 요청 new 생성 제거)
 * @ 2026.05.06    김다솜        카테고리별 퀴즈 상세 결과 조회 메서드 추가 (getEvaluationDetail)
 * @ 2026.05.08    김다솜        퀴즈 제출 완료 시 알림 생성 및 SSE 실시간 전송 연동
 */

package com.ict06.team1_fin_pj.domain.evaluation.service;

import com.ict06.team1_fin_pj.common.dto.evaluation.*;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuestionType;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizQuestionEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizResultEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizGenerationRuleEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.QuizGenerationRuleRepository;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationQuestionRepository;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationResultRepository;
import com.ict06.team1_fin_pj.domain.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl {

    private final EvaluationQuestionRepository questionRepository;
    private final EvaluationResultRepository resultRepository;
    private final EmpRepository empRepository;
    private final RestTemplate restTemplate;
    private final NotificationServiceImpl notificationService;
    private final QuizGenerationRuleRepository quizGenerationRuleRepository;

    //학습 카테고리별 퀴즈 문항 조회
    public List<EvaluationQuestionResponse> getQuizQuestionsByCategory(String categoryName) {
        return questionRepository.findByCategoryName(categoryName)
                .stream()
                .map(question -> EvaluationQuestionResponse.builder()
                        .questionId(question.getQuestionId())
                        .contentId(
                                question.getContent() != null
                                ? question.getContent().getContentId()
                                        : null
                        )
                        .categoryName(question.getCategoryName())
                        .questionType(question.getQuestionType())
                        .questionText(question.getQuestionText())
                        .option1(question.getOption1())
                        .option2(question.getOption2())
                        .option3(question.getOption3())
                        .option4(question.getOption4())
                        .score(question.getScore())
                        .build()
                )
                .toList();
    }

    // 카테고리별 합격 기준 점수 가져오기 (기본값 80)
    private int getPassScore(String categoryName) {
        return quizGenerationRuleRepository
                .findFirstByCategoryNameAndIsActiveTrueOrderByRuleIdDesc(categoryName)
                .map(rule -> rule.getPassScore() != null ? rule.getPassScore() : 80)
                .orElse(80);
    }

    // 채점 및 결과 저장
    // 객관식: 정답 번호 비교로 자동 채점
    // 문항별 결과 QUIZ_RESULT 테이블에 저장 -> 카테고리 기준 총점 및 만점 계산 후 통과 여부 반환
    @Transactional
    public EvaluationSubmitResponse submitQuiz(EvaluationSubmitRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new RuntimeException("제출된 답안이 없습니다.");
        }

        EmpEntity emp = empRepository.findByEmpNo(request.getEmpNo())
                .orElseThrow(() -> new RuntimeException("사원 정보가 없습니다."));

        List<QuizResultEntity> existingResults = resultRepository.findByEmployee_EmpNoAndQuestion_CategoryName(
                request.getEmpNo(),
                request.getCategoryName()
        );

        if (!existingResults.isEmpty()) {
            int previousTotalScore = existingResults.stream()
                    .mapToInt(result -> result.getScore() == null ? 0 : result.getScore())
                    .sum();

            int previousMaxScore = existingResults.stream()
                    .mapToInt(result -> result.getQuestion() != null && result.getQuestion().getScore() != null
                            ? result.getQuestion().getScore()
                            : 0)
                    .sum();

            int passScore = getPassScore(request.getCategoryName());
            boolean previouslyPassed = previousMaxScore > 0 && previousTotalScore >= (previousMaxScore * (passScore / 100.0));

            if (previouslyPassed) {
                throw new RuntimeException("이미 제출한 평가입니다.");
            }

            resultRepository.deleteByEmployee_EmpNoAndQuestion_CategoryName(
                    request.getEmpNo(),
                    request.getCategoryName()
            );
        }

        int totalScore = 0;
        int maxScore = 0;

        List<EvaluationAnswerResult> results = new ArrayList<>();

        for (EvaluationAnswerRequest answer : request.getAnswers()) {
            QuizQuestionEntity question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("문항 정보가 없습니다."));

            if (!question.getCategoryName().equals(request.getCategoryName())) {
                throw new RuntimeException("요청한 카테고리와 문항 카테고리가 일치하지 않습니다.");
            }

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE
                    && answer.getSelectedNo() == null) {
                throw new RuntimeException("객관식 문항에 선택 번호가 없습니다.");
            }

            Boolean isCorrect = null;
            Integer score = 0;
            QuizResultEntity result;

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                isCorrect = question.getAnswerNo().equals(answer.getSelectedNo());
                score = Boolean.TRUE.equals(isCorrect) ? question.getScore() : 0;

                result = QuizResultEntity.builder()
                        .employee(emp)
                        .question(question)
                        .selectedNo(answer.getSelectedNo())
                        .answerText(null)
                        .isCorrect(isCorrect)
                        .score(score)
                        .submittedAt(LocalDateTime.now())
                        .build();
            } else if (question.getQuestionType() == QuestionType.SHORT_ANSWER
                    || question.getQuestionType() == QuestionType.ESSAY) {

                isCorrect = null;
                score = 0;

                try {
                    String url = "http://localhost:8000/api/ai/evaluation/evaluate";

                    Map<String, String> body = Map.of(
                            "user_answer", answer.getAnswerText(),
                            "reference_answer", question.getSampleAnswer()
                    );

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<Map<String, String>> requestEntity =
                            new HttpEntity<>(body, headers);

                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            url,
                            requestEntity,
                            Map.class
                    );

                    Map res = response.getBody();

                    Double aiScore = ((Number) res.get("score")).doubleValue();
                    String aiFeedback = (String) res.get("feedback");
                    Double similarity = ((Number) res.get("similarity")).doubleValue();

                    int questionMaxScore = question.getScore() != null ? question.getScore() : 0;
                    double normalizedRatio = Math.max(0.0, Math.min(aiScore, 100.0)) / 100.0;
                    score = (int) Math.round(normalizedRatio * questionMaxScore);

                    result = QuizResultEntity.builder()
                            .employee(emp)
                            .question(question)
                            .selectedNo(answer.getSelectedNo())
                            .answerText(answer.getAnswerText())
                            .isCorrect(null)
                            .score(score)
                            .aiScore(BigDecimal.valueOf(aiScore))
                            .aiFeedback(aiFeedback)
                            .similarityScore(BigDecimal.valueOf(similarity))
                            .submittedAt(LocalDateTime.now())
                            .build();
                } catch (Exception e) {
                    System.out.println("AI 서버 호출 실패: " + e.getMessage());

                    result = QuizResultEntity.builder()
                            .employee(emp)
                            .question(question)
                            .selectedNo(answer.getSelectedNo())
                            .answerText(answer.getAnswerText())
                            .isCorrect(null)
                            .score(0)
                            .submittedAt(LocalDateTime.now())
                            .build();
                }

            } else {
                throw new RuntimeException("지원하지 않는 문항 유형입니다.");
            }

            totalScore += score;
            maxScore += question.getScore();

            resultRepository.save(result);

            results.add(EvaluationAnswerResult.builder()
                    .questionId(question.getQuestionId())
                    .isCorrect(isCorrect)
                    .score(score)
                    .explanation(question.getExplanation())
                    .aiScore(result.getAiScore())
                    .similarityScore(result.getSimilarityScore())
                    .aiFeedback(result.getAiFeedback())
                    .build());

        }

        int passScore = getPassScore(request.getCategoryName());
        boolean passed = totalScore >= (maxScore * (passScore / 100.0));
        sendQuizSubmitNotification(request.getEmpNo(), request.getCategoryName(), totalScore, maxScore, passed);

        return EvaluationSubmitResponse.builder()
                .empNo(request.getEmpNo())
                .categoryName(request.getCategoryName())
                .totalScore(totalScore)
                .maxScore(maxScore)
                .passed(passed)
                .results(results)
                .build();
    }

    // 퀴즈 응답 제출 후 결과 알림 생성
    private void sendQuizSubmitNotification(String empNo, String categoryName, int totalScore, int maxScore, boolean passed) {
        String encodedCategory = URLEncoder.encode(categoryName, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "/evaluation/result/detail/" + empNo + "/" + encodedCategory;
        String resultText = passed ? "통과" : "미통과";

        notificationService.sendNotification(
                empNo,
                "EVALUATION",
                "평가 제출 완료",
                categoryName + " 평가가 제출되었습니다. 결과: " + resultText + " (" + totalScore + "/" + maxScore + "점)",
                url
        );
    }

    // 사번 기준으로 카테고리별 퀴즈 평가 결과 조회
    // @param empNo 사번
    // @return 카테고리별 평가 결과 리스트
    public List<EvaluationCategoryResultResponse> getEvaluationResults(String empNo) {
        // 1. 해당 사원의 전체 퀴즈 결과 조회
        List<QuizResultEntity> results = resultRepository.findByEmployee_EmpNo(empNo);

        // 2. 카테고리 기준으로 결과 그룹화
        Map<String, List<QuizResultEntity>> grouped = results.stream()
                // 문제 정보 없는 경우 제외
                .filter(r -> r.getQuestion() != null)
                // 카테고리 없는 경우 제외
                .filter(r -> r.getQuestion().getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getQuestion().getCategoryName()
                ));

        // 3. 카테고리별 점수 집계 및 결과 생성
        return grouped.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<QuizResultEntity> list = entry.getValue();

                    // 3-1. 총점 계산(사용자 획득 점수)
                    int totalScore = list.stream()
                            .mapToInt(r -> r.getScore() == null ? 0 : r.getScore())
                            .sum();

                    // 3-2. 만점 계산(문항 점수 기준)
                    int maxScore = list.stream()
                            .mapToInt(r -> r.getQuestion().getScore() == null ? 0 : r.getQuestion().getScore())
                            .sum();

                    // 3-3. 통과 여부 판단(80점 기준)
                    int passScore = getPassScore(category);
                    boolean passed = maxScore > 0 && totalScore >= maxScore * (passScore / 100.0);

                    // 3-4. 결과 DTO 생성
                    return EvaluationCategoryResultResponse.builder()
                            .categoryName(category)
                            .totalScore(totalScore)
                            .maxScore(maxScore)
                            .submitted(true)
                            .passed(passed)
                            .build();
                })
                .toList();
    }

    /**
     * 카테고리별 퀴즈 상세 결과 조회
     * - 특정 사원이 응시한 카테고리의 모든 문항과 본인의 답변, 정답 여부를 반환
     * @param empNo 사번
     * @param categoryName 카테고리명
     * @return 상세 결과 응답 DTO
     */
    public EvaluationDetailResponse getEvaluationDetail(String empNo, String categoryName) {
        // 1. 해당 사원의 특정 카테고리 퀴즈 응답 결과 리스트 조회
        List<QuizResultEntity> results = resultRepository.findByEmployee_EmpNoAndQuestion_CategoryName(empNo, categoryName);

        if(results.isEmpty()) {
            throw new RuntimeException("해당 카테고리의 평가 내역을 찾을 수 없습니다.");
        }

        // 2. 문항별 상세 정보 매핑
        List<EvaluationDetailResponse.QuestionDetail> questionDetails = results.stream()
                .map(result -> {
                    QuizQuestionEntity question = result.getQuestion();

                    // 답변 형식 처리(객관식: 선택번호 / 주관식: 입력텍스트)
                    String userAnswer = (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE)
                            ? formatMultipleChoiceAnswer(question, result.getSelectedNo(), "미선택")
                            : result.getAnswerText();

                    // 정답 형식 처리(객관식: 정답번호 / 주관식: 예시답안)
                    String correctAnswer = (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE)
                            ? formatMultipleChoiceAnswer(question, question.getAnswerNo(), "정답 없음")
                            : question.getSampleAnswer();

                    return EvaluationDetailResponse.QuestionDetail.builder()
                            .questionId(question.getQuestionId())
                            .questionText(question.getQuestionText())
                            .userAnswer(userAnswer)
                            .correctAnswer(correctAnswer)
                            .isCorrect(result.getIsCorrect())
                            .score(result.getScore())
                            .aiScore(result.getAiScore())
                            .aiFeedback(result.getAiFeedback())
                            .similarityScore(result.getSimilarityScore())
                            .build();
                })
                .collect(Collectors.toList());

        int totalScore = results.stream()
                .mapToInt(result -> result.getScore() == null ? 0 : result.getScore())
                .sum();

        int maxScore = results.stream()
                .mapToInt(result -> result.getQuestion() != null && result.getQuestion().getScore() != null
                        ? result.getQuestion().getScore()
                        : 0)
                .sum();

        int passScore = getPassScore(categoryName);
        boolean passed = maxScore > 0 && totalScore >= maxScore * (passScore / 100.0);

        // 3. 최종 DTO 생성 및 반환
        return EvaluationDetailResponse.builder()
                .categoryName(categoryName)
                .empNo(empNo)
                .totalScore(totalScore)
                .maxScore(maxScore)
                .passed(passed)
                .questions(questionDetails)
                .build();
    }

    private String formatMultipleChoiceAnswer(QuizQuestionEntity question, Integer answerNo, String emptyLabel) {
        if (answerNo == null) {
            return emptyLabel;
        }

        String optionText = switch (answerNo) {
            case 1 -> question.getOption1();
            case 2 -> question.getOption2();
            case 3 -> question.getOption3();
            case 4 -> question.getOption4();
            default -> null;
        };

        if (optionText == null || optionText.isBlank()) {
            return String.valueOf(answerNo);
        }

        return answerNo + ". " + optionText;
    }
}
