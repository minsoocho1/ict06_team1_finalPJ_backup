/**
 * @FileName : OnboardingDashboardService.java
 * @Description : 온보딩 대시보드 Service
 *                - 사원별 학습 진행률 집계
 *                - 평가 응시/통과 현황 집계
 *                - 평균 평가 점수 계산
 * @Author : 김다솜
 * @Date : 2026. 05. 06
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.06    김다솜        최초 생성 및 온보딩 대시보드 집계 로직 구현
 * @ 2026.05.08    김다솜        체크리스트 진행률 집계 추가
 * @ 2026.05.14    김다솜        학습 진행률 계산 방식 변경 (카테고리 단위 -> 전체 항목 완료 수 기준)
 */

package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.OnboardingDashboardResponse;
import com.ict06.team1_fin_pj.common.dto.onboarding.CategoryProgressResponse;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadmapRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistRepository;
import com.ict06.team1_fin_pj.domain.evaluation.entity.QuizResultEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingDashboardServiceImpl {

    private final RoadProgressRepository roadProgressRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadItemRepository roadItemRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistProgressRepository checklistProgressRepository;

    // 사번 기준 학습 진행률, 체크리스트 현황, 평가 통계 등을 포함한 종합 대시보드 데이터 산출
    public OnboardingDashboardResponse getDashboard(String empNo) {

        List<RoadItemEntity> roadmapItems = roadmapRepository
                .findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(empNo)
                .map(roadmap -> roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.getRoadmapId()))
                .orElse(List.of());

        List<RoadProgressEntity> progresses =
                roadProgressRepository.findByEmployee_EmpNo(empNo);

        Map<Integer, ProgressStatus> statusByItemId = progresses.stream()
                .filter(progress -> progress.getItem() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getItem().getItemId(),
                        RoadProgressEntity::getStatus,
                        (oldValue, newValue) -> newValue
                ));

        int totalLearningCount = roadmapItems.size();
        int completedLearningCount = (int) roadmapItems.stream()
                .filter(item -> statusByItemId.get(item.getItemId()) == ProgressStatus.COMPLETED)
                .count();

        Map<String, List<RoadItemEntity>> itemsByCategory = roadmapItems.stream()
                .filter(item -> item.getCategoryName() != null)
                .collect(Collectors.groupingBy(RoadItemEntity::getCategoryName));

        List<CategoryProgressResponse> categoryProgresses = itemsByCategory.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    List<RoadItemEntity> categoryItems = entry.getValue();

                    int categoryTotalCount = categoryItems.size();
                    int categoryCompletedCount = (int) categoryItems.stream()
                            .filter(item -> statusByItemId.get(item.getItemId()) == ProgressStatus.COMPLETED)
                            .count();
                    int categoryProgressPercent = categoryTotalCount == 0
                            ? 0
                            : (int) Math.round((categoryCompletedCount * 100.0) / categoryTotalCount);

                    return CategoryProgressResponse.builder()
                            .categoryName(categoryName)
                            .totalLearningCount(categoryTotalCount)
                            .completedLearningCount(categoryCompletedCount)
                            .progressPercent(categoryProgressPercent)
                            .build();
                })
                .sorted(
                        Comparator.comparingInt(CategoryProgressResponse::getProgressPercent).reversed()
                                .thenComparing(CategoryProgressResponse::getCategoryName)
                )
                .toList();

        int totalCategoryCount = itemsByCategory.size();

        int completedCategoryCount = (int) itemsByCategory.values().stream()
                .filter(categoryItems -> categoryItems.stream()
                        .allMatch(item -> statusByItemId.get(item.getItemId()) == ProgressStatus.COMPLETED))
                .count();

        int learningProgressPercent = totalLearningCount == 0
                ? 0
                : (int) Math.round((completedLearningCount * 100.0) / totalLearningCount);

        int totalChecklistCount = (int) checklistRepository.count();
        int completedChecklistCount = (int) checklistProgressRepository
                .countByEmployee_EmpNoAndStatus(empNo, ProgressStatus.COMPLETED);
        int checklistProgressPercent = totalChecklistCount == 0
                ? 0
                : (int) Math.round((completedChecklistCount * 100.0) / totalChecklistCount);

        List<QuizResultEntity> quizResults =
                evaluationResultRepository.findByEmployee_EmpNo(empNo);

        Map<String, List<QuizResultEntity>> groupedByCategory = quizResults.stream()
                .filter(result -> result.getQuestion() != null)
                .filter(result -> result.getQuestion().getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        result -> result.getQuestion().getCategoryName()
                ));

        int submittedEvaluationCount = groupedByCategory.size();

        int passedEvaluationCount = (int) groupedByCategory.values().stream()
                .filter(results -> {
                    int totalScore = results.stream()
                            .mapToInt(result -> result.getScore() == null ? 0 : result.getScore())
                            .sum();

                    int maxScore = results.stream()
                            .mapToInt(result -> result.getQuestion().getScore() == null ? 0 : result.getQuestion().getScore())
                            .sum();

                    return maxScore > 0 && totalScore >= maxScore * 0.8;
                })
                .count();

        int evaluationPassRatePercent = submittedEvaluationCount == 0
                ? 0
                : (int) Math.round((passedEvaluationCount * 100.0) / submittedEvaluationCount);

        return OnboardingDashboardResponse.builder()
                .totalLearningCount(totalLearningCount)
                .completedLearningCount(completedLearningCount)
                .totalChecklistCount(totalChecklistCount)
                .completedChecklistCount(completedChecklistCount)
                .checklistProgressPercent(checklistProgressPercent)
                .totalCategoryCount(totalCategoryCount)
                .completedCategoryCount(completedCategoryCount)
                .learningProgressPercent(learningProgressPercent)
                .submittedEvaluationCount(submittedEvaluationCount)
                .passedEvaluationCount(passedEvaluationCount)
                .evaluationPassRatePercent(evaluationPassRatePercent)
                .categoryProgresses(categoryProgresses)
                .build();
    }
}
