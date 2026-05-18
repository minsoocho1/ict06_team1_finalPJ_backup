/**
 * @FileName : RoadmapServiceImpl.java
 * @Description : AI 온보딩 로드맵 생성 및 추천 로직 담당 서비스
 *                - 사원별 개인화된 온보딩 학습 경로 자동 생성 및 관리
 *                - 콘텐츠 추천 로직과 학습 일정 관리, 캘린더 연동 처리
 * @Author : 김다솜
 * @Date : 2026. 05. 07
 * @Modification_History
 * @
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.07    김다솜        최초 생성 및 로드맵 자동 생성 로직(selectRoadmapContents) 구현
 * @ 2026.05.11    김다솜        로드맵 아이템 수정/삭제 및 일정 관리 연동 보완
 * @ 2026.05.14    김다솜        로드맵 재생성 시 진행 상태 복구, 캘린더 연동 최적화 및 컴파일 오류 수정
 */

package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AdminRoadItemRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.RoadmapGroupResponse;
import com.ict06.team1_fin_pj.common.dto.onboarding.RoadmapItemResponse;
import com.ict06.team1_fin_pj.common.dto.onboarding.RoadmapResponse;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleEntity;
import com.ict06.team1_fin_pj.domain.calendar.entity.ScheduleType;
import com.ict06.team1_fin_pj.domain.calendar.repository.CalendarRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ChecklistEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.GeneratedType;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadProgressEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadmapEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.ChecklistRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.OnContentRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl {

    private final EmpRepository empRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadItemRepository roadItemRepository;
    private final RoadProgressRepository roadProgressRepository;
    private final OnContentRepository onContentRepository;
    private final RoadProgressServiceImpl roadProgressService;
    private final CalendarRepository calendarRepository;
    private final ChecklistRepository checklistRepository;

    @Transactional
    public RoadmapResponse getOrCreateRoadmap(String empNo) {
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        validateRoadmapEligibleEmployee(emp);

        RoadmapEntity roadmap = roadmapRepository
                .findFirstByEmployee_EmpNoOrderByRoadmapIdDesc(empNo)
                .orElseGet(() -> createDefaultRoadmap(emp));

        List<RoadItemEntity> existingItems =
                roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.getRoadmapId());

        if (existingItems.isEmpty()) {
            roadmapRepository.delete(roadmap);
            roadmap = createDefaultRoadmap(emp);
        }

        return buildRoadmapResponse(empNo, roadmap);
    }

    @Transactional
    public RoadmapEntity regenerateRoadmap(String empNo) {
        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        validateRoadmapEligibleEmployee(emp);

        return createDefaultRoadmap(emp);
    }

    @Transactional
    public RoadmapEntity resetAndRegenerateRoadmap(Integer roadmapId) {
        RoadmapEntity roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RuntimeException("Roadmap not found."));

        String empNo = roadmap.getEmployee().getEmpNo();
        validateRoadmapEligibleEmployee(roadmap.getEmployee());

        List<RoadItemEntity> oldItems =
                roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId);
        List<RoadProgressEntity> oldProgress = roadProgressRepository.findByEmployee_EmpNo(empNo);
        List<String> retainedContentTitles = extractRetainedContentTitles(oldItems, oldProgress);

        deleteCalendarSchedules(roadmapId);
        roadProgressRepository.deleteByItem_Roadmap_RoadmapId(roadmapId);
        roadItemRepository.deleteByRoadmap_RoadmapId(roadmapId);
        roadmapRepository.delete(roadmap);
        roadmapRepository.flush();

        EmpEntity emp = empRepository.findByEmpNo(empNo)
                .orElseThrow(() -> new RuntimeException("Employee not found."));

        relinkChecklistContentsByTitle();
        RoadmapEntity newRoadmap = createDefaultRoadmap(emp);
        appendRetainedContents(newRoadmap, emp, retainedContentTitles);
        List<RoadItemEntity> newItems =
                roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(newRoadmap.getRoadmapId());
        roadProgressService.restoreProgress(newItems, oldProgress);

        return newRoadmap;
    }

    /**
     * 로드맵 아이템 삭제 및 연결 일정 제거
     */
    @Transactional
    public void deleteRoadItem(Integer itemId) {
        RoadItemEntity item = roadItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Roadmap item not found."));

        deleteSingleCalendarSchedule(item);
        roadProgressRepository.deleteByItem_ItemId(itemId);
        roadItemRepository.delete(item);
    }

    /**
     * 로드맵 아이템 수정 및 일정 동기화
     */
    @Transactional
    public void updateRoadItem(Integer itemId, AdminRoadItemRequestDto requestDto) {
        RoadItemEntity item = roadItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Roadmap item not found."));

        OnContentEntity content = onContentRepository.findById(requestDto.getContentId())
                .orElseThrow(() -> new RuntimeException("Content not found."));

        item.updateRoadItem(
                content,
                requestDto.getItemTitle(),
                item.getRecommendationReason(),
                requestDto.getCategoryName(),
                requestDto.getOrderNo(),
                requestDto.getStartDate(),
                requestDto.getDueDate()
        );

        roadItemRepository.save(item);
        syncToCalendar(item);
    }

    private RoadmapEntity createDefaultRoadmap(EmpEntity emp) {
        RoadmapEntity roadmap = RoadmapEntity.builder()
                .title(emp.getName() + " Onboarding Roadmap")
                .employee(emp)
                .department(emp.getDepartment())
                .position(emp.getPosition())
                .generatedType(GeneratedType.MANUAL)
                .version(1)
                .isCompleted(false)
                .build();

        RoadmapEntity savedRoadmap = roadmapRepository.save(roadmap);

        List<OnContentEntity> recommendedContents = selectRoadmapContents(emp);
        int orderNo = 1;

        for (OnContentEntity content : recommendedContents) {
            createItem(
                    savedRoadmap,
                    emp,
                    content,
                    resolveCategoryName(content),
                    buildRecommendationReason(content, emp),
                    orderNo++
            );
        }

        return savedRoadmap;
    }

    private List<OnContentEntity> selectRoadmapContents(EmpEntity emp) {
        List<OnContentEntity> allContents = onContentRepository.findAll();
        List<OnContentEntity> eligibleContents = allContents.stream()
                .filter(content -> isEligibleForEmployee(content, emp))
                .toList();

        LinkedHashSet<OnContentEntity> selectedContents = new LinkedHashSet<>();

        eligibleContents.stream()
                .filter(content -> Boolean.TRUE.equals(content.getIsMandatory()))
                .sorted(roadmapContentComparator(emp))
                .forEach(selectedContents::add);

        eligibleContents.stream()
                .filter(content -> matchesEmployeeTarget(content, emp))
                .sorted(roadmapContentComparator(emp))
                .forEach(selectedContents::add);

        eligibleContents.stream()
                .filter(this::isCommonContent)
                .sorted(roadmapContentComparator(emp))
                .forEach(selectedContents::add);

        long mandatoryCount = eligibleContents.stream()
                .filter(content -> Boolean.TRUE.equals(content.getIsMandatory()))
                .count();
        long targetMatchCount = eligibleContents.stream()
                .filter(content -> matchesEmployeeTarget(content, emp))
                .count();
        long commonCount = eligibleContents.stream()
                .filter(this::isCommonContent)
                .count();

        System.out.println("[RoadmapService] 콘텐츠 선택 결과 - 사번: " + emp.getEmpNo()
                + ", 전체: " + allContents.size()
                + ", 사용가능: " + eligibleContents.size()
                + ", 필수: " + mandatoryCount
                + ", 타겟매칭: " + targetMatchCount
                + ", 공통: " + commonCount
                + ", 1차선택: " + selectedContents.size());

        if (!selectedContents.isEmpty()) {
            return selectedContents.stream().toList();
        }

        List<OnContentEntity> fallbackContents = eligibleContents.stream()
                .filter(content -> Boolean.TRUE.equals(content.getIsMandatory()) || isCommonContent(content))
                .sorted(roadmapContentComparator(emp))
                .toList();

        if (!fallbackContents.isEmpty()) {
            System.out.println("[RoadmapService] fallback 적용 - 필수/공통 콘텐츠 반환, 사번: "
                    + emp.getEmpNo() + ", 선택 수: " + fallbackContents.size());
            return fallbackContents;
        }

        List<OnContentEntity> commonContents = eligibleContents.stream()
                .filter(this::isCommonContent)
                .sorted(roadmapContentComparator(emp))
                .toList();

        if (!commonContents.isEmpty()) {
            System.out.println("[RoadmapService] fallback 적용 - 공통 콘텐츠 반환, 사번: "
                    + emp.getEmpNo() + ", 선택 수: " + commonContents.size());
            return commonContents;
        }

        if (!eligibleContents.isEmpty()) {
            System.out.println("[RoadmapService] fallback 적용 - 사용가능 콘텐츠 전체 반환, 사번: "
                    + emp.getEmpNo() + ", 선택 수: " + eligibleContents.size());
            return eligibleContents.stream()
                    .sorted(roadmapContentComparator(emp))
                    .toList();
        }

        List<OnContentEntity> finalFallback = allContents.stream()
                .filter(content -> !isDevelopmentCategory(content) || isDevelopmentTrack(emp))
                .filter(content -> !isDesignCategory(content) || isDesignTrack(emp))
                .sorted(roadmapContentComparator(emp))
                .limit(3)
                .toList();

        System.out.println("[RoadmapService] 최종 fallback 적용 - 비어있는 로드맵 방지, 사번: "
                + emp.getEmpNo() + ", 선택 수: " + finalFallback.size());

        return finalFallback;
    }

    private Comparator<OnContentEntity> roadmapContentComparator(EmpEntity emp) {
        return Comparator
                .comparing((OnContentEntity content) -> !Boolean.TRUE.equals(content.getIsMandatory()))
                .thenComparing(content -> !matchesEmployeeTarget(content, emp))
                .thenComparing(content -> nullToEmpty(content.getCategory()))
                .thenComparing(content -> content.getDifficulty() != null ? content.getDifficulty().ordinal() : 99)
                .thenComparing(OnContentEntity::getContentId);
    }

    private boolean matchesEmployeeTarget(OnContentEntity content, EmpEntity emp) {
        if (matchesEmployeePosition(content, emp)) {
            return true;
        }

        if (matchesEmployeeDepartment(content, emp)) {
            return true;
        }

        String targetPosition = normalize(content.getTargetPosition());
        if (targetPosition.isBlank()) {
            return false;
        }

        List<String> employeeKeywords = new ArrayList<>();
        employeeKeywords.add(emp.getDepartment() != null ? emp.getDepartment().getDeptName() : null);
        employeeKeywords.add(emp.getPosition() != null ? emp.getPosition().getPositionName() : null);
        employeeKeywords.add(emp.getStatus());

        return employeeKeywords.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(keyword -> targetPosition.contains(keyword) || keyword.contains(targetPosition));
    }

    private boolean isEligibleForEmployee(OnContentEntity content, EmpEntity emp) {
        if (!isDevelopmentTrack(emp) && isDevelopmentCategory(content)) {
            return false;
        }

        if (!isDesignTrack(emp) && isDesignCategory(content)) {
            return false;
        }

        return true;
    }

    private boolean isDevelopmentTrack(EmpEntity emp) {
        List<String> employeeKeywords = Arrays.asList(
                emp.getDepartment() != null ? emp.getDepartment().getDeptName() : null,
                emp.getDepartment() != null && emp.getDepartment().getParentDept() != null
                        ? emp.getDepartment().getParentDept().getDeptName()
                        : null,
                emp.getPosition() != null ? emp.getPosition().getPositionName() : null
        );

        return employeeKeywords.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .anyMatch(keyword ->
                        keyword.contains("개발")
                                || keyword.contains("backend")
                                || keyword.contains("frontend")
                                || keyword.contains("백엔드")
                                || keyword.contains("프론트")
                                || keyword.contains("engineer")
                                || keyword.contains("엔지니어")
                );
    }

    private boolean isDevelopmentCategory(OnContentEntity content) {
        return contentMatchesAnyKeyword(
                content,
                List.of("개발", "backend", "frontend", "백엔드", "프론트", "spring", "jpa", "redis", "react", "api")
        );
    }

    private boolean isDesignTrack(EmpEntity emp) {
        List<String> employeeKeywords = Arrays.asList(
                emp.getDepartment() != null ? emp.getDepartment().getDeptName() : null,
                emp.getDepartment() != null && emp.getDepartment().getParentDept() != null
                        ? emp.getDepartment().getParentDept().getDeptName()
                        : null,
                emp.getPosition() != null ? emp.getPosition().getPositionName() : null
        );

        return employeeKeywords.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .anyMatch(keyword ->
                        keyword.contains("디자인")
                                || keyword.contains("design")
                                || keyword.contains("ux")
                                || keyword.contains("ui")
                );
    }

    private boolean isDesignCategory(OnContentEntity content) {
        return contentMatchesAnyKeyword(
                content,
                List.of("디자인", "design", "ux", "ui", "figma", "prototype", "qa")
        );
    }

    private boolean isCommonContent(OnContentEntity content) {
        String category = nullToEmpty(content.getCategory()).trim();
        String targetPosition = nullToEmpty(content.getTargetPosition()).trim();

        return "전사".equals(category) || "공통".equals(targetPosition);
    }

    private boolean matchesEmployeePosition(OnContentEntity content, EmpEntity emp) {
        if (emp.getPosition() == null || content.getTargetPositions().isEmpty()) {
            return false;
        }

        Integer employeePositionId = emp.getPosition().getPositionId();

        return content.getTargetPositions().stream()
                .anyMatch(position -> position.getPositionId().equals(employeePositionId));
    }

    private boolean matchesEmployeeDepartment(OnContentEntity content, EmpEntity emp) {
        if (emp.getDepartment() == null || content.getTargetDepartments().isEmpty()) {
            return false;
        }

        Integer employeeDepartmentId = emp.getDepartment().getDeptId();

        return content.getTargetDepartments().stream()
                .anyMatch(department -> department.getDeptId().equals(employeeDepartmentId));
    }

    private String resolveCategoryName(OnContentEntity content) {
        if (content.getCategory() != null && !content.getCategory().isBlank()) {
            return content.getCategory();
        }

        return Boolean.TRUE.equals(content.getIsMandatory())
                ? "Mandatory Training"
                : "Job Training";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> extractRetainedContentTitles(
            List<RoadItemEntity> oldItems,
            List<RoadProgressEntity> oldProgress
    ) {
        Map<Integer, ProgressStatus> progressStatusMap = oldProgress.stream()
                .filter(progress -> progress.getItem() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getItem().getItemId(),
                        RoadProgressEntity::getStatus,
                        (existing, replacement) -> existing
                ));

        LinkedHashSet<String> retainedTitles = new LinkedHashSet<>();

        for (RoadItemEntity item : oldItems) {
            boolean mandatory = Boolean.TRUE.equals(item.getContent().getIsMandatory());
            ProgressStatus status = progressStatusMap.getOrDefault(item.getItemId(), ProgressStatus.NOT_STARTED);
            boolean started = status == ProgressStatus.IN_PROGRESS || status == ProgressStatus.COMPLETED;

            if (mandatory || started) {
                retainedTitles.add(item.getContent().getTitle());
            }
        }

        return retainedTitles.stream().toList();
    }

    private void appendRetainedContents(RoadmapEntity roadmap, EmpEntity emp, List<String> retainedContentTitles) {
        if (retainedContentTitles == null || retainedContentTitles.isEmpty()) {
            return;
        }

        List<RoadItemEntity> currentItems = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.getRoadmapId());
        LinkedHashSet<String> existingTitles = currentItems.stream()
                .map(item -> item.getContent().getTitle())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, OnContentEntity> contentByTitle = onContentRepository.findAll().stream()
                .collect(Collectors.toMap(OnContentEntity::getTitle, content -> content, (existing, replacement) -> existing));

        int nextOrder = currentItems.size() + 1;
        for (String title : retainedContentTitles) {
            if (existingTitles.contains(title)) {
                continue;
            }

            OnContentEntity content = contentByTitle.get(title);
            if (content == null) {
                continue;
            }

            createItem(
                    roadmap,
                    emp,
                    content,
                    resolveCategoryName(content),
                    buildRecommendationReason(content, emp),
                    nextOrder++
            );
            existingTitles.add(title);
        }
    }

    private void relinkChecklistContentsByTitle() {
        Map<String, OnContentEntity> contentByTitle = onContentRepository.findAll().stream()
                .collect(Collectors.toMap(OnContentEntity::getTitle, content -> content, (existing, replacement) -> existing));

        List<ChecklistEntity> checklists = checklistRepository.findByRelatedContentIsNotNull();
        for (ChecklistEntity checklist : checklists) {
            if (checklist.getRelatedContent() == null) {
                continue;
            }

            String title = checklist.getRelatedContent().getTitle();
            OnContentEntity currentContent = contentByTitle.get(title);
            if (currentContent == null) {
                continue;
            }

            if (!Objects.equals(checklist.getRelatedContent().getContentId(), currentContent.getContentId())) {
                checklist.updateRelatedContent(currentContent);
            }
        }
    }

    private boolean contentMatchesAnyKeyword(OnContentEntity content, List<String> keywords) {
        List<String> contentKeywords = new ArrayList<>();
        contentKeywords.add(content.getCategory());
        contentKeywords.add(content.getSubCategory());
        contentKeywords.add(content.getTargetPosition());
        contentKeywords.add(content.getTitle());
        contentKeywords.addAll(
                content.getTargetDepartments().stream()
                        .map(department -> department != null ? department.getDeptName() : null)
                        .toList()
        );

        List<String> normalizedContentKeywords = contentKeywords.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(keyword -> !keyword.isBlank())
                .toList();

        return keywords.stream()
                .map(this::normalize)
                .anyMatch(expected -> normalizedContentKeywords.stream().anyMatch(value -> value.contains(expected)));
    }

    private void validateRoadmapEligibleEmployee(EmpEntity emp) {
        if (isAdminEmployee(emp)) {
            throw new IllegalStateException("Administrator accounts are not eligible for onboarding roadmaps.");
        }
    }

    private boolean isAdminEmployee(EmpEntity emp) {
        return emp.getRole() != null
                && emp.getRole().getRoleName() != null
                && normalize(emp.getRole().getRoleName()).contains("admin");
    }

    private void createItem(
            RoadmapEntity roadmap,
            EmpEntity emp,
            OnContentEntity content,
            String categoryName,
            String recommendationReason,
            Integer orderNo
    ) {
        LocalDate startDate = LocalDate.now().plusDays((long) (orderNo - 1) * 3);
        LocalDate dueDate = startDate.plusDays(2);

        RoadItemEntity item = RoadItemEntity.builder()
                .roadmap(roadmap)
                .content(content)
                .itemTitle(content.getTitle())
                .recommendationReason(recommendationReason)
                .categoryName(categoryName)
                .orderNo(orderNo)
                .startDate(startDate)
                .dueDate(dueDate)
                .build();

        RoadItemEntity savedItem = roadItemRepository.save(item);

        RoadProgressEntity progress = RoadProgressEntity.builder()
                .employee(emp)
                .item(savedItem)
                .status(ProgressStatus.NOT_STARTED)
                .rate(BigDecimal.ZERO)
                .build();

        roadProgressRepository.save(progress);
        syncToCalendar(savedItem);
    }

    /**
     * 온보딩 학습 일정을 캘린더 일정과 동기화
     */
    public void syncToCalendar(RoadItemEntity item) {
        String empNo = item.getRoadmap().getEmployee().getEmpNo();
        String ridTag = "[RID:" + item.getItemId() + "]";

        ScheduleEntity schedule = calendarRepository.findByCreator_EmpNoAndCategory(empNo, "ONBOARDING").stream()
                .filter(s -> s.getContent() != null && s.getContent().contains(ridTag))
                .findFirst()
                .orElse(null);

        String title = "[온보딩] " + item.getItemTitle();
        String content = item.getRecommendationReason() + "\n\n" + ridTag;

        if (schedule == null) {
            schedule = ScheduleEntity.builder()
                    .title(title)
                    .content(content)
                    .startTime(item.getStartDate().atStartOfDay())
                    .endTime(item.getDueDate().atTime(23, 59, 59))
                    .type(ScheduleType.PERSONAL)
                    .creator(item.getRoadmap().getEmployee())
                    .department(item.getRoadmap().getEmployee().getDepartment())
                    .category("ONBOARDING")
                    .location(null)
                    .isAllDay(true)
                    .isPublic(false)
                    .repeatRule(null)
                    .isDeleted(false)
                    .build();
        } else {
            schedule.updateSchedule(
                    title,
                    content,
                    item.getStartDate().atStartOfDay(),
                    item.getDueDate().atTime(23, 59, 59),
                    schedule.getType() != null ? schedule.getType() : ScheduleType.PERSONAL,
                    item.getRoadmap().getEmployee().getDepartment(),
                    "ONBOARDING",
                    schedule.getLocation(),
                    true,
                    false,
                    schedule.getRepeatRule()
            );
        }

        calendarRepository.save(schedule);
    }

    /**
     * 로드맵 재생성 전 기존 온보딩 일정 전체 삭제
     */
    private void deleteCalendarSchedules(Integer roadmapId) {
        List<RoadItemEntity> items = roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmapId);
        if (items.isEmpty()) {
            return;
        }

        String empNo = items.get(0).getRoadmap().getEmployee().getEmpNo();
        List<ScheduleEntity> onboardingSchedules = calendarRepository.findByCreator_EmpNoAndCategory(empNo, "ONBOARDING");

        for (RoadItemEntity item : items) {
            String ridTag = "[RID:" + item.getItemId() + "]";
            onboardingSchedules.stream()
                    .filter(schedule -> schedule.getContent() != null && schedule.getContent().contains(ridTag))
                    .forEach(calendarRepository::delete);
        }
    }

    /**
     * 단일 로드맵 아이템 일정 삭제
     */
    private void deleteSingleCalendarSchedule(RoadItemEntity item) {
        String empNo = item.getRoadmap().getEmployee().getEmpNo();
        String ridTag = "[RID:" + item.getItemId() + "]";

        calendarRepository.findByCreator_EmpNoAndCategory(empNo, "ONBOARDING").stream()
                .filter(schedule -> schedule.getContent() != null && schedule.getContent().contains(ridTag))
                .forEach(calendarRepository::delete);
    }

    private RoadmapResponse buildRoadmapResponse(String empNo, RoadmapEntity roadmap) {
        List<RoadItemEntity> items =
                roadItemRepository.findByRoadmap_RoadmapIdOrderByOrderNo(roadmap.getRoadmapId());

        List<RoadmapItemResponse> itemResponses = items.stream()
                .map(item -> {
                    RoadProgressEntity progress = roadProgressRepository
                            .findByEmployee_EmpNoAndItem_ItemId(empNo, item.getItemId())
                            .orElse(null);

                    return RoadmapItemResponse.builder()
                            .item_id(item.getItemId())
                            .content_id(item.getContent().getContentId())
                            .item_title(item.getItemTitle())
                            .recommendation_reason(
                                    isReadableRecommendationReason(item.getRecommendationReason())
                                            ? item.getRecommendationReason()
                                            : buildRecommendationReason(item.getContent(), roadmap.getEmployee())
                            )
                            .category_name(item.getCategoryName())
                            .order_no(item.getOrderNo())
                            .start_date(item.getStartDate())
                            .due_date(item.getDueDate())
                            .status(progress != null ? progress.getStatus().name() : "NOT_STARTED")
                            .rate(progress != null ? progress.getRate() : BigDecimal.ZERO)
                            .build();
                })
                .toList();

        Map<String, List<RoadmapItemResponse>> grouped =
                itemResponses.stream().collect(Collectors.groupingBy(RoadmapItemResponse::getCategory_name));

        List<RoadmapGroupResponse> groups = grouped.entrySet().stream()
                .map(entry -> RoadmapGroupResponse.builder()
                        .category_name(entry.getKey())
                        .items(entry.getValue().stream()
                                .sorted(Comparator.comparing(RoadmapItemResponse::getOrder_no))
                                .toList())
                        .build())
                .sorted(Comparator.comparing(group -> group.getItems().get(0).getOrder_no()))
                .toList();

        return RoadmapResponse.builder()
                .recommended_roadmap(groups)
                .build();
    }

    private String buildRecommendationReason(OnContentEntity content, EmpEntity emp) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();

        if (Boolean.TRUE.equals(content.getIsMandatory())) {
            reasons.add("필수 학습 콘텐츠입니다.");
        }

        if (matchesEmployeeDepartment(content, emp) && emp.getDepartment() != null) {
            reasons.add(emp.getDepartment().getDeptName() + " 대상 추천입니다.");
        }

        if (matchesEmployeePosition(content, emp) && emp.getPosition() != null) {
            reasons.add(emp.getPosition().getPositionName() + " 직무와 연관된 콘텐츠입니다.");
        }

        String targetPosition = normalize(content.getTargetPosition());
        if (reasons.isEmpty() && "공통".equals(content.getTargetPosition())) {
            reasons.add("전사 공통 온보딩 과정입니다.");
        } else if (reasons.isEmpty() && !targetPosition.isBlank()) {
            reasons.add(content.getTargetPosition() + " 기준으로 추천된 콘텐츠입니다.");
        }

        if (content.getDifficulty() != null) {
            reasons.add("난이도는 " + content.getDifficulty().name() + " 단계입니다.");
        }

        return String.join(" ", reasons);
    }

    private boolean isReadableRecommendationReason(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return !value.contains("�")
                && !value.contains("??")
                && value.codePoints().filter(Character::isLetterOrDigit).count() >= 4;
    }
}
