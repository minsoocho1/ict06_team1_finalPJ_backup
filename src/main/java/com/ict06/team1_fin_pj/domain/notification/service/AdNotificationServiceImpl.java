/**
 * @FileName : AdNotificationServiceImpl.java
 * @Description : 관리자 알림 서비스 구현체 (사용자 알림 로직 재사용)
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 */
package com.ict06.team1_fin_pj.domain.notification.service;

import com.ict06.team1_fin_pj.common.dto.notification.AdNotificationResponseDto;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationEntity;
import com.ict06.team1_fin_pj.domain.notification.entity.NotificationType;
import com.ict06.team1_fin_pj.domain.notification.repository.NotificationRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.ict06.team1_fin_pj.domain.notification.entity.QNotificationEntity.notificationEntity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdNotificationServiceImpl implements AdNotificationService {

    private final NotificationRepository notificationRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public AdNotificationResponseDto getAdminSummary(String adminEmpNo) {
        // 1. 읽지 않은 알림을 최신순으로 전체 조회
        List<NotificationEntity> unreadList = queryFactory
                .selectFrom(notificationEntity)
                .where(notificationEntity.employee.empNo.eq(adminEmpNo)
                        .and(notificationEntity.isRead.isFalse()))
                .orderBy(notificationEntity.createdAt.desc())
                .fetch();

        // 2. 온보딩/AI 카테고리 수 집계
        long aiCount = unreadList.stream()
                .filter(n -> n.getNotiType() == NotificationType.AI || n.getNotiType() == NotificationType.ONBOARDING)
                .count();

        // 3. 전자결재 카테고리 수 집계
        long approvalCount = unreadList.stream()
                .filter(n -> n.getNotiType() == NotificationType.APPROVAL)
                .count();

        // 4. 최근 5건 알림 아이템 변환
        List<AdNotificationResponseDto.NotificationItem> items = unreadList.stream()
                .limit(5)
                .map(this::convertToItem)
                .collect(Collectors.toList());

        return AdNotificationResponseDto.builder()
                .totalCount(unreadList.size())
                .aiCount((int) aiCount)
                .approvalCount((int) approvalCount)
                .items(items)
                .build();
    }

    private AdNotificationResponseDto.NotificationItem convertToItem(NotificationEntity entity) {
        String icon = switch (entity.getNotiType()) {
            case APPROVAL -> "bi-file-earmark-check";
            case AI, ONBOARDING -> "bi-robot";
            default -> "bi-bell";
        };

        return AdNotificationResponseDto.NotificationItem.builder()
                .notiId(entity.getNotiId())
                .title(entity.getTitle())
                .category(entity.getNotiType().getLabel())
                .icon(icon)
                .timeAgo(formatTimeAgo(entity.getCreatedAt()))
                .link(entity.getUrl())
                .build();
    }

    private String formatTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "-";
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return "방금 전";
        if (seconds < 3600) return (seconds / 60) + "분 전";
        if (seconds < 86400) return (seconds / 3600) + "시간 전";
        return (seconds / 86400) + "일 전";
    }
}