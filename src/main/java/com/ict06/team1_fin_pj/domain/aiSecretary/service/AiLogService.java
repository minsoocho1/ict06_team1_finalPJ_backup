/**
 * @FileName : AiLogService.java
 * @Description : AI 사용 로그 Service 구현체
 *                - AI 비서 및 챗봇 사용 로그 저장
 *                - 사용자 요청/AI 응답 메시지 기반 로그 데이터 구성
 *                - 성공 여부, fallback 여부, 처리 시간, 오류 메시지 기록
 *                - 관리자 AI 운영 대시보드 및 권한 차단 로그 조회 데이터 제공
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.12    송혜진       AI 비서/챗봇 사용 로그 저장 구조 정리
 * @ 2026.05.22    송혜진       RAG 권한 차단 및 retrieval trace 연계 기준 반영
 * @ 2026.05.27    송혜진       관리자 AI 대시보드 및 권한 차단 로그 조회 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import java.util.Map;

public interface AiLogService {

    void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );

    void saveChatbotLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    );

    AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );

    AiLogEntity saveChatbotLogAndReturn(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage,
            Map<String, String> permissionDeniedInfo
    );

    void saveAssistantLog(
            AiChatMessageEntity userMessage,
            AiChatMessageEntity aiMessage,
            String feature,
            boolean providerSuccess,
            boolean fallback,
            long durationMs,
            String errorMessage
    );
}
