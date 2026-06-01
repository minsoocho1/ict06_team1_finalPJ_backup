/**
 * @FileName : AiSecretaryService.java
 * @Description : AI 비서/챗봇 세션 및 메시지 관리 서비스 구현체
 *                - AI 비서 문서 작성 세션 생성 및 조회
 *                - 사내 챗봇 세션 생성 및 최근 세션 재사용
 *                - AI 대화 메시지 저장 및 세션별 메시지 목록 조회
 *                - 챗봇 답변의 참고 문서 references 복원 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성 (세션 생성 및 메시지 저장, 목록 조회 메서드 추가)
 * @ 2026.05.05    송혜진       CHATBOT 최근 48시간 내 단일 세션 조회 또는 생성 메서드 추가
 * @ 2026.05.12    송혜진       ASSISTANT 세션 장기 유지 및 최근 작성 목록 조회 기준 반영
 * @ 2026.05.22    송혜진       챗봇 세션 소유자 empNo 기반 RAG 검색 연동 흐름 정리
 * @ 2026.05.27    송혜진       챗봇 답변 참고 문서 references 복원 기능 추가
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotReferenceDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatMessageEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiChatSessionEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.SessionType;

import java.util.List;
import java.util.Map;

public interface AiSecretaryService {

    AiChatSessionEntity createSession(String empNo, SessionType sessionType, String title);

    AiChatSessionEntity createAssistantSession(String empNo, String title);

    AiChatSessionEntity getOrCreateChatbotSession(String empNo);

    List<AiChatSessionEntity> getSessionList(String empNo, SessionType sessionType);

    List<AiChatMessageEntity> getMessageList(Integer sessionId);

    Map<Integer, List<ChatbotReferenceDto>> getMessageReferences(List<Integer> assistantMessageIds);

    AiChatMessageEntity saveMessage(Integer sessionId, AiChatMessageEntity message);
}
