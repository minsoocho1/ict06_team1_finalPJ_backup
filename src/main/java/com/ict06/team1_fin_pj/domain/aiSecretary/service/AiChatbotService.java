/**
 * @FileName : AiChatbotService.java
 * @Description : 사용자 챗봇 질문 처리 서비스 인터페이스
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 (챗봇 질문 처리 메서드 정의)
 * @ 2026.05.12    송혜진        챗봇 세션 기반 질문/응답 처리 구조 반영
 * @ 2026.05.22    송혜진        RAG 기반 챗봇 응답 및 trace 저장 흐름에 맞춰 구현체 연동 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

// [ 챗봇 질문 처리 흐름 담당 ]
// 프롬프트 구성/ AI Provider 호출/ AI 응답 저장/ 응답 DTO 반환
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotAskResponseDto;

public interface AiChatbotService {

    ChatbotAskResponseDto ask(Integer sessionId, String content);
}
