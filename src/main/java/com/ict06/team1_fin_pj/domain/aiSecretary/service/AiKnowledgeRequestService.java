/**
 * @FileName : AiKnowledgeRequestServiceImpl.java
 * @Description : AI 챗봇 자료 등록 요청 Service 구현체
 *                - 사용자의 챗봇/RAG 자료 등록 요청 저장 및 조회
 *                - 자료 유형, 카테고리, 권한 희망 조건, 요청 사유 관리
 *                - 내 요청 목록 조회 및 요청 상세 데이터 구성
 *                - 관리자 RAG 문서 관리 화면과 연계되는 자료 등록 요청 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.20    송혜진       챗봇 자료 등록 요청 저장 및 내 요청 목록 조회 기능 정리
 * @ 2026.05.27    송혜진       권한 희망 조건 및 관리자 RAG 상세 연계 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestCreateDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestSuggestionsDto;

import java.util.List;

public interface AiKnowledgeRequestService {

    KnowledgeResponseDto createRequest(KnowledgeRequestCreateDto requestDto);

    List<KnowledgeResponseDto> getMyRequests(String empNo);

    KnowledgeRequestSuggestionsDto getSuggestions();
}
