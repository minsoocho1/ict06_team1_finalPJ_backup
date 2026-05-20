/**
 * @FileName : NotificationType.java
 * @Description : 알림 유형 Enum
 * @Author : 김다솜
 * @Date : 2026. 05. 10
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.10    김다솜        평가 알림 및 업무별 알림 유형 표시값 관리
 * @ 2026.05.12    김다솜        온보딩 일정 알림 유형 추가
 */
package com.ict06.team1_fin_pj.domain.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    APPROVAL("결재"),
    ATTENDANCE("근태"),
    AI("AI"),
    EVALUATION("평가"),
    ONBOARDING("온보딩"),
    NOTICE("공지"),
    MYPAGE("정보 수정");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }
}