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
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeRequestSuggestionsDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeStatus;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiKnowledgeRequestRepository;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiKnowledgeRequestServiceImpl implements AiKnowledgeRequestService {

    private static final List<String> DEFAULT_REQUEST_TYPES = List.of(
            "사내 규정",
            "업무 매뉴얼",
            "FAQ",
            "서비스 이용 안내",
            "기타"
    );

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "근태",
            "인사",
            "전자결재",
            "교육",
            "복지",
            "시스템",
            "기타"
    );

    private final AiKnowledgeRequestRepository aiKnowledgeRequestRepository;
    private final EmpRepository empRepository;

    @Override
    @Transactional
    public KnowledgeResponseDto createRequest(KnowledgeRequestCreateDto requestDto) {
        EmpEntity requester = empRepository.findByEmpNo(requestDto.getRequesterNo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "요청자 정보를 찾을 수 없습니다."
                ));

        AiKnowledgeRequestEntity entity = AiKnowledgeRequestEntity.builder()
                .requester(requester)
                .title(trimToNull(requestDto.getTitle()))
                .requestType(trimToNull(requestDto.getRequestType()))
                .category(trimToNull(requestDto.getCategory()))
                .targetDept(trimToNull(requestDto.getTargetDept()))
                .reason(trimToNull(requestDto.getReason()))
                .sampleQuestion(trimToNull(requestDto.getSampleQuestion()))
                .referenceUrl(trimToNull(requestDto.getReferenceUrl()))
                .accessLevel(trimToNull(requestDto.getAccessLevel()))
                .status(AiKnowledgeStatus.PENDING)
                .build();

        AiKnowledgeRequestEntity saved = aiKnowledgeRequestRepository.save(entity);
        return KnowledgeResponseDto.from(saved);
    }

    @Override
    public List<KnowledgeResponseDto> getMyRequests(String empNo) {
        return aiKnowledgeRequestRepository.findByRequester_EmpNoOrderByCreatedAtDesc(empNo)
                .stream()
                .map(KnowledgeResponseDto::from)
                .toList();
    }

    @Override
    public KnowledgeRequestSuggestionsDto getSuggestions() {
        return KnowledgeRequestSuggestionsDto.builder()
                .requestTypes(mergeSuggestions(DEFAULT_REQUEST_TYPES, aiKnowledgeRequestRepository.findDistinctRequestTypes()))
                .categories(mergeSuggestions(DEFAULT_CATEGORIES, aiKnowledgeRequestRepository.findDistinctCategories()))
                .build();
    }

    private List<String> mergeSuggestions(List<String> defaults, List<String> databaseValues) {
        Set<String> merged = new LinkedHashSet<>();
        addAllNormalized(merged, defaults);
        addAllNormalized(merged, databaseValues);
        return List.copyOf(merged);
    }

    private void addAllNormalized(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                target.add(normalized);
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
