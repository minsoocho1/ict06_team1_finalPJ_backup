/**
 * @FileName : AdNotificationService.java
 * @Description : 관리자 알림 서비스 인터페이스
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 */
package com.ict06.team1_fin_pj.domain.notification.service;

import com.ict06.team1_fin_pj.common.dto.notification.AdNotificationResponseDto;

public interface AdNotificationService {
    // 관리자용 실시간 알림 요약 정보 조회
    AdNotificationResponseDto getAdminSummary(String adminEmpNo);
}