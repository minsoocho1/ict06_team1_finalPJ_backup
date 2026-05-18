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