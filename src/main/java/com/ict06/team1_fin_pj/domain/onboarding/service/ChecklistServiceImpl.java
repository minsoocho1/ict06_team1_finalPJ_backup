/**
 * @FileName : ChecklistServiceImpl.java
 * @Description : AI 온보딩 체크리스트 Service
 *                체크리스트 조회 및 완료 처리 로직 담당
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성/체크리스트 조회 및 완료 처리 로직 구현
 * @ 2026.05.14    김다솜        사원별 로드맵 콘텐츠 기반 체크리스트 동적 필터링 구현
 * @ 2026.05.15    김다솜        학습 콘텐츠 연결 항목의 직접 체크 제한 및 평가 완료 항목 되돌리기 차단
 */

package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.ChecklistResponse;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.evaluation.repository.EvaluationResultRepository;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
public class ChecklistServiceImpl {

    private final ChecklistRepository checklistRepository;
    private final ChecklistProgressRepository progressRepository;
    private final EmpRepository empRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadItemRepository roadItemRepository;
    private final EvaluationResultRepository evaluationResultRepository;

    // 사원별 체크리스트 목록 조회 (로드맵 기반 자동 생성/필터링 로직 추가)
    public List<ChecklistResponse> getChecklist(String empNo) {
        // 1. 해당 사원의 로드맵 아이템에 포함된 콘텐츠 ID 목록 추출
        List<Integer> roadmapContentIds = roadmapRepository.findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(empNo)
                .map(roadmap -> roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.getRoadmapId()))
                .orElse(List.of())
                .stream()
                .map(item -> item.getContent().getContentId())
                .collect(Collectors.toList());

        // 2. 전체 체크리스트 조회
        List<ChecklistEntity> allChecklist = checklistRepository.findAllByOrderByOrderNoAsc();

        // 3. 로드맵 기반 필터링:
        // - 'USER' 타입: 학습과 무관한 기본 필수 체크리스트 (무조건 포함)
        // - 'SYSTEM' 타입: 특정 학습 콘텐츠 완료 시 자동 체크되는 항목 (내 로드맵에 해당 콘텐츠가 있을 때만 포함)
        List<ChecklistEntity> filteredChecklist = allChecklist.stream()
                .filter(item -> "USER".equals(item.getChecklistType()) ||
                               (item.getRelatedContent() != null && roadmapContentIds.contains(item.getRelatedContent().getContentId())))
                .collect(Collectors.toList());

        Map<Integer, ChecklistProgressEntity> progressMap =
                progressRepository.findByEmployee_EmpNo(empNo)
                        .stream()
                        .collect(Collectors.toMap(
                                progress -> progress.getChecklist().getChecklistId(),
                                progress -> progress
                        ));

        return filteredChecklist.stream()
                .map(item -> {
                    ChecklistProgressEntity progress = progressMap.get(item.getChecklistId());
                    ProgressStatus status = progress != null ? progress.getStatus() : ProgressStatus.NOT_STARTED;
                    String relatedCategory = item.getRelatedContent() != null ? item.getRelatedContent().getCategory() : null;
                    boolean evaluationSubmitted = relatedCategory != null
                            && evaluationResultRepository.existsByEmployee_EmpNoAndQuestion_CategoryName(empNo, relatedCategory);

                    return ChecklistResponse.builder()
                            .checklistId(item.getChecklistId())
                            .title(item.getTitle())
                            .category(item.getCategory())
                            .description(item.getDescription())
                            .isMandatory(item.getIsMandatory())
                            .orderNo(item.getOrderNo())
                            .status(status)
                            //연결된 학습 콘텐츠 정보
                            .relatedContentId(item.getRelatedContent() != null ? item.getRelatedContent().getContentId() : null)
                            .relatedContentTitle(item.getRelatedContent() != null ? item.getRelatedContent().getTitle() : null)
                            .learningCompleted(status == ProgressStatus.COMPLETED)
                            .evaluationSubmitted(evaluationSubmitted)
                            .build();
                })
                .collect(Collectors.toList());
    }

    //체크리스트 완료 처리
    @Transactional
    public void completeChecklist(String empNo, Integer checklistId) {
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("사원 없음"));

        ChecklistEntity checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("체크리스트 없음"));

        var existing = progressRepository
                .findByEmployee_EmpNoAndChecklist_ChecklistId(empNo, checklistId);

        if(existing.isPresent()) {
            existing.get().complete();
        } else {
            ChecklistProgressEntity progress = ChecklistProgressEntity.builder()
                    .employee(emp)
                    .checklist(checklist)
                    .status(ProgressStatus.COMPLETED)
                    .completedAt(now())
                    .build();

            progressRepository.save(progress);
        }
    }

    //체크리스트 미완료 처리
    @Transactional
    public void uncompleteChecklist(String empNo, Integer checklistId) {
        ChecklistEntity checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("체크리스트 없음"));

        String relatedCategory = checklist.getRelatedContent() != null ? checklist.getRelatedContent().getCategory() : null;
        if (relatedCategory != null
                && evaluationResultRepository.existsByEmployee_EmpNoAndQuestion_CategoryName(empNo, relatedCategory)) {
            throw new IllegalStateException("평가를 마친 항목입니다.");
        }

        var existing = progressRepository
                .findByEmployee_EmpNoAndChecklist_ChecklistId(empNo, checklistId);

        existing.ifPresent(ChecklistProgressEntity::uncomplete);
    }

}
