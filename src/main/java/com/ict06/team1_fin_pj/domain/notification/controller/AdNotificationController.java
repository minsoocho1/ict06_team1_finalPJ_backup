/**
 * @FileName : AdNotificationController.java
 * @Description : 관리자 알림 API 컨트롤러
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 * @Modification_History
 *
 * @ 수정일자        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜        최초 생성
 * @ 2026.05.19    김다솜        관리자 세션 기반 헤더 알림 조회가 가능하도록 API 경로와 인증 주체 조회 방식 수정
 */
package com.ict06.team1_fin_pj.domain.notification.controller;

import com.ict06.team1_fin_pj.common.dto.notification.AdNotificationResponseDto;
import com.ict06.team1_fin_pj.common.security.PrincipalDetails;
import com.ict06.team1_fin_pj.domain.notification.service.AdNotificationService;
import com.ict06.team1_fin_pj.domain.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdNotificationController {

    private final AdNotificationService adNotificationService;
    private final NotificationServiceImpl notificationService; // 기존 일반 알림 서비스 재사용

    /**
     * 관리자용 실시간 알림 요약 API
     */
    @GetMapping("/summary")
    public ResponseEntity<AdNotificationResponseDto> getSummary(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        if (principal == null || principal.getEmpNo() == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(adNotificationService.getAdminSummary(principal.getEmpNo()));
    }

    /**
     * 관리자 알림 단건 읽음 처리
     */
    @PatchMapping("/{notiId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Integer notiId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        if (principal == null || principal.getEmpNo() == null) {
            return ResponseEntity.status(401).build();
        }

        notificationService.markAsRead(principal.getEmpNo(), notiId);
        return ResponseEntity.ok().build();
    }
}
