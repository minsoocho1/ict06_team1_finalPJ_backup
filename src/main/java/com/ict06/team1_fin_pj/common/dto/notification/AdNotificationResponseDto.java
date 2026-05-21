/**
 * @FileName : AdNotificationResponseDto.java
 * @Description : 관리자 알림 요약 정보 응답 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 */
package com.ict06.team1_fin_pj.common.dto.notification;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AdNotificationResponseDto {
    private int totalCount;    // 전체 미읽음 알림 수
    private int aiCount;       // 온보딩/AI 관련 알림 수
    private int approvalCount; // 전자결재 관련 알림 수
    
    private List<NotificationItem> items; // 최근 알림 5개 목록

    @Getter
    @Builder
    public static class NotificationItem {
        private Integer notiId;
        private String title;
        private String category;
        private String icon;
        private String timeAgo;
        private String link;
    }
}