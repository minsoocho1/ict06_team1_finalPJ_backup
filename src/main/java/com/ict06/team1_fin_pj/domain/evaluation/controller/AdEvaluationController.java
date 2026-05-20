/**
 * @FileName : AdEvaluationController.java
 * @Description : 관리자 온보딩 평가 관리 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 04. 24
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.24    김다솜        최초 생성
 * @ 2026.05.10    김다솜        AI 퀴즈 출제 기준 목록/등록/수정/삭제 기능 추가
 * @ 2026.05.11    김다솜        AI 퀴즈 자동 생성, 평가 문제 목록, 문제 목록 이동 기능 추가
 * @ 2026.05.13    김다솜        평가 문제 수정/삭제 화면 및 DB 반영 기능 추가
 * @ 2026.05.18    김다솜        AI 분석 리포트 화면 이동, LLM 이탈 분석 비동기 API 추가
 */
package com.ict06.team1_fin_pj.domain.evaluation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdQuizGenerationRuleRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuestionRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizGenerationRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AdminQuizSaveRequestDto;
import com.ict06.team1_fin_pj.common.dto.evaluation.AiQuizGenerationResponseDto;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuestionType;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizGenerationRuleEntity;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizQuestionEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationQuestionRepository;
import com.ict06.team1_fin_pj.domain.evaluation.repository.QuizGenerationRuleRepository;
import com.ict06.team1_fin_pj.domain.evaluation.service.AdEvaluationService;
import com.ict06.team1_fin_pj.domain.onboarding.entity.Difficulty;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/evaluation")
@RequiredArgsConstructor
public class AdEvaluationController {

    private final QuizGenerationRuleRepository quizGenerationRuleRepository;
    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final OnContentRepository onContentRepository;
    private final AdEvaluationService adEvaluationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 평가 관리 메인 화면
    @RequestMapping("/main")
    public String evaluationMain(HttpServletRequest request, HttpServletResponse response, Model model)
            throws ServletException, IOException {
        return "admin/evaluation/evaluationMain";
    }

    // AI 퀴즈 출제 기준 목록
    @GetMapping("/quiz-rules")
    public String quizRuleList(Model model) {
        List<QuizGenerationRuleEntity> rules = quizGenerationRuleRepository.findAllByOrderByCategoryNameAscRuleIdAsc();
        List<String> categories = getContentCategories();
        List<String> configuredCategories = rules.stream()
                .map(QuizGenerationRuleEntity::getCategoryName)
                .distinct()
                .sorted()
                .toList();
        List<String> missingCategories = categories.stream()
                .filter(category -> configuredCategories.stream().noneMatch(category::equals))
                .toList();

        model.addAttribute("rules", rules);
        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("configuredRuleCount", rules.size());
        model.addAttribute("activeRuleCount", rules.stream().filter(rule -> rule.getIsActive() != null && rule.getIsActive()).count());
        model.addAttribute("missingCategories", missingCategories);
        return "admin/evaluation/quizRuleList";
    }

    // 평가 문제 목록
    @GetMapping("/questions")
    public String questionList(Model model) {
        model.addAttribute("questions", evaluationQuestionRepository.findAll());
        return "admin/evaluation/questionList";
    }

    // 평가 결과 통계 화면
    @GetMapping("/results")
    public String resultList(Model model) {
        model.addAttribute("analytics", adEvaluationService.getEvaluationAnalytics());
        return "admin/evaluation/resultList";
    }

    // 평가 결과 기반 AI 분석 리포트 화면
    @GetMapping("/ai-report")
    public String aiEvalReport(Model model) {
        model.addAttribute("analytics", adEvaluationService.getEvaluationAnalytics());
        return "admin/evaluation/aiEvalReport";
    }

    @GetMapping("/ai-report/retention-analysis")
    @ResponseBody
    public ResponseEntity<?> aiRetentionAnalysis() {
        var analytics = adEvaluationService.getEvaluationAnalytics();
        String analysis = adEvaluationService.getAiRetentionRiskAnalysis(analytics.getRetentionRiskStats());
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    /**
     * 평가 결과 통계 화면에서 호출하는 평가 기준 수정 API
     */
    @PostMapping("/criteria")
    @ResponseBody
    public ResponseEntity<?> updateCriteria(
            @RequestParam String categoryName,
            @RequestParam Integer passScore,
            @RequestParam Double weight) {
        try {
            adEvaluationService.updateCategoryCriteria(categoryName, passScore, weight);
            return ResponseEntity.ok(Map.of("success", true, "message", "평가 기준을 수정했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * AI 기반 통계 분석 코멘트 요청
     */
    @PostMapping("/ai-analysis")
    @ResponseBody
    public ResponseEntity<?> getAiAnalysis(@RequestBody Map<String, Object> stats) {
        String analysis = adEvaluationService.getAiEvaluationAnalysis(stats);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    // 평가 문제 수정 화면
    @GetMapping("/questions/{questionId}/edit")
    public String editQuestionForm(
            @PathVariable Integer questionId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return evaluationQuestionRepository.findById(questionId)
                .map(question -> {
                    model.addAttribute("question", toQuestionRequestDto(question));
                    model.addAttribute("questionId", questionId);
                    addQuestionFormOptions(model);
                    model.addAttribute("isEdit", true);
                    return "admin/evaluation/questionForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "평가 문제를 찾을 수 없습니다.");
                    return "redirect:/admin/evaluation/questions";
                });
    }

    // 평가 문제 수정 저장
    @PostMapping("/questions/{questionId}/edit")
    public String updateQuestion(
            @PathVariable Integer questionId,
            @ModelAttribute("question") AdminQuestionRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        OnContentEntity content = onContentRepository.findById(requestDto.getContentId()).orElse(null);
        if (content == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "연결 콘텐츠를 찾을 수 없습니다.");
            return "redirect:/admin/evaluation/questions";
        }

        return evaluationQuestionRepository.findById(questionId)
                .map(question -> {
                    question.updateQuestion(
                            content,
                            requestDto.getCategoryName(),
                            requestDto.getQuestionType(),
                            requestDto.getQuestionText(),
                            normalizeBlank(requestDto.getOption1()),
                            normalizeBlank(requestDto.getOption2()),
                            normalizeBlank(requestDto.getOption3()),
                            normalizeBlank(requestDto.getOption4()),
                            requestDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE ? requestDto.getAnswerNo() : null,
                            requestDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE ? null : normalizeBlank(requestDto.getSampleAnswer()),
                            requestDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE ? null : normalizeKeywordAnswer(requestDto.getKeywordAnswerText()),
                            requestDto.getQuestionType() == QuestionType.MULTIPLE_CHOICE ? null : normalizeBlank(requestDto.getRubric()),
                            requestDto.getScore(),
                            normalizeBlank(requestDto.getExplanation())
                    );
                    evaluationQuestionRepository.save(question);
                    redirectAttributes.addFlashAttribute("successMessage", "평가 문제를 수정했습니다.");
                    return "redirect:/admin/evaluation/questions";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "평가 문제를 찾을 수 없습니다.");
                    return "redirect:/admin/evaluation/questions";
                });
    }

    // 평가 문제 삭제
    @PostMapping("/questions/{questionId}/delete")
    public String deleteQuestion(
            @PathVariable Integer questionId,
            RedirectAttributes redirectAttributes
    ) {
        if (!evaluationQuestionRepository.existsById(questionId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "평가 문제를 찾을 수 없습니다.");
            return "redirect:/admin/evaluation/questions";
        }

        evaluationQuestionRepository.deleteById(questionId);
        redirectAttributes.addFlashAttribute("successMessage", "평가 문제를 삭제했습니다.");
        return "redirect:/admin/evaluation/questions";
    }

    // AI 퀴즈 자동 생성 화면
    @GetMapping("/quiz-generator")
    public String quizGeneratorForm(Model model) {
        model.addAttribute("requestDto", new AdminQuizGenerationRequestDto());
        addQuizGeneratorOptions(model);
        return "admin/evaluation/quizGeneratorForm";
    }

    // AI 퀴즈 초안 생성
    @PostMapping("/quiz-generator/preview")
    public String previewGeneratedQuiz(
            @ModelAttribute("requestDto") AdminQuizGenerationRequestDto requestDto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            AiQuizGenerationResponseDto response = adEvaluationService.generateQuizDrafts(requestDto);
            model.addAttribute("generatedQuestions", response.getQuestions());
            model.addAttribute("generatedQuestionsJson", objectMapper.writeValueAsString(response.getQuestions()));
            model.addAttribute("selectedContent", onContentRepository.findById(requestDto.getContentId()).orElse(null));
            addQuizGeneratorOptions(model);
            return "admin/evaluation/quizGeneratorForm";
        } catch (JsonProcessingException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "생성된 문제 초안을 화면에 표시하는 중 오류가 발생했습니다.");
            return "redirect:/admin/evaluation/quiz-generator";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/evaluation/quiz-generator";
        }
    }

    // 생성된 퀴즈 초안 저장
    @PostMapping("/quiz-generator/save")
    public String saveGeneratedQuiz(
            @ModelAttribute AdminQuizSaveRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int savedCount = adEvaluationService.saveGeneratedQuestions(requestDto);
            redirectAttributes.addFlashAttribute("successMessage", savedCount + "개의 문제 초안을 저장했습니다.");
            return "redirect:/admin/evaluation/questions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/evaluation/quiz-generator";
        }
    }

    // AI 퀴즈 출제 기준 등록 화면
    @GetMapping("/quiz-rules/new")
    public String quizRuleForm(Model model) {
        model.addAttribute("rule", QuizGenerationRuleEntity.builder()
                .questionCount(5)
                .passScore(80)
                .weightPercent(100)
                .difficulty(Difficulty.EASY)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .isActive(true)
                .build());
        addQuizRuleFormOptions(model);
        model.addAttribute("isEdit", false);
        return "admin/evaluation/quizRuleForm";
    }

    // AI 퀴즈 출제 기준 등록
    @PostMapping("/quiz-rules")
    public String createQuizRule(
            @ModelAttribute AdQuizGenerationRuleRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        QuizGenerationRuleEntity rule = QuizGenerationRuleEntity.builder()
                .categoryName(requestDto.getCategoryName())
                .questionCount(requestDto.getQuestionCount())
                .passScore(requestDto.getPassScore())
                .weightPercent(requestDto.getWeightPercent())
                .difficulty(requestDto.getDifficulty())
                .questionType(requestDto.getQuestionType())
                .isActive(Boolean.TRUE.equals(requestDto.getIsActive()))
                .build();

        quizGenerationRuleRepository.save(rule);
        redirectAttributes.addFlashAttribute("successMessage", "AI 퀴즈 출제 기준을 등록했습니다.");
        return "redirect:/admin/evaluation/quiz-rules";
    }

    // AI 퀴즈 출제 기준 수정 화면
    @GetMapping("/quiz-rules/{ruleId}/edit")
    public String editQuizRuleForm(
            @PathVariable Integer ruleId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return quizGenerationRuleRepository.findById(ruleId)
                .map(rule -> {
                    model.addAttribute("rule", rule);
                    addQuizRuleFormOptions(model);
                    model.addAttribute("isEdit", true);
                    return "admin/evaluation/quizRuleForm";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "AI 퀴즈 출제 기준을 찾을 수 없습니다.");
                    return "redirect:/admin/evaluation/quiz-rules";
                });
    }

    // AI 퀴즈 출제 기준 수정
    @PostMapping("/quiz-rules/{ruleId}/edit")
    public String updateQuizRule(
            @PathVariable Integer ruleId,
            @ModelAttribute AdQuizGenerationRuleRequestDto requestDto,
            RedirectAttributes redirectAttributes
    ) {
        return quizGenerationRuleRepository.findById(ruleId)
                .map(rule -> {
                    rule.updateRule(
                            requestDto.getCategoryName(),
                            requestDto.getQuestionCount(),
                            requestDto.getPassScore(),
                            requestDto.getWeightPercent(),
                            requestDto.getDifficulty(),
                            requestDto.getQuestionType(),
                            requestDto.getIsActive()
                    );
                    quizGenerationRuleRepository.save(rule);
                    redirectAttributes.addFlashAttribute("successMessage", "AI 퀴즈 출제 기준을 수정했습니다.");
                    return "redirect:/admin/evaluation/quiz-rules";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "AI 퀴즈 출제 기준을 찾을 수 없습니다.");
                    return "redirect:/admin/evaluation/quiz-rules";
                });
    }

    // AI 퀴즈 출제 기준 삭제
    @PostMapping("/quiz-rules/{ruleId}/delete")
    public String deleteQuizRule(
            @PathVariable Integer ruleId,
            RedirectAttributes redirectAttributes
    ) {
        if (!quizGenerationRuleRepository.existsById(ruleId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "AI 퀴즈 출제 기준을 찾을 수 없습니다.");
            return "redirect:/admin/evaluation/quiz-rules";
        }

        quizGenerationRuleRepository.deleteById(ruleId);
        redirectAttributes.addFlashAttribute("successMessage", "AI 퀴즈 출제 기준을 삭제했습니다.");
        return "redirect:/admin/evaluation/quiz-rules";
    }

    // 출제 기준 화면 옵션 구성
    private void addQuizRuleFormOptions(Model model) {
        model.addAttribute("categories", getContentCategories());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());
    }

    // 자동 생성 화면 옵션 구성
    private void addQuizGeneratorOptions(Model model) {
        model.addAttribute("contents", onContentRepository.findAll());
        model.addAttribute("difficulties", Difficulty.values());
    }

    // 평가 문제 수정 화면 옵션 구성
    private void addQuestionFormOptions(Model model) {
        model.addAttribute("contents", onContentRepository.findAll());
        model.addAttribute("categories", getContentCategories());
        model.addAttribute("questionTypes", QuestionType.values());
    }

    // 평가 문제 엔티티를 수정 DTO로 변환
    private AdminQuestionRequestDto toQuestionRequestDto(QuizQuestionEntity question) {
        AdminQuestionRequestDto dto = new AdminQuestionRequestDto();
        dto.setContentId(question.getContent() != null ? question.getContent().getContentId() : null);
        dto.setCategoryName(question.getCategoryName());
        dto.setQuestionType(question.getQuestionType());
        dto.setQuestionText(question.getQuestionText());
        dto.setOption1(question.getOption1());
        dto.setOption2(question.getOption2());
        dto.setOption3(question.getOption3());
        dto.setOption4(question.getOption4());
        dto.setAnswerNo(question.getAnswerNo());
        dto.setSampleAnswer(question.getSampleAnswer());
        dto.setKeywordAnswerText(toKeywordAnswerInput(question.getKeywordAnswer()));
        dto.setRubric(question.getRubric());
        dto.setScore(question.getScore());
        dto.setExplanation(question.getExplanation());
        return dto;
    }

    // 키워드 답안 입력값을 JSON 배열 문자열로 변환
    private String normalizeKeywordAnswer(String keywordAnswerText) {
        String value = normalizeBlank(keywordAnswerText);
        if (value == null) {
            return null;
        }

        try {
            if (value.startsWith("[")) {
                List<String> parsed = objectMapper.readValue(value, new TypeReference<List<String>>() {});
                return objectMapper.writeValueAsString(parsed);
            }

            List<String> keywords = new ArrayList<>();
            for (String token : value.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    keywords.add(trimmed);
                }
            }

            return keywords.isEmpty() ? null : objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("키워드 답안 형식이 올바르지 않습니다.");
        }
    }

    // 키워드 답안 JSON 배열을 입력용 문자열로 변환
    private String toKeywordAnswerInput(String keywordAnswer) {
        if (keywordAnswer == null || keywordAnswer.isBlank()) {
            return null;
        }

        try {
            List<String> parsed = objectMapper.readValue(keywordAnswer, new TypeReference<List<String>>() {});
            return String.join(", ", parsed);
        } catch (JsonProcessingException e) {
            return keywordAnswer;
        }
    }

    // 공백 입력값 정리
    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // 콘텐츠 카테고리 목록 조회
    private List<String> getContentCategories() {
        return onContentRepository.findAll().stream()
                .map(OnContentEntity::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
