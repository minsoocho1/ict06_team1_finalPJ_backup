/**
 * @FileName : AdOnboardingNotificationHistoryDto.java
 * @Description : 관리자 온보딩 일정 항목별 알림 발송 이력 DTO
 * @Author : 김다솜
 * @Date : 2026. 05. 18
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.18    김다솜        최초 생성 및 사원-콘텐츠별 알림 발송 이력 표시 데이터 추가
 */
package com.ict06.team1_fin_pj.common.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdOnboardingNotificationHistoryDto {

    private Integer notiId;
    private String title;
    private String content;
    private String url;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
