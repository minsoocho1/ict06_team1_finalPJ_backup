/**
 * @FileName : RoadProgressServiceImpl.java
 * @Description : AI 온보딩 학습 진행률 Service
 *                학습 완료 요청 시 ROAD_PROGRESS 데이터를 생성하거나 수정
 * @Author : 김다솜
 * @Date : 2026. 04. 29
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.29    김다솜        최초 생성/학습 완료 처리 및 진행률 저장 로직 구현
 * @ 2026.05.10    김다솜        로드맵 목록 즉시 반영을 위한 학습 완료 취소 처리 추가
 * @ 2026.05.14    김다솜        복구 로직 N+1 쿼리 최적화 및 학습 완료 시 체크리스트 자동 완료 연동 추가
 */

package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadProgressServiceImpl {

    private final RoadProgressRepository progressRepository;
    private final EmpRepository empRepository;
    private final RoadItemRepository itemRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistProgressRepository checklistProgressRepository;

    // 학습 항목 완료 상태 저장 및 연관 체크리스트 자동 완료 처리
    @Transactional
    public void completeLearning(String empNo, Integer itemId) {

        //1. 사원 조회
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("사원 없음"));

        //2. 아이템 조회
        RoadItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("아이템 없음"));

        //3. 기존 진행 데이터 조회
        var existing = progressRepository
                .findByEmployee_EmpNoAndItem_ItemId(empNo, itemId);

        if(existing.isPresent()) {
            //4-1. 있으면 update
            existing.get().updateProgress(
                    ProgressStatus.COMPLETED,
                    BigDecimal.valueOf(100)
            );
        } else {
            //4-2. 없으면 insert
            RoadProgressEntity progress = RoadProgressEntity.builder()
                    .employee(emp)
                    .item(item)
                    .status(ProgressStatus.COMPLETED)
                    .rate(BigDecimal.valueOf(100))
                    .build();

            progressRepository.save(progress);
        }

        // 5. 연관된 체크리스트 자동 완료 처리 (학습하기 완료 연동)
        syncChecklistStatus(emp, item.getContent().getContentId(), true);
    }

    // 학습 항목 완료 취소(미시작 상태로 변경) 및 연관 체크리스트 동기화 해제
    @Transactional
    public void uncompleteLearning(String empNo, Integer itemId) {
        //1. 사원 조회
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("사원 없음"));

        //2. 아이템 조회
        RoadItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("아이템 없음"));

        //3. 기존 진행 데이터 조회
        var existing = progressRepository
                .findByEmployee_EmpNoAndItem_ItemId(empNo, itemId);

        if(existing.isPresent()) {
            existing.get().updateProgress(
                    ProgressStatus.NOT_STARTED,
                    BigDecimal.ZERO
            );
        } else {
            RoadProgressEntity progress = RoadProgressEntity.builder()
                    .employee(emp)
                    .item(item)
                    .status(ProgressStatus.NOT_STARTED)
                    .rate(BigDecimal.ZERO)
                    .build();

            progressRepository.save(progress);
        }

        // 5. 연관된 체크리스트 자동 미완료 처리 (연동 취소)
        syncChecklistStatus(emp, item.getContent().getContentId(), false);
    }

    // 학습 완료 여부에 따른 체크리스트 진행 정보(ChecklistProgress) 상태 동기화 반영
    private void syncChecklistStatus(EmpEntity emp, Integer contentId, boolean isComplete) {
        checklistRepository.findByRelatedContent_ContentId(contentId)
                .ifPresent(checklist -> {
                    var progressOpt = checklistProgressRepository.findByEmployee_EmpNoAndChecklist_ChecklistId(emp.getEmpNo(), checklist.getChecklistId());
                    
                    if (isComplete) {
                        progressOpt.ifPresentOrElse(
                                ChecklistProgressEntity::complete,
                                () -> {
                                    ChecklistProgressEntity cp = ChecklistProgressEntity.builder()
                                            .employee(emp)
                                            .checklist(checklist)
                                            .status(ProgressStatus.COMPLETED)
                                            .completedAt(LocalDateTime.now())
                                            .build();
                                    checklistProgressRepository.save(cp);
                                }
                        );
                    } else {
                        progressOpt.ifPresent(ChecklistProgressEntity::uncomplete);
                    }
                });
    }

    // 로드맵 재생성 시 기존 학습 이력을 새 아이템으로 이관 및 상태 복구 (N+1 최적화 적용)
    @Transactional
    public void restoreProgress(List<RoadItemEntity> newItems, List<RoadProgressEntity> oldProgressList) {
        if (oldProgressList == null || oldProgressList.isEmpty() || newItems == null || newItems.isEmpty()) return;

        // 1. 필터링 및 맵핑: 필수 콘텐츠거나, 진행 상태가 '시작 전'이 아닌 경우만 백업
        Map<String, RoadProgressEntity> progressMap = oldProgressList.stream()
                .filter(p -> p.getItem() != null && p.getItem().getContent() != null)
                .filter(p -> {
                    boolean isMandatory = Boolean.TRUE.equals(p.getItem().getContent().getIsMandatory());
                    boolean isStarted = p.getStatus() != ProgressStatus.NOT_STARTED;
                    return isMandatory || isStarted; // 필수이거나 진행 중/완료인 것만 유지
                })
                .collect(Collectors.toMap(
                        p -> p.getItem().getContent().getTitle(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        // 2. 신규 아이템들에 대한 빈 진행 기록을 한 번에 조회 (N+1 방지)
        String empNo = newItems.get(0).getRoadmap().getEmployee().getEmpNo();
        List<Integer> newItemIds = newItems.stream().map(RoadItemEntity::getItemId).toList();

        Map<Integer, RoadProgressEntity> newProgressMap = progressRepository.findByEmployee_EmpNo(empNo).stream()
                .filter(p -> newItemIds.contains(p.getItem().getItemId()))
                .collect(Collectors.toMap(
                        p -> p.getItem().getItemId(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        // 3. 신규 아이템들을 순회하며 기존 데이터가 있는 경우 상태 업데이트
        for (RoadItemEntity newItem : newItems) {
            Integer contentId = newItem.getContent().getContentId();
            String contentTitle = newItem.getContent().getTitle();
            if (progressMap.containsKey(contentTitle)) {
                RoadProgressEntity oldData = progressMap.get(contentTitle);

                RoadProgressEntity newProgress = newProgressMap.get(newItem.getItemId());
                if (newProgress != null) {
                    newProgress.updateProgress(oldData.getStatus(), oldData.getRate());
                    progressRepository.save(newProgress);

                    // 체크리스트 진행 내역 복구: 기존 상태가 완료였다면 체크리스트도 다시 동기화
                    if (oldData.getStatus() == ProgressStatus.COMPLETED) {
                        syncChecklistStatus(newProgress.getEmployee(), contentId, true);
                    }
                }
            }
        }
    }
}
