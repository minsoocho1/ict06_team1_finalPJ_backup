/**
 * @FileName : AiChatbotService.java
 * @Description : 사용자 챗봇 질문 처리, RAG 검색 결과 기반 답변 생성 및 retrieval trace 저장 담당
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진        최초 생성 (챗봇 질문 처리 및 AI 응답 메시지 저장)
 * @ 2026.05.12    송혜진        챗봇 세션 기반 질문 처리 및 AI_LOG 저장 흐름 정리
 * @ 2026.05.22    송혜진        RAG 검색 결과를 프롬프트에 반영하고 중복 threshold 후처리 제거
 * @ 2026.05.22    송혜진        답변에 사용된 RAG chunk 기준으로 AI_RETRIEVAL_TRACE 저장 연동
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

// [ 챗봇 질문 처리 흐름 담당 ]
// 프롬프트 구성/ AI Provider 호출/ AI 응답 저장/ 응답 DTO 반환
import com.ict06.team1_fin_pj.common.dto.aiSecretary.ChatbotAskResponseDto;

public interface AiChatbotService {

    ChatbotAskResponseDto ask(Integer sessionId, String content);
}
