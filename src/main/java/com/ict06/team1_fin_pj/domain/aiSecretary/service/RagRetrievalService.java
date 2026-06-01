/**
 * @FileName : RagRetrievalService.java
 * @Description : 사내 AI 챗봇 RAG 검색 Service 구현체
 *                - 사용자 질문 기반 문서 청크 검색
 *                - 사용자 조직/직책 권한 기준 RAG 문서 필터링
 *                - pgvector 기반 유사도 검색 결과 후처리
 *                - 최종 답변 prompt에 사용할 ragChunks 구성
 *                - 권한 차단 및 no-context 케이스 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 22
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.22    송혜진       최초 생성
 * @ 2026.05.22    송혜진       사용자 권한 기반 RAG 검색 필터링 기준 정리
 * @ 2026.05.27    송혜진       본부/팀/직책 전원 및 다중 조건 처리 기준 반영
 * @ 2026.05.28    송혜진       최종 ragChunks 기반 references 및 trace 연계 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;

import java.util.List;
import java.util.Map;

public interface RagRetrievalService {

    List<RagRetrievedChunkDto> retrieveTopChunks(String question, int topK, String empNo);

    Map<String, String> consumeLastPermissionDeniedInfo();
}
