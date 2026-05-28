/**
 * @FileName : AdAiSecretaryService.java
 * @Description : 관리자 AI 사내포털 서비스 인터페이스
 * @Author : 송혜진
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.17    송혜진        최초 생성 (관리자 AI 사내포털 서비스 기본 메서드 정의)
 * @ 2026.05.14    송혜진        AI 비서 운영 대시보드 조회 및 로그 다운로드 메서드 추가
 * @ 2026.05.20    송혜진        지식 베이스 및 RAG 관리 화면용 자료 요청/문서 관리 메서드 추가
 * @ 2026.05.22    송혜진        자료 등록 요청 승인/반려 및 RAG 문서 관리 흐름 반영
 * @ 2026.05.22    송혜진        관리자 최종 권한 조건 및 RAG 문서 활성화 처리 기준 정리
 * @ 2026.05.22    송혜진        RAG 문서 활성화/비활성화 상태 전이 및 PUBLISHED 반영 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.DocumentActivationResultDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;

import java.util.List;
import java.util.Map;

public interface AdAiSecretaryService {

    AdAiDashboardResponseDto getDashboardData(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result,
            int page
    );

    byte[] downloadRecentLogCsv(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result
    );

    List<KnowledgeResponseDto> getKnowledgeRequestsForAdmin(
            String requestStartDate,
            String requestEndDate,
            String requestStatus,
            String requestType,
            String requestCategory
    );

    List<Map<String, Object>> getDocumentManagementRows(
            List<KnowledgeResponseDto> knowledgeRequests,
            String docStage,
            String accessLevel,
            String docKeyword
    );

    byte[] downloadDocumentManagementCsv(
            String docStage,
            String accessLevel,
            String docKeyword
    );

    Map<String, Object> updateDocumentManagementDetail(
            Integer documentId,
            String title,
            String requestType,
            String category,
            String targetDept,
            String adminComment
    );

    List<Map<String, Object>> getAccessBlockLogs();

    KnowledgeResponseDto reviewKnowledgeRequest(
            Long requestId,
            String status,
            String adminComment,
            String reviewerEmpNo,
            String targetDept
    );

    Integer createDirectRagDocument(
            String title,
            String requestType,
            String category,
            String reason,
            String sampleQuestion,
            String referenceUrl,
            String accessLevel,
            String targetDept,
            String adminEmpNo
    );

    DocumentActivationResultDto activateOrToggleDocument(
            Integer documentId,
            String adminEmpNo
    );
}
