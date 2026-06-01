/**
 * @FileName : AiTemplateRequestServiceImpl.java
 * @Description : AI 추천 템플릿 요청 Service 구현체
 *                - 사용자의 추천 템플릿 추가 요청 저장 및 조회
 *                - 내 템플릿 요청 목록 조회
 *                - 요청 상태, 카테고리, 사용 목적, 요청 사유 관리
 *                - 관리자 템플릿 승인 관리 화면과 연계되는 요청 데이터 처리
 *
 * @Author : 송혜진
 * @Date : 2026. 04. 28
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.28    송혜진       최초 생성
 * @ 2026.05.11    송혜진       추천 템플릿 요청 등록 및 내 요청 목록 조회 기능 정리
 * @ 2026.05.28    송혜진       관리자 추천 템플릿 승인 관리 연계 기준 반영
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestCreateRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.TemplateRequestResponseDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateRequestEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentType;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiTemplateRequestRepository;
import com.ict06.team1_fin_pj.domain.approval.entity.RequestStatus;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiTemplateRequestServiceImpl implements AiTemplateRequestService {

    private final AiTemplateRequestRepository aiTemplateRequestRepository;
    private final EmpRepository empRepository;

    @Override
    @Transactional
    public TemplateRequestResponseDto createRequest(TemplateRequestCreateRequestDto requestDto) {

        EmpEntity employee = empRepository.findByEmpNo(requestDto.getEmpNo())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사원입니다. empNo=" + requestDto.getEmpNo())
                );

        DocumentType documentType = normalizeType(requestDto.getType());

        String category = trimToNull(requestDto.getCategory());
        String dept = trimToNull(requestDto.getDept());
        String situation = trimToNull(requestDto.getSituation());
        String title = requestDto.getTitle().trim();

        boolean alreadyExists =
                aiTemplateRequestRepository.existsByEmployee_EmpNoAndTitleAndCategoryAndDeptAndSituationAndStatusIn(
                        requestDto.getEmpNo(),
                        title,
                        category,
                        dept,
                        situation,
                        List.of(RequestStatus.PENDING, RequestStatus.APPROVED)
                );

        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 검토 대기 중이거나 승인된 추천 템플릿 추가 요청이 있습니다."
            );
        }

        AiTemplateRequestEntity entity = AiTemplateRequestEntity.builder()
                .employee(employee)
                .type(documentType)
                .category(category)
                .dept(dept)
                .situation(situation)
                .tone(trimToNull(requestDto.getTone()))
                .title(title)
                .description(trimToNull(requestDto.getDescription()))
                .content(requestDto.getContent().trim())
                .previewJson(requestDto.getPreview())
                .optionsJson(buildOptionsJson(requestDto))
                .status(RequestStatus.PENDING)
                .build();

        AiTemplateRequestEntity savedEntity = aiTemplateRequestRepository.save(entity);

        return TemplateRequestResponseDto.from(savedEntity);
    }

    @Override
    public List<TemplateRequestResponseDto> getMyRequests(String empNo) {
        return aiTemplateRequestRepository
                .findByEmployee_EmpNoOrderByCreatedAtDesc(empNo)
                .stream()
                .map(TemplateRequestResponseDto::from)
                .toList();
    }

    private DocumentType normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return DocumentType.REPORT;
        }

        String normalized = type.trim().toUpperCase();

        return switch (normalized) {
            case "MINUTES" -> DocumentType.MINUTES;
            case "APPROVAL" -> DocumentType.APPROVAL;
            default -> DocumentType.REPORT;
        };
    }

    private Map<String, Object> buildOptionsJson(TemplateRequestCreateRequestDto requestDto) {
        Map<String, Object> options = new HashMap<>();

        options.put("includeTitle", Boolean.TRUE.equals(requestDto.getIncludeTitle()));
        options.put("includeParagraphs", Boolean.TRUE.equals(requestDto.getIncludeParagraphs()));
        options.put("includeSignature", Boolean.TRUE.equals(requestDto.getIncludeSignature()));

        return options;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}