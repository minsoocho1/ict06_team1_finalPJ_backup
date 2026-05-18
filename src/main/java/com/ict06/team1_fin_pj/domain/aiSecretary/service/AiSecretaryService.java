/**
 * @FileName : AiSecretaryService.js
 * @Description : 세션/메시지 저장 담당
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 (세션 생성 및 메시지 저장, 목록 조회 메서드 추가)
 * @ 2026.05.05    송혜진        CHATBOT 최근 48시간 내 단일 세션 조회 또는 생성 메서드 추가
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;

import java.util.List;

public interface AiSecretaryService {

    // 채팅 세션 생성
    AiChatSessionEntity createSession(String empNo, SessionType sessionType, String title);

    // ASSISTANT 세션 생성
    AiChatSessionEntity createAssistantSession(String empNo, String title);

    // CHATBOT 최근 48시간 내 단일 세션 조회 또는 생성
    AiChatSessionEntity getOrCreateChatbotSession(String empNo);

    // 채팅 세션 목록 조회
    List<AiChatSessionEntity> getSessionList(String empNo, SessionType sessionType);

    // 채팅 세션 내 메시지 목록 조회
    List<AiChatMessageEntity> getMessageList(Integer sessionId);

    // 채팅 세션 내 메시지 저장
    AiChatMessageEntity saveMessage(Integer sessionId, AiChatMessageEntity message);


}