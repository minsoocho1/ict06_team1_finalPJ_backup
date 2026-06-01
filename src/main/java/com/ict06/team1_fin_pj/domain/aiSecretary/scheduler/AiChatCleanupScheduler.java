/**
 * @FileName : AiChatCleanupScheduler.java
 * @Description : AI 챗봇 대화 정리 스케줄러
 *                - 일정 시간이 지난 CHATBOT 세션 및 메시지 정리
 *                - 챗봇 단기 대화 데이터 보관 정책 적용
 *                - AI 비서 ASSISTANT 세션과 챗봇 CHATBOT 세션 보관 정책 분리
 *                - 장기 분석용 로그와 단기 대화 데이터를 분리하여 관리
 *
 * @Author : 송혜진
 * @Date : 2026. 05. 12
 * @Modification_History
 * @
 * @ 수정일       수정자       수정내용
 * @ ----------  ---------   ----------------------------------------
 * @ 2026.05.12  송혜진       최초 생성
 * @ 2026.05.28  송혜진       CHATBOT 단기 보관 및 세션 정리 정책 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.scheduler;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatCleanupScheduler {

    private static final long CHATBOT_RETENTION_HOURS = 48L;

    private final AiChatSessionRepository aiChatSessionRepository;

    // 매일 새벽 3시에 48시간 지난 CHATBOT 세션 삭제
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredChatbotSessions() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(CHATBOT_RETENTION_HOURS);

        List<AiChatSessionEntity> expiredSessions =
                aiChatSessionRepository.findBySessionTypeAndLastMessageAtBefore(
                        SessionType.CHATBOT,
                        cutoff
                );

        if (expiredSessions.isEmpty()) {
            log.info("[AI CHATBOT CLEANUP] 삭제 대상 없음. cutoff={}", cutoff);
            return;
        }

        // deleteAllInBatch 사용 금지.
        aiChatSessionRepository.deleteAll(expiredSessions);

        log.info(
                "[AI CHATBOT CLEANUP] 만료 CHATBOT 세션 삭제 완료. count={}, cutoff={}",
                expiredSessions.size(),
                cutoff
        );
    }
}