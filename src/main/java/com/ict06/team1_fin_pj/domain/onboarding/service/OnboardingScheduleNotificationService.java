/**
 * @FileName : OnboardingScheduleNotificationService.java
 * @Description : 관리자 온보딩 일정 학습항목 알림 발송 Service
 * @Author : 김다솜
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.12    김다솜        최초 생성 및 학습항목 수동 알림, 시작/마감 1일 전 자동 알림 처리 추가
 * @ 2026.05.14    김다솜        알림 타입 매핑 정렬 및 상수화 처리
 * @ 2026.05.18    김다솜        로드맵 학습항목별 알림 발송 이력 조회 기능 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AdOnboardingNotificationHistoryDto;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationType;
import com.ict06.team1_fin_pj.domain.notification.repository.NotificationRepository;
import com.ict06.team1_fin_pj.domain.notification.service.NotificationServiceImpl;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ProgressStatus;
import com.ict06.team1_fin_pj.domain.onboarding.entity.RoadItemEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadItemRepository;
import com.ict06.team1_fin_pj.domain.onboarding.repository.RoadProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingScheduleNotificationService {

    // NotificationType Enum의 name과 일치시킴
    private static final String NOTIFICATION_TYPE = NotificationType.ONBOARDING.name();

    private final RoadItemRepository roadItemRepository;
    private final RoadProgressRepository roadProgressRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public void sendManualItemNotification(Integer itemId) {
        RoadItemEntity item = roadItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("로드맵 아이템을 찾을 수 없습니다."));

        EmpEntity employee = getEmployee(item);
        if (employee == null) {
            throw new IllegalStateException("알림을 받을 직원을 찾을 수 없습니다.");
        }

        notificationService.sendNotification(
                employee.getEmpNo(),
                NOTIFICATION_TYPE,
                buildManualTitle(item),
                buildManualContent(item),
                buildItemUrl(item)
        );
    }

    @Transactional(readOnly = true)
    public List<AdOnboardingNotificationHistoryDto> getItemNotificationHistories(RoadItemEntity item) {
        EmpEntity employee = getEmployee(item);
        if (employee == null) {
            return List.of();
        }

        return notificationRepository.findByEmployee_EmpNoAndNotiTypeAndUrlOrderByCreatedAtDesc(
                        employee.getEmpNo(),
                        NotificationType.ONBOARDING,
                        buildItemUrl(item)
                ).stream()
                .map(this::toNotificationHistoryDto)
                .toList();
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendScheduleReminderNotifications() {
        LocalDate reminderDate = LocalDate.now().plusDays(1);
        Set<Integer> completedItemIds = roadProgressRepository.findAll().stream()
                .filter(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .map(progress -> progress.getItem().getItemId())
                .collect(Collectors.toSet());

        roadItemRepository.findAllByOrderByRoadmap_Employee_EmpNoAscOrderNoAsc().stream()
                .filter(item -> !completedItemIds.contains(item.getItemId()))
                .forEach(item -> {
                    if (reminderDate.equals(item.getStartDate())) {
                        sendAutomaticNotificationIfAbsent(
                                item,
                                "온보딩 학습 시작 예정",
                                "내일 시작 예정인 온보딩 학습이 있습니다."
                        );
                    }

                    if (reminderDate.equals(item.getDueDate())) {
                        sendAutomaticNotificationIfAbsent(
                                item,
                                "온보딩 학습 마감 예정",
                                "내일 마감 예정인 온보딩 학습이 있습니다."
                        );
                    }
                });
    }

    private void sendAutomaticNotificationIfAbsent(RoadItemEntity item, String title, String prefix) {
        EmpEntity employee = getEmployee(item);
        if (employee == null) {
            return;
        }

        String url = buildItemUrl(item);
        String notificationTitle = title + " - " + safeItemTitle(item);
        if (notificationRepository.existsByEmployee_EmpNoAndNotiTypeAndTitleAndUrl(
                employee.getEmpNo(),
                NotificationType.ONBOARDING,
                notificationTitle,
                url
        )) {
            return;
        }

        notificationService.sendNotification(
                employee.getEmpNo(),
                NOTIFICATION_TYPE,
                notificationTitle,
                prefix + " 학습항목: " + safeItemTitle(item),
                url
        );
    }

    private EmpEntity getEmployee(RoadItemEntity item) {
        return item.getRoadmap() != null ? item.getRoadmap().getEmployee() : null;
    }

    private String buildManualTitle(RoadItemEntity item) {
        return "온보딩 학습 알림 - " + safeItemTitle(item);
    }

    private String buildManualContent(RoadItemEntity item) {
        return "온보딩 학습항목을 확인해 주세요. 기간: "
                + (item.getStartDate() != null ? item.getStartDate() : "-")
                + " ~ "
                + (item.getDueDate() != null ? item.getDueDate() : "-");
    }

    private String buildItemUrl(RoadItemEntity item) {
        if (item.getContent() != null && item.getContent().getContentId() != null) {
            return "/onboarding/learning/" + item.getContent().getContentId();
        }

        return "/onboarding/myroadmap";
    }

    private String safeItemTitle(RoadItemEntity item) {
        if (item.getItemTitle() != null && !item.getItemTitle().isBlank()) {
            return item.getItemTitle();
        }

        return item.getContent() != null ? item.getContent().getTitle() : "온보딩 학습";
    }

    private AdOnboardingNotificationHistoryDto toNotificationHistoryDto(NotificationEntity notification) {
        return AdOnboardingNotificationHistoryDto.builder()
                .notiId(notification.getNotiId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .url(notification.getUrl())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
