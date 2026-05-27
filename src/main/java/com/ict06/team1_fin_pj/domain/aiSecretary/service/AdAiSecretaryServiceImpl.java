/**
 * @FileName : AdAiSecretaryServiceImpl.java
 * @Description : 관리자 AI 사내포털 서비스 구현체
 * @Author : 송혜진
 * @Date : 2026. 04. 17
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    ----------------------------------------
 * @ 2026.04.17    송혜진        최초 생성 (관리자 AI 사내포털 서비스 기본 구조 작성)
 * @ 2026.05.14    송혜진        AI 비서 운영 대시보드 데이터 조회 및 CSV 다운로드 처리 추가
 * @ 2026.05.20    송혜진        지식 베이스 및 RAG 관리 화면용 문서/자료 요청 조회 로직 추가
 * @ 2026.05.22    송혜진        자료 등록 요청 승인 시 DOCUMENT 생성 및 RAG 처리 상태 흐름 정리
 * @ 2026.05.22    송혜진        관리자 최종 권한 조건 targetDept 저장 및 문서 상세 권한 표시 기준 반영
 * @ 2026.05.22    송혜진        RAG 문서 활성화/비활성화 상태 전이 및 PUBLISHED 반영 기준 정리
 */

package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiLogType;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeStatus;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentProcessLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.ProcessStage;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.ProcessStatus;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiKnowledgeRequestRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiLogRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiTemplateDashboardRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiDocumentRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.DocumentProcessLogRepository;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardDocumentStatusDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardFeatureUsageDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardRecentLogDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardSummaryDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AdAiDashboardUsageTrendDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.DocumentActivationResultDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.KnowledgeResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentChunkResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentProcessRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentProcessResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.AccessLevel;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentDomain;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocVectorEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.time.format.DateTimeParseException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdAiSecretaryServiceImpl implements AdAiSecretaryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AI_RAG_DOMAIN = DocumentDomain.AI_RAG.name();
    private static final String DIRECT_ADMIN_COMMENT = "*관리자 직접 등록";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final int RECENT_LOG_PAGE_SIZE = 10;

    private final AiLogRepository aiLogRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final AiTemplateDashboardRepository aiTemplateDashboardRepository;
    private final AiKnowledgeRequestRepository aiKnowledgeRequestRepository;
    private final DocumentProcessLogRepository documentProcessLogRepository;
    private final RestTemplate restTemplate;
    private final EmpRepository empRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    @Override
    public AdAiDashboardResponseDto getDashboardData(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result,
            int page
    ) {
        PeriodRange dashboardRange = resolveRange(period, startDate, endDate);
        List<AiLogEntity> currentLogs = loadLogs(dashboardRange);
        List<AiLogEntity> previousLogs = loadLogs(new PeriodRange(
                dashboardRange.compareStartAt(),
                dashboardRange.compareEndAt(),
                dashboardRange.compareStartAt().minusDays(dashboardRange.days()),
                dashboardRange.compareStartAt(),
                dashboardRange.compareLabel(),
                dashboardRange.days()
        ));

        Page<AiLogEntity> recentLogPage = loadRecentLogPage(
                dashboardRange,
                department,
                aiType,
                result,
                page,
                RECENT_LOG_PAGE_SIZE
        );

        return AdAiDashboardResponseDto.builder()
                .summaryCards(buildSummaryCards(currentLogs, previousLogs, dashboardRange.compareLabel()))
                .featureUsageList(buildFeatureUsage(currentLogs, dashboardRange))
                .usageTrendList(buildUsageTrend(dashboardRange, currentLogs))
                .recentLogList(buildRecentLogs(recentLogPage))
                .documentStatusList(buildDocumentStatusSummary())
                .documentStatusTitle(buildDocumentStatusTitle())
                .currentPage(recentLogPage.getTotalPages() == 0 ? 0 : recentLogPage.getNumber() + 1)
                .totalPages(recentLogPage.getTotalPages())
                .totalLogCount(recentLogPage.getTotalElements())
                .hasPrevious(recentLogPage.hasPrevious())
                .hasNext(recentLogPage.hasNext())
                .build();
    }

    @Override
    public byte[] downloadRecentLogCsv(
            int period,
            String startDate,
            String endDate,
            String department,
            String aiType,
            String result
    ) {
        PeriodRange range = resolveRange(period, startDate, endDate);
        List<AiLogEntity> logs = aiLogRepository.findDashboardLogsForExport(
                range.startAt(),
                range.endAt(),
                safe(department),
                safe(aiType),
                safe(result),
                AiLogType.CHATBOT,
                AiLogType.ASSISTANT
        );

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        appendCsvRow(csv,
                "Request Time",
                "Requester",
                "Department",
                "AI Type",
                "Request Summary",
                "Result Time",
                "Message Content"
        );

        for (AiLogEntity log : logs) {
            if (log == null) {
                continue;
            }

            appendCsvRow(csv,
                    formatCsvCell(log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FORMATTER)),
                    formatCsvCell(resolveEmployeeName(log)),
                    formatCsvCell(resolveDepartmentName(log)),
                    formatCsvCell(resolveTypeLabel(log)),
                    formatCsvCell(buildRequestSummary(log)),
                    formatCsvCell(resolveResultLabel(log)),
                    formatCsvCell(resolveDurationText(log))
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<KnowledgeResponseDto> getKnowledgeRequestsForAdmin(
            String requestStartDate,
            String requestEndDate,
            String requestStatus,
            String requestType,
            String requestCategory
    ) {
        LocalDate parsedStartDate = parseDate(requestStartDate);
        LocalDate parsedEndDate = parseDate(requestEndDate);

        if (parsedStartDate != null && parsedEndDate != null && parsedStartDate.isAfter(parsedEndDate)) {
            LocalDate temp = parsedStartDate;
            parsedStartDate = parsedEndDate;
            parsedEndDate = temp;
        }

        final LocalDate startDate = parsedStartDate;
        final LocalDate endDate = parsedEndDate;

        String normalizedStatus = safe(requestStatus);
        String normalizedType = safe(requestType);
        String normalizedCategory = safe(requestCategory);

        Predicate<KnowledgeResponseDto> statusFilter = dto ->
                normalizedStatus.isEmpty()
                        || normalizedStatus.equalsIgnoreCase(safe(dto.getStatus()))
                        || normalizedStatus.equalsIgnoreCase(safe(dto.getStatusLabel()));

        Predicate<KnowledgeResponseDto> typeFilter = dto ->
                normalizedType.isEmpty()
                        || normalizedType.equalsIgnoreCase(safe(dto.getRequestType()));

        Predicate<KnowledgeResponseDto> categoryFilter = dto ->
                normalizedCategory.isEmpty()
                        || normalizedCategory.equalsIgnoreCase(safe(dto.getCategory()));

        Predicate<KnowledgeResponseDto> dateFilter = dto -> {
            if (startDate == null && endDate == null) {
                return true;
            }

            if (dto.getCreatedAt() == null) {
                return false;
            }

            LocalDate createdDate = dto.getCreatedAt().toLocalDate();

            boolean afterStart = startDate == null || !createdDate.isBefore(startDate);
            boolean beforeEnd = endDate == null || !createdDate.isAfter(endDate);

            return afterStart && beforeEnd;
        };

        return aiKnowledgeRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(KnowledgeResponseDto::from)
                .filter(dateFilter)
                .filter(statusFilter)
                .filter(typeFilter)
                .filter(categoryFilter)
                .sorted(Comparator
                        .comparing((KnowledgeResponseDto dto) ->
                                !"PENDING".equalsIgnoreCase(safe(dto.getStatus())))
                        .thenComparing(
                                KnowledgeResponseDto::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public KnowledgeResponseDto reviewKnowledgeRequest(
            Long requestId,
            String status,
            String adminComment,
            String reviewerEmpNo,
            String targetDept
    ) {
        if (requestId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 ID가 필요합니다.");
        }

        AiKnowledgeStatus reviewStatus = parseReviewStatus(status);
        String trimmedComment = safe(adminComment);
        if (trimmedComment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 검토 메모를 입력해 주세요.");
        }

        AiKnowledgeRequestEntity request = aiKnowledgeRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자료 등록 요청을 찾을 수 없습니다."));

        EmpEntity reviewer = empRepository.findByEmpNo(safe(reviewerEmpNo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "검토자 정보를 찾을 수 없습니다."));

        AiKnowledgeStatus currentStatus = request.getStatus();
        if (currentStatus != AiKnowledgeStatus.PENDING) {
            if (currentStatus == AiKnowledgeStatus.APPROVED
                    && (request.getTargetDoc() == null || request.getTargetDoc().getDocId() == null)) {
                recoverApprovedKnowledgeRequest(request);

                AiKnowledgeRequestEntity recovered = aiKnowledgeRequestRepository.findById(requestId).orElse(request);
                if (recovered.getTargetDoc() != null && recovered.getTargetDoc().getDocId() != null) {
                    return KnowledgeResponseDto.from(recovered);
                }
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 요청입니다.");
        }

        request.updateReviewStatus(reviewStatus, trimmedComment, reviewer);
        AiKnowledgeRequestEntity saved = aiKnowledgeRequestRepository.saveAndFlush(request);

        if (reviewStatus == AiKnowledgeStatus.APPROVED) {
            String normalizedTargetDept = safe(targetDept);
            if (!normalizedTargetDept.isBlank()) {
                updateKnowledgeRequestTargetDept(saved.getRequestId(), normalizedTargetDept);
                saved = aiKnowledgeRequestRepository.findById(requestId).orElse(saved);
            }
            processApprovedKnowledgeRequest(saved, reviewer);
            saved = aiKnowledgeRequestRepository.findById(requestId).orElse(saved);
        }

        return KnowledgeResponseDto.from(saved);
    }

    @Override
    @Transactional
    public Integer createDirectRagDocument(
            String title,
            String requestType,
            String category,
            String reason,
            String sampleQuestion,
            String referenceUrl,
            String accessLevel,
            String targetDept,
            String adminEmpNo
    ) {
        String normalizedTitle = requireText(title, "문서명을 입력해 주세요.");
        String normalizedRequestType = requireText(requestType, "문서 유형을 선택해 주세요.");
        String normalizedCategory = requireText(category, "카테고리를 선택해 주세요.");
        String normalizedReason = requireText(reason, "요청 사유를 입력해 주세요.");
        String normalizedSampleQuestion = requireText(sampleQuestion, "챗봇 질문 예시를 입력해 주세요.");
        String normalizedReferenceUrl = requireText(referenceUrl, "참고 URL 또는 콘텐츠 경로를 입력해 주세요.");
        String normalizedTargetDept = requireText(targetDept, "권한 희망 조건을 선택해 주세요.");
        String normalizedAccessLevel = safe(accessLevel).isBlank() ? "CUSTOM" : safe(accessLevel).toUpperCase(Locale.ROOT);

        EmpEntity admin = empRepository.findByEmpNo(safe(adminEmpNo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

        DocumentEntity document = DocumentEntity.builder()
                .title(normalizedTitle)
                .filePath(normalizedReferenceUrl)
                .documentDomain(DocumentDomain.AI_RAG)
                .accessLevel(resolveDocumentAccessLevel(normalizedAccessLevel, normalizedTargetDept))
                .currentStage(DocumentStage.UPLOADED)
                .createdBy(admin)
                .build();

        DocumentEntity savedDocument = aiDocumentRepository.saveAndFlush(document);
        Integer savedDocId = savedDocument.getDocId();
        if (savedDocId == null) {
            throw new IllegalStateException("생성된 DOCUMENT의 식별자를 확인할 수 없습니다.");
        }

        insertDirectKnowledgeRequest(
                admin.getEmpNo(),
                normalizedTitle,
                normalizedRequestType,
                normalizedCategory,
                normalizedReason,
                normalizedSampleQuestion,
                normalizedReferenceUrl,
                normalizedAccessLevel,
                normalizedTargetDept,
                savedDocId
        );

        return savedDocId;
    }

    @Override
    @Transactional
    public DocumentActivationResultDto activateOrToggleDocument(Integer documentId, String adminEmpNo) {
        if (documentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서 ID가 필요합니다.");
        }

        DocumentEntity document = aiDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        if (document.getDocumentDomain() != DocumentDomain.AI_RAG) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_RAG 문서만 상태를 변경할 수 있습니다.");
        }

        EmpEntity admin = empRepository.findByEmpNo(safe(adminEmpNo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

        DocumentStage currentStage = document.getCurrentStage();
        if (currentStage == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "문서 상태 정보가 없습니다.");
        }

        return switch (currentStage) {
            case UPLOADED -> {
                processDocumentThroughAiServer(document, admin);
                DocumentEntity refreshed = aiDocumentRepository.findById(documentId).orElse(document);
                DocumentStage refreshedStage = refreshed.getCurrentStage();
                if (refreshedStage == DocumentStage.APPROVAL_PENDING) {
                    yield buildActivationResult(true, "문서 처리가 완료되었습니다. 활성화를 진행해 주세요.", refreshedStage);
                }
                if (refreshedStage == DocumentStage.CHUNK_FAILED || refreshedStage == DocumentStage.EMBED_FAILED) {
                    yield buildActivationResult(false, "문서 처리에 실패했습니다. 실패 사유를 확인해 주세요.", refreshedStage);
                }
                yield buildActivationResult(false, "문서 처리 상태를 확인해 주세요.", refreshedStage);
            }
            case CHUNK_FAILED, EMBED_FAILED -> {
                processDocumentThroughAiServer(document, admin);
                DocumentEntity refreshed = aiDocumentRepository.findById(documentId).orElse(document);
                DocumentStage refreshedStage = refreshed.getCurrentStage();
                if (refreshedStage == DocumentStage.APPROVAL_PENDING) {
                    yield buildActivationResult(true, "재처리가 완료되었습니다. 활성화를 진행해 주세요.", refreshedStage);
                }
                if (refreshedStage == DocumentStage.CHUNK_FAILED || refreshedStage == DocumentStage.EMBED_FAILED) {
                    yield buildActivationResult(false, "재처리에 실패했습니다. 실패 사유를 확인해 주세요.", refreshedStage);
                }
                yield buildActivationResult(false, "재처리 상태를 확인해 주세요.", refreshedStage);
            }
            case APPROVAL_PENDING -> {
                document.updateStage(DocumentStage.PUBLISHED);
                aiDocumentRepository.saveAndFlush(document);
                yield buildActivationResult(true, "RAG 검색 대상으로 활성화되었습니다.", DocumentStage.PUBLISHED);
            }
            case PUBLISHED -> {
                document.updateStage(DocumentStage.APPROVAL_PENDING);
                aiDocumentRepository.saveAndFlush(document);
                yield buildActivationResult(true, "RAG 검색 대상에서 제외되었습니다.", DocumentStage.APPROVAL_PENDING);
            }
            case CHUNKING, EMBEDDING ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "문서 처리 중에는 상태를 변경할 수 없습니다.");
        };
    }

    private void insertDirectKnowledgeRequest(
            String adminEmpNo,
            String title,
            String requestType,
            String category,
            String reason,
            String sampleQuestion,
            String referenceUrl,
            String accessLevel,
            String targetDept,
            Integer targetDocId
    ) {
        entityManager.createNativeQuery("""
                insert into ai_knowledge_request (
                    requester_no,
                    title,
                    request_type,
                    category,
                    target_dept,
                    reason,
                    sample_question,
                    reference_url,
                    access_level,
                    status,
                    admin_comment,
                    reviewer_no,
                    reviewed_at,
                    target_doc_id,
                    created_at,
                    updated_at
                ) values (
                    :requesterNo,
                    :title,
                    :requestType,
                    :category,
                    :targetDept,
                    :reason,
                    :sampleQuestion,
                    :referenceUrl,
                    :accessLevel,
                    :status,
                    :adminComment,
                    :reviewerNo,
                    CURRENT_TIMESTAMP,
                    :targetDocId,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """)
                .setParameter("requesterNo", adminEmpNo)
                .setParameter("title", title)
                .setParameter("requestType", requestType)
                .setParameter("category", category)
                .setParameter("targetDept", targetDept)
                .setParameter("reason", reason)
                .setParameter("sampleQuestion", sampleQuestion)
                .setParameter("referenceUrl", referenceUrl)
                .setParameter("accessLevel", accessLevel)
                .setParameter("status", AiKnowledgeStatus.APPROVED.name())
                .setParameter("adminComment", DIRECT_ADMIN_COMMENT)
                .setParameter("reviewerNo", adminEmpNo)
                .setParameter("targetDocId", targetDocId)
                .executeUpdate();
    }

    private void updateKnowledgeRequestTargetDept(Long requestId, String targetDept) {
        if (requestId == null || safe(targetDept).isBlank()) {
            return;
        }

        entityManager.createNativeQuery("""
                update ai_knowledge_request
                   set target_dept = :targetDept,
                       updated_at = CURRENT_TIMESTAMP
                 where knowledge_request_id = :requestId
                """)
                .setParameter("targetDept", targetDept)
                .setParameter("requestId", requestId)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    private void processApprovedKnowledgeRequest(AiKnowledgeRequestEntity request, EmpEntity reviewer) {
        if (request == null || request.getStatus() != AiKnowledgeStatus.APPROVED) {
            return;
        }

        DocumentEntity targetDocument = request.getTargetDoc();
        if (targetDocument == null || targetDocument.getDocId() == null) {
            String referenceUrl = safe(request.getReferenceUrl());
            Optional<DocumentEntity> existingDocument = referenceUrl.isBlank()
                    ? Optional.empty()
                    : aiDocumentRepository.findFirstByFilePathAndDocumentDomainOrderByCreatedAtDesc(
                    referenceUrl,
                    DocumentDomain.AI_RAG
            );

            targetDocument = existingDocument.orElseGet(() -> createDocumentFromKnowledgeRequest(request, reviewer));
            linkKnowledgeRequestToDocument(request.getRequestId(), targetDocument.getDocId());
            entityManager.refresh(request);
        } else {
            targetDocument = aiDocumentRepository.findById(targetDocument.getDocId()).orElse(targetDocument);
        }

        if (targetDocument.getDocId() == null) {
            throw new IllegalStateException("생성된 DOCUMENT의 식별자를 확인할 수 없습니다.");
        }

        if (!shouldProcessDocument(targetDocument)) {
            return;
        }

        processDocumentThroughAiServer(targetDocument, reviewer);
    }

    private void recoverApprovedKnowledgeRequest(AiKnowledgeRequestEntity request) {
        if (request == null) {
            return;
        }

        if (request.getTargetDoc() != null && request.getTargetDoc().getDocId() != null) {
            return;
        }

        String referenceUrl = safe(request.getReferenceUrl());
        if (referenceUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참고 URL이 없습니다.");
        }

        Optional<DocumentEntity> existingDocument = aiDocumentRepository.findFirstByFilePathAndDocumentDomainOrderByCreatedAtDesc(
                referenceUrl,
                DocumentDomain.AI_RAG
        );
        if (existingDocument.isEmpty()) {
            return;
        }

        DocumentEntity document = existingDocument.get();
        try {
            linkKnowledgeRequestToDocument(request.getRequestId(), document.getDocId());
            entityManager.refresh(request);
        } catch (Exception ignored) {
            return;
        }
    }

    private DocumentEntity createDocumentFromKnowledgeRequest(AiKnowledgeRequestEntity request, EmpEntity reviewer) {
        String title = safe(request == null ? null : request.getTitle());
        String referenceUrl = safe(request == null ? null : request.getReferenceUrl());
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서명이 없습니다.");
        }
        if (referenceUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참고 URL이 없습니다.");
        }

        DocumentEntity document = DocumentEntity.builder()
                .title(title)
                .filePath(referenceUrl)
                .documentDomain(DocumentDomain.AI_RAG)
                .accessLevel(resolveDocumentAccessLevel(request.getAccessLevel(), request.getTargetDept()))
                .currentStage(DocumentStage.UPLOADED)
                .createdBy(reviewer)
                .build();

        DocumentEntity saved = aiDocumentRepository.saveAndFlush(document);
        Integer savedDocId = saved.getDocId();
        if (savedDocId == null) {
            throw new IllegalStateException("생성된 DOCUMENT의 식별자를 확인할 수 없습니다.");
        }
        return aiDocumentRepository.findById(savedDocId).orElse(saved);
    }

    private void processDocumentThroughAiServer(DocumentEntity document, EmpEntity processedBy) {
        if (document == null || document.getDocId() == null) {
            return;
        }

        document.updateStage(DocumentStage.CHUNKING);
        aiDocumentRepository.saveAndFlush(document);

        DocumentProcessLogEntity chunkLog = createProcessLog(document, processedBy, ProcessStage.CHUNK);
        documentProcessLogRepository.saveAndFlush(chunkLog);

        AiDocumentProcessResponseDto response;
        try {
            response = requestDocumentProcessing(document);
            chunkLog.markSuccess();
            documentProcessLogRepository.saveAndFlush(chunkLog);
        } catch (Exception e) {
            String errorMessage = extractRootMessage(e);
            chunkLog.markFail(errorMessage);
            documentProcessLogRepository.saveAndFlush(chunkLog);
            document.updateStage(DocumentStage.CHUNK_FAILED);
            aiDocumentRepository.saveAndFlush(document);
            return;
        }

        document.updateStage(DocumentStage.EMBEDDING);
        aiDocumentRepository.saveAndFlush(document);

        DocumentProcessLogEntity embedLog = createProcessLog(document, processedBy, ProcessStage.EMBED);
        documentProcessLogRepository.saveAndFlush(embedLog);

        try {
            boolean vectorPersisted = persistChunks(document, response.getChunks());
            document.updateSummaryPreview(safe(response.getExtractedTextPreview()));
            document.updateStage(vectorPersisted ? DocumentStage.APPROVAL_PENDING : DocumentStage.EMBEDDING);
            aiDocumentRepository.saveAndFlush(document);

            embedLog.markSuccess();
            documentProcessLogRepository.saveAndFlush(embedLog);
        } catch (Exception e) {
            String errorMessage = extractRootMessage(e);
            embedLog.markFail(errorMessage);
            documentProcessLogRepository.saveAndFlush(embedLog);
            document.updateStage(DocumentStage.EMBED_FAILED);
            aiDocumentRepository.saveAndFlush(document);
        }
    }

    private AiDocumentProcessResponseDto requestDocumentProcessing(DocumentEntity document) {
        String url = aiServerBaseUrl + "/api/ai/documents/process";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiDocumentProcessRequestDto body = AiDocumentProcessRequestDto.builder()
                .docId(document.getDocId())
                .title(document.getTitle())
                .filePath(document.getFilePath())
                .build();

        HttpEntity<AiDocumentProcessRequestDto> entity = new HttpEntity<>(body, headers);
        ResponseEntity<AiDocumentProcessResponseDto> response = restTemplate.postForEntity(
                url,
                entity,
                AiDocumentProcessResponseDto.class
        );

        AiDocumentProcessResponseDto payload = response.getBody();
        if (payload == null || payload.getChunks() == null || payload.getChunks().isEmpty()) {
            throw new IllegalStateException("청크 생성 결과가 비어 있습니다.");
        }

        if (payload.getDocId() != null && !payload.getDocId().equals(document.getDocId())) {
            throw new IllegalStateException("AI 서버 응답 문서 ID가 요청 문서 ID와 일치하지 않습니다.");
        }

        return payload;
    }

    private boolean persistChunks(DocumentEntity document, List<AiDocumentChunkResponseDto> chunkDtos) {
        if (document == null || chunkDtos == null || chunkDtos.isEmpty()) {
            throw new IllegalStateException("청크 생성 결과가 비어 있습니다.");
        }

        boolean persistVector = isDocVectorTextStorage();

        if (document.getChunks() != null && !document.getChunks().isEmpty()) {
            document.clearChunks();
            aiDocumentRepository.saveAndFlush(document);
        }

        for (AiDocumentChunkResponseDto chunkDto : chunkDtos) {
            String embeddingData = safe(chunkDto.getEmbeddingData());
            if (embeddingData.isBlank()) {
                throw new IllegalStateException("임베딩 데이터가 없습니다.");
            }

            DocChunkEntity chunk = DocChunkEntity.builder()
                    .chunkNo(chunkDto.getChunkNo())
                    .content(safe(chunkDto.getContent()))
                    .tokenCount(chunkDto.getTokenCount())
                    .sectionTitle(safe(chunkDto.getSectionTitle()))
                    .build();

            if (persistVector) {
                DocVectorEntity vector = DocVectorEntity.builder()
                        .embeddingData(embeddingData)
                        .modelName(safe(chunkDto.getModelName(), "jhgan/ko-sroberta-multitask"))
                        .dimension(chunkDto.getDimension() != null ? chunkDto.getDimension() : 0)
                        .build();
                chunk.setVector(vector);
            }

            document.addChunk(chunk);
        }

        aiDocumentRepository.saveAndFlush(document);
        return persistVector;
    }

    private boolean shouldProcessDocument(DocumentEntity document) {
        if (document == null) {
            return false;
        }

        if (document.getChunks() == null || document.getChunks().isEmpty()) {
            return true;
        }

        DocumentStage stage = document.getCurrentStage();
        return stage == DocumentStage.UPLOADED
                || stage == DocumentStage.CHUNK_FAILED
                || stage == DocumentStage.EMBED_FAILED;
    }

    private void linkKnowledgeRequestToDocument(Long requestId, Integer docId) {
        if (requestId == null || docId == null) {
            throw new IllegalArgumentException("지식 요청 ID 또는 문서 ID가 없습니다.");
        }

        int updated = entityManager.createNativeQuery("""
                update ai_knowledge_request
                set target_doc_id = :docId
                where knowledge_request_id = :requestId
                """)
                .setParameter("docId", docId)
                .setParameter("requestId", requestId)
                .executeUpdate();

        if (updated <= 0) {
            throw new IllegalStateException("AI_KNOWLEDGE_REQUEST와 DOCUMENT를 연결하지 못했습니다.");
        }
    }

    private boolean isDocVectorTextStorage() {
        if (docVectorStorageTypeCache != null) {
            return "text".equalsIgnoreCase(docVectorStorageTypeCache);
        }

        Object result = entityManager.createNativeQuery("""
                select lower(udt_name)
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'doc_vector'
                  and column_name = 'embedding_data'
                """)
                .getSingleResult();

        docVectorStorageTypeCache = result == null ? null : result.toString();
        return "text".equalsIgnoreCase(docVectorStorageTypeCache);
    }

    private String docVectorStorageTypeCache;

    private AccessLevel resolveDocumentAccessLevel(String accessLevel, String targetDept) {
        String normalized = safe(accessLevel).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PUBLIC" -> AccessLevel.PUBLIC;
            case "ADMIN_ONLY" -> AccessLevel.PRIVATE;
            case "CUSTOM" -> safe(targetDept).isBlank() ? AccessLevel.PRIVATE : AccessLevel.DEPT;
            default -> AccessLevel.PUBLIC;
        };
    }

    private DocumentProcessLogEntity createProcessLog(DocumentEntity document, EmpEntity processedBy, ProcessStage stage) {
        return DocumentProcessLogEntity.builder()
                .document(document)
                .stage(stage)
                .status(ProcessStatus.PROCESSING)
                .startedAt(LocalDateTime.now())
                .processedBy(processedBy)
                .build();
    }

    @Override
    public List<Map<String, Object>> getDocumentManagementRows(
            List<KnowledgeResponseDto> knowledgeRequests,
            String docStage,
            String accessLevel,
            String docKeyword
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Integer, KnowledgeResponseDto> requestsByTargetDocId = buildKnowledgeRequestMapByTargetDocId(knowledgeRequests);

        List<DocumentEntity> documents = Optional.ofNullable(aiDocumentRepository.findAiRagDocuments())
                .orElseGet(List::of);
        for (DocumentEntity document : documents) {
            KnowledgeResponseDto matchedRequest = document == null ? null : requestsByTargetDocId.get(document.getDocId());
            rows.add(buildDocumentManagementRow(document, matchedRequest));
        }

        if (knowledgeRequests != null) {
            knowledgeRequests.stream()
                    .filter(this::isApprovedOrPublishedKnowledgeRequest)
                    .filter(request -> request.getTargetDocId() == null)
                    .map(this::buildApprovedRequestRow)
                    .forEach(rows::add);
        }

        rows.sort(Comparator.comparing(
                (Map<String, Object> row) -> (LocalDateTime) row.get("registeredAt"),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return applyDocumentManagementFilters(rows, docStage, accessLevel, docKeyword);
    }

    @Override
    public byte[] downloadDocumentManagementCsv(
            String docStage,
            String accessLevel,
            String docKeyword
    ) {
        List<KnowledgeResponseDto> allKnowledgeRequests = getKnowledgeRequestsForAdmin("", "", "", "", "");
        List<Map<String, Object>> rows = getDocumentManagementRows(allKnowledgeRequests, docStage, accessLevel, docKeyword);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        appendCsvRow(csv,
                "문서ID",
                "문서명",
                "상태",
                "접근권한",
                "청크수",
                "벡터수",
                "등록일"
        );

        for (Map<String, Object> row : rows) {
            if (row == null || !"DOCUMENT".equalsIgnoreCase(safe(row.get("rowType")))) {
                continue;
            }

            appendCsvRow(csv,
                    formatCsvCell(safe(row.get("documentId"), "-")),
                    formatCsvCell(safe(row.get("title"), "-")),
                    formatCsvCell(safe(row.get("statusLabel"), "-")),
                    formatCsvCell(safe(row.get("accessLevel"), "-")),
                    formatCsvCell(safe(row.get("chunkCount"), "0")),
                    formatCsvCell(safe(row.get("vectorCount"), "0")),
                    formatCsvCell(formatRowDateTime((LocalDateTime) row.get("registeredAt")))
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public Map<String, Object> updateDocumentManagementDetail(
            Integer documentId,
            String title,
            String requestType,
            String category,
            String targetDept,
            String adminComment
    ) {
        if (documentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서 ID가 필요합니다.");
        }

        String normalizedTitle = requireText(title, "문서명을 입력해 주세요.");
        String normalizedRequestType = requireText(requestType, "자료 유형을 입력해 주세요.");
        String normalizedCategory = requireText(category, "카테고리를 입력해 주세요.");
        String normalizedTargetDept = requireText(targetDept, "최종 권한 조건을 선택해 주세요.");
        String normalizedAdminComment = safe(adminComment);

        DocumentEntity document = aiDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        List<AiKnowledgeRequestEntity> matchedRequests = aiKnowledgeRequestRepository.findByTargetDoc_DocIdInOrderByCreatedAtDesc(List.of(documentId));
        AiKnowledgeRequestEntity matchedRequest = matchedRequests.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "연결된 자료 등록 요청이 없어 상세 정보를 수정할 수 없습니다."));

        document.updateTitle(normalizedTitle);
        aiDocumentRepository.save(document);

        matchedRequest.updateAdminDocumentSettings(
                normalizedRequestType,
                normalizedCategory,
                normalizedTargetDept,
                normalizedAdminComment
        );
        aiKnowledgeRequestRepository.saveAndFlush(matchedRequest);
        aiDocumentRepository.flush();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "저장되었습니다.");
        result.put("documentId", document.getDocId());
        result.put("title", document.getTitle());
        result.put("requestType", matchedRequest.getRequestType());
        result.put("category", matchedRequest.getCategory());
        result.put("targetDept", matchedRequest.getTargetDept());
        result.put("adminComment", matchedRequest.getAdminComment());
        result.put("updatedAt", formatRowDateTime(
                matchedRequest.getUpdatedAt() != null ? matchedRequest.getUpdatedAt() : document.getUpdatedAt()
        ));
        return result;
    }

    @Override
    public List<Map<String, Object>> getAccessBlockLogs() {
        return aiLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == AiLogType.CHATBOT)
                .filter(log -> containsIgnoreCase(log.getQuery(), "permissionDenied=true"))
                .map(this::buildAccessBlockLogRow)
                .limit(20)
                .collect(Collectors.toList());
    }

    private Map<Integer, KnowledgeResponseDto> buildKnowledgeRequestMapByTargetDocId(List<KnowledgeResponseDto> knowledgeRequests) {
        Map<Integer, KnowledgeResponseDto> requestsByTargetDocId = new LinkedHashMap<>();
        if (knowledgeRequests == null || knowledgeRequests.isEmpty()) {
            return requestsByTargetDocId;
        }

        for (KnowledgeResponseDto request : knowledgeRequests) {
            if (request == null || request.getTargetDocId() == null) {
                continue;
            }
            requestsByTargetDocId.put(request.getTargetDocId(), request);
        }

        return requestsByTargetDocId;
    }

    private Map<String, Object> buildDocumentManagementRow(DocumentEntity document, KnowledgeResponseDto matchedRequest) {
        Map<String, Object> row = new LinkedHashMap<>();
        DocumentStage stage = document == null ? null : document.getCurrentStage();
        int chunkCount = document == null || document.getChunks() == null ? 0 : document.getChunks().size();
        int vectorCount = document == null || document.getChunks() == null
                ? 0
                : (int) document.getChunks().stream()
                .filter(chunk -> chunk.getVector() != null)
                .count();

        row.put("rowType", "DOCUMENT");
        row.put("documentDomain", AI_RAG_DOMAIN);
        row.put("documentId", document == null ? null : document.getDocId());
        row.put("requestId", matchedRequest == null ? null : matchedRequest.getKnowledgeRequestId());
        row.put("registeredAt", document == null ? null : (document.getCreatedAt() != null ? document.getCreatedAt() : document.getUpdatedAt()));
        row.put("status", stage == null ? "" : stage.name());
        row.put("statusLabel", resolveDocumentStageLabel(stage));
        row.put("title", document == null || document.getTitle() == null ? "-" : document.getTitle());
        row.put("category", matchedRequest != null ? safe(matchedRequest.getCategory(), "-") : "-");
        row.put("requestType", matchedRequest != null ? safe(matchedRequest.getRequestType(), "-") : "-");
        row.put("approverName", document != null && document.getCreatedBy() != null && document.getCreatedBy().getName() != null
                ? document.getCreatedBy().getName()
                : "-");
        row.put("chunkCount", chunkCount);
        row.put("vectorCount", vectorCount);
        row.put("requesterName", matchedRequest != null ? safe(matchedRequest.getRequesterName(), "-") : "-");
        row.put("requestDate", matchedRequest != null ? matchedRequest.getCreatedAt() : null);
        row.put("reason", matchedRequest != null ? safe(matchedRequest.getReason(), "-") : "-");
        row.put("sampleQuestion", matchedRequest != null ? safe(matchedRequest.getSampleQuestion(), "-") : "-");
        row.put("referenceUrl", matchedRequest != null && matchedRequest.getReferenceUrl() != null && !matchedRequest.getReferenceUrl().isBlank()
                ? matchedRequest.getReferenceUrl()
                : (document != null && document.getFilePath() != null ? document.getFilePath() : ""));
        row.put("summary", document != null && document.getSummaryPreview() != null && !document.getSummaryPreview().isBlank()
                ? document.getSummaryPreview()
                : "-");
        row.put("adminComment", matchedRequest != null ? safe(matchedRequest.getAdminComment(), "-") : "-");
        row.put("accessLevel", matchedRequest != null && safe(matchedRequest.getAccessLevel()).length() > 0
                ? resolveRequestAccessLevelLabel(matchedRequest.getAccessLevel())
                : (document == null || document.getAccessLevel() == null
                ? "-"
                : resolveDocumentAccessLevelLabel(document.getAccessLevel().name())));
        row.put("targetDept", matchedRequest != null ? safe(matchedRequest.getTargetDept(), "-") : "-");
        row.put("permissionUpdatedAt", matchedRequest != null && matchedRequest.getUpdatedAt() != null
                ? matchedRequest.getUpdatedAt()
                : (document == null ? null : document.getUpdatedAt()));
        boolean directUpload = matchedRequest == null
                || DIRECT_ADMIN_COMMENT.equals(safe(matchedRequest.getAdminComment()));
        row.put("sourceType", directUpload ? "관리자 직접 등록 문서" : "사용자 요청 기반 문서");
        row.put("failureReason", isFailedDocumentStage(stage) ? "실패 사유가 기록되지 않았습니다." : "-");
        row.put("accessCondition", buildDocumentAccessCondition(document, matchedRequest));
        row.put("retryable", isFailedDocumentStage(stage));
        row.put("chunkDetailsJson", toJson(buildChunkDetailRows(document)));
        row.put("vectorDetailsJson", toJson(buildVectorDetailRows(document)));
        return row;
    }

    private Map<String, Object> buildApprovedRequestRow(KnowledgeResponseDto request) {
        Map<String, Object> row = new LinkedHashMap<>();
        LocalDateTime registeredAt = request.getReviewedAt() != null ? request.getReviewedAt() : request.getCreatedAt();
        String requestStatus = safe(request.getStatus());
        String statusLabel = "PUBLISHED".equalsIgnoreCase(requestStatus) ? "반영 완료" : "업로드 완료";

        row.put("rowType", "REQUEST");
        row.put("documentDomain", AI_RAG_DOMAIN);
        row.put("documentId", request.getTargetDocId());
        row.put("requestId", request.getKnowledgeRequestId());
        row.put("registeredAt", registeredAt);
        row.put("status", requestStatus);
        row.put("statusLabel", statusLabel);
        row.put("title", safe(request.getTitle(), "-"));
        row.put("category", safe(request.getCategory(), "-"));
        row.put("requestType", safe(request.getRequestType(), "-"));
        row.put("approverName", safe(request.getReviewerName(), "-"));
        row.put("chunkCount", 0);
        row.put("vectorCount", 0);
        row.put("requesterName", safe(request.getRequesterName(), "-"));
        row.put("requestDate", request.getCreatedAt());
        row.put("reason", safe(request.getReason(), "-"));
        row.put("sampleQuestion", safe(request.getSampleQuestion(), "-"));
        row.put("referenceUrl", safe(request.getReferenceUrl()));
        row.put("summary", safe(request.getReason(), "-"));
        row.put("adminComment", safe(request.getAdminComment(), "-"));
        row.put("accessLevel", resolveRequestAccessLevelLabel(request.getAccessLevel()));
        row.put("targetDept", safe(request.getTargetDept(), "-"));
        row.put("sourceType", "\uC2B9\uC778 \uC694\uCCAD");
        row.put("failureReason", "-");
        row.put("retryable", false);
        return row;
    }

    private List<Map<String, Object>> applyDocumentManagementFilters(
            List<Map<String, Object>> rows,
            String docStage,
            String accessLevel,
            String docKeyword
    ) {
        String normalizedStage = safe(docStage);
        String normalizedAccessLevel = safe(accessLevel);
        String normalizedKeyword = safe(docKeyword);

        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> matchesDocumentStageFilter(row, normalizedStage))
                .filter(row -> matchesDocumentAccessFilter(row, normalizedAccessLevel))
                .filter(row -> matchesDocumentKeywordFilter(row, normalizedKeyword))
                .collect(Collectors.toList());
    }

    private boolean matchesDocumentStageFilter(Map<String, Object> row, String docStage) {
        return docStage.isBlank() || docStage.equalsIgnoreCase(safe(row.get("status")));
    }

    private boolean matchesDocumentAccessFilter(Map<String, Object> row, String accessLevel) {
        if (accessLevel.isBlank()) {
            return true;
        }

        String accessLabel = safe(row.get("accessLevel"));
        return switch (accessLevel.toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> accessLabel.contains("전체 공개");
            case "CUSTOM", "DEPT", "ROLE" -> accessLabel.contains("조건") || accessLabel.contains("역할");
            case "ADMIN_ONLY", "PRIVATE" -> accessLabel.contains("관리자 전용");
            default -> true;
        };
    }

    private boolean matchesDocumentKeywordFilter(Map<String, Object> row, String docKeyword) {
        return docKeyword.isBlank() || containsIgnoreCase(safe(row.get("title")), docKeyword);
    }

    private Map<String, Object> buildAccessBlockLogRow(AiLogEntity log) {
        Map<String, Object> row = new LinkedHashMap<>();
        String deniedReason = decodeMetaValue(parseTextMeta(log.getQuery(), "deniedReason"));
        String deniedDocTitle = decodeMetaValue(parseTextMeta(log.getQuery(), "deniedDocTitle"));
        String deniedDocId = decodeMetaValue(parseTextMeta(log.getQuery(), "deniedDocId"));
        String userHeadquarter = decodeMetaValue(parseTextMeta(log.getQuery(), "userHeadquarter"));
        String userTeam = decodeMetaValue(parseTextMeta(log.getQuery(), "userTeam"));
        String userPosition = decodeMetaValue(parseTextMeta(log.getQuery(), "userPosition"));
        String targetDept = decodeMetaValue(parseTextMeta(log.getQuery(), "targetDept"));

        String userName = resolveEmployeeName(log);
        String userEmpNo = log.getEmployee() == null ? "-" : safe(log.getEmployee().getEmpNo(), "-");

        row.put("user", "-".equals(userName) ? userEmpNo : userEmpNo + " / " + userName);
        row.put("dept", buildAccessBlockDeptText(userHeadquarter, userTeam, userPosition));
        row.put("documentTitle", deniedDocTitle.isBlank() ? ("문서 ID " + safe(deniedDocId, "-")) : deniedDocTitle);
        row.put("reason", mapDeniedReasonLabel(deniedReason));
        row.put("createdAt", formatRowDateTime(log.getCreatedAt()));
        row.put("targetDept", targetDept.isBlank() ? "-" : targetDept);
        row.put("deniedDocId", deniedDocId);
        row.put("deniedReason", deniedReason);
        row.put("questionSummary", buildRequestSummary(log));
        return row;
    }

    private String buildAccessBlockDeptText(String headquarter, String team, String position) {
        List<String> parts = new ArrayList<>();
        if (!safe(headquarter).isBlank()) {
            parts.add(headquarter);
        }
        if (!safe(team).isBlank()) {
            parts.add(team);
        }
        if (!safe(position).isBlank()) {
            parts.add(position);
        }
        return parts.isEmpty() ? "-" : String.join(" / ", parts);
    }

    private String mapDeniedReasonLabel(String deniedReason) {
        return switch (safe(deniedReason)) {
            case "headquarter-mismatch" -> "본부 불일치";
            case "team-mismatch" -> "팀 불일치";
            case "position-mismatch" -> "직책 불일치";
            case "parse-failed" -> "권한 조건 해석 실패";
            default -> "기타";
        };
    }

    private String formatRowDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String resolveDocumentStageLabel(DocumentStage stage) {
        if (stage == null) {
            return "-";
        }

        return switch (stage) {
            case UPLOADED -> "업로드 완료";
            case CHUNKING, EMBEDDING -> "임베딩 진행";
            case APPROVAL_PENDING -> "임베딩 완료";
            case CHUNK_FAILED, EMBED_FAILED -> "처리 실패";
            case PUBLISHED -> "반영 완료";
        };
    }

    private DocumentActivationResultDto buildActivationResult(boolean success, String message, DocumentStage stage) {
        return DocumentActivationResultDto.builder()
                .success(success)
                .message(message)
                .stage(stage == null ? "" : stage.name())
                .stageLabel(resolveDocumentStageLabel(stage))
                .build();
    }

    private String resolveRequestAccessLevelLabel(String accessLevel) {
        String normalized = safe(accessLevel);
        if (normalized.isEmpty()) {
            return "-";
        }

        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> "전체 공개";
            case "CUSTOM" -> "조건 조합";
            case "ADMIN_ONLY" -> "관리자 전용";
            default -> normalized;
        };
    }

    private String resolveDocumentAccessLevelLabel(String accessLevel) {
        String normalized = safe(accessLevel);
        if (normalized.isEmpty()) {
            return "-";
        }

        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> "전체 공개";
            case "DEPT", "ROLE" -> "조건 조합";
            case "PRIVATE" -> "관리자 전용";
            default -> normalized;
        };
    }

    private String buildDocumentAccessCondition(DocumentEntity document, KnowledgeResponseDto matchedRequest) {
        if (matchedRequest != null) {
            String normalized = safe(matchedRequest.getAccessLevel()).toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "PUBLIC" -> "전체 공개";
                case "CUSTOM" -> {
                    String targetDept = safe(matchedRequest.getTargetDept());
                    yield targetDept.isBlank() ? "조건부 공개" : "조건부 공개 · " + targetDept;
                }
                case "ADMIN_ONLY" -> "관리자 전용";
                default -> resolveRequestAccessLevelLabel(matchedRequest.getAccessLevel());
            };
        }

        if (document == null || document.getAccessLevel() == null) {
            return "-";
        }

        return switch (document.getAccessLevel()) {
            case PUBLIC -> "전체 공개";
            case DEPT -> "조건부 공개";
            case ROLE -> "\uC5ED\uD560 \uACF5\uAC1C";
            case PRIVATE -> "관리자 전용";
        };
    }

    private List<Map<String, Object>> buildChunkDetailRows(DocumentEntity document) {
        if (document == null || document.getChunks() == null || document.getChunks().isEmpty()) {
            return List.of();
        }

        return document.getChunks().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DocChunkEntity::getChunkNo, Comparator.nullsLast(Integer::compareTo)))
                .map(chunk -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    DocVectorEntity vector = chunk.getVector();
                    detail.put("chunkNo", chunk.getChunkNo());
                    detail.put("sectionTitle", safe(chunk.getSectionTitle(), "-"));
                    detail.put("tokenCount", chunk.getTokenCount() == null ? 0 : chunk.getTokenCount());
                    detail.put("contentPreview", limitText(safe(chunk.getContent(), "-"), 260));
                    detail.put("hasVector", vector != null);
                    detail.put("modelName", vector == null ? "-" : safe(vector.getModelName(), "-"));
                    detail.put("dimension", vector == null || vector.getDimension() == null ? null : vector.getDimension());
                    detail.put("embeddingPreview", vector == null ? "-" : limitText(safe(vector.getEmbeddingData(), "-"), 220));
                    return detail;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildVectorDetailRows(DocumentEntity document) {
        if (document == null || document.getChunks() == null || document.getChunks().isEmpty()) {
            return List.of();
        }

        return document.getChunks().stream()
                .filter(Objects::nonNull)
                .map(DocChunkEntity::getVector)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(vector -> vector.getChunk() == null ? null : vector.getChunk().getChunkNo(),
                        Comparator.nullsLast(Integer::compareTo)))
                .map(vector -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("chunkNo", vector.getChunk() == null ? null : vector.getChunk().getChunkNo());
                    detail.put("modelName", safe(vector.getModelName(), "-"));
                    detail.put("dimension", vector.getDimension());
                    detail.put("embeddingPreview", limitText(safe(vector.getEmbeddingData(), "-"), 220));
                    return detail;
                })
                .collect(Collectors.toList());
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String limitText(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(maxLength - 1, 0)) + "...";
    }

    private boolean isApprovedOrPublishedKnowledgeRequest(KnowledgeResponseDto request) {
        String status = safe(request == null ? null : request.getStatus()).toUpperCase(Locale.ROOT);
        return "APPROVED".equals(status) || "PUBLISHED".equals(status);
    }

    private boolean isFailedDocumentStage(DocumentStage stage) {
        return stage == DocumentStage.CHUNK_FAILED || stage == DocumentStage.EMBED_FAILED;
    }


    private List<AdAiDashboardSummaryDto> buildSummaryCards(
            List<AiLogEntity> currentLogs,
            List<AiLogEntity> previousLogs,
            String compareLabel
    ) {
        long currentTotal = currentLogs.size();
        long previousTotal = previousLogs.size();
        long currentChatbot = countByType(currentLogs, AiLogType.CHATBOT);
        long previousChatbot = countByType(previousLogs, AiLogType.CHATBOT);
        long currentAssistant = countAssistantOnly(currentLogs);
        long previousAssistant = countAssistantOnly(previousLogs);
        double currentAverageSeconds = averageDurationSeconds(currentLogs);
        double previousAverageSeconds = averageDurationSeconds(previousLogs);

        List<AdAiDashboardSummaryDto> summaryCards = new ArrayList<>();
        summaryCards.add(buildSummaryCard("총 AI 요청", currentTotal, previousTotal, "count", compareLabel, false));
        summaryCards.add(buildSummaryCard("챗봇 질문", currentChatbot, previousChatbot, "count", compareLabel, false));
        summaryCards.add(buildSummaryCard("AI 비서 사용", currentAssistant, previousAssistant, "count", compareLabel, false));
        summaryCards.add(buildSummaryCard("평균 응답 시간", currentAverageSeconds, previousAverageSeconds, "sec", compareLabel, true));
        return summaryCards;
    }

    private AdAiDashboardSummaryDto buildSummaryCard(
            String label,
            double currentValue,
            double previousValue,
            String unit,
            String compareLabel,
            boolean lowerIsBetter
    ) {
        String valueText = "sec".equals(unit)
                ? String.format(Locale.KOREA, "%.1f", currentValue)
                : String.valueOf((long) currentValue);
        String changeRate = formatChangeRate(currentValue, previousValue);
        String trend = resolveTrend(currentValue, previousValue, lowerIsBetter);

        return AdAiDashboardSummaryDto.builder()
                .label(label)
                .value(valueText)
                .unit(unit)
                .changeRate(changeRate)
                .compareText(compareLabel)
                .trend(trend)
                .build();
    }

    private String formatChangeRate(double currentValue, double previousValue) {
        if (previousValue == 0.0d) {
            if (currentValue == 0.0d) {
                return "0.0%";
            }
            return "+100.0%";
        }

        double rate = ((currentValue - previousValue) / previousValue) * 100.0d;
        String sign = rate > 0 ? "+" : "";
        return sign + String.format(Locale.KOREA, "%.1f%%", rate);
    }

    private String resolveTrend(double currentValue, double previousValue, boolean lowerIsBetter) {
        if (currentValue == previousValue) {
            return "secondary";
        }

        if (lowerIsBetter) {
            return currentValue <= previousValue ? "up" : "down";
        }

        return currentValue >= previousValue ? "up" : "down";
    }

    private List<AdAiDashboardFeatureUsageDto> buildFeatureUsage(List<AiLogEntity> currentLogs, PeriodRange dashboardRange) {
        long chatbot = countByType(currentLogs, AiLogType.CHATBOT);
        long assistant = countAssistantOnly(currentLogs);
        long correction = countCorrectionLogs(currentLogs);
        long template = countTemplateUsage(dashboardRange);
        long total = chatbot + assistant + correction + template;

        return List.of(
                AdAiDashboardFeatureUsageDto.builder()
                        .label("챗봇")
                        .count(chatbot)
                        .percentage(calculatePercentage(chatbot, total))
                        .barClass("bg-primary")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("AI 비서")
                        .count(assistant)
                        .percentage(calculatePercentage(assistant, total))
                        .barClass("bg-success")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("문장 교정")
                        .count(correction)
                        .percentage(calculatePercentage(correction, total))
                        .barClass("bg-warning")
                        .build(),
                AdAiDashboardFeatureUsageDto.builder()
                        .label("템플릿 생성")
                        .count(template)
                        .percentage(calculatePercentage(template, total))
                        .barClass("bg-info")
                        .build()
        );
    }

    private int calculatePercentage(long count, long total) {
        if (total <= 0L) {
            return 0;
        }
        return (int) Math.round(count * 100.0d / total);
    }

    private List<AdAiDashboardUsageTrendDto> buildUsageTrend(PeriodRange range, List<AiLogEntity> currentLogs) {
        if (range == null) {
            return List.of();
        }

        Map<LocalDate, TrendCounter> grouped = new LinkedHashMap<>();
        for (AiLogEntity log : currentLogs) {
            if (log == null || log.getCreatedAt() == null) {
                continue;
            }

            LocalDate currentDate = log.getCreatedAt().atZone(SEOUL).toLocalDate();
            TrendCounter counter = grouped.computeIfAbsent(currentDate, key -> new TrendCounter());
            counter.totalCount++;

            if (log.getType() == AiLogType.CHATBOT) {
                counter.chatbotCount++;
            } else if (log.getType() == AiLogType.ASSISTANT && !isCorrectionLog(log)) {
                counter.assistantCount++;
            }
        }

        List<AdAiDashboardUsageTrendDto> usageTrends = new ArrayList<>();
        LocalDate startDate = range.startAt().toLocalDate();
        for (int i = 0; i < range.days(); i++) {
            LocalDate currentDate = startDate.plusDays(i);
            TrendCounter counter = grouped.getOrDefault(currentDate, new TrendCounter());
            usageTrends.add(AdAiDashboardUsageTrendDto.builder()
                    .label(currentDate.format(DATE_LABEL_FORMATTER))
                    .dateValue(currentDate.toString())
                    .count(counter.totalCount)
                    .totalCount(counter.totalCount)
                    .chatbotCount(counter.chatbotCount)
                    .assistantCount(counter.assistantCount)
                    .build());
        }
        return usageTrends;
    }

    private Page<AiLogEntity> loadRecentLogPage(
            PeriodRange range,
            String department,
            String aiType,
            String result,
            int page,
            int size
    ) {
        if (range == null) {
            return Page.empty(PageRequest.of(0, size));
        }

        String normalizedType = safe(aiType).toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(normalizedType)) {
            return Page.empty(PageRequest.of(Math.max(page - 1, 0), size));
        }

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        return aiLogRepository.findDashboardLogs(
                range.startAt(),
                range.endAt(),
                safe(department),
                safe(aiType),
                safe(result),
                AiLogType.CHATBOT,
                AiLogType.ASSISTANT,
                pageable
        );
    }

    private List<AdAiDashboardRecentLogDto> buildRecentLogs(Page<AiLogEntity> recentLogPage) {
        if (recentLogPage == null || recentLogPage.isEmpty()) {
            return List.of();
        }

        return recentLogPage.getContent().stream()
                .filter(Objects::nonNull)
                .map(this::toRecentLogDto)
                .toList();
    }

    private boolean matchesDepartment(AiLogEntity log, String department) {
        if (department == null || department.isBlank()) {
            return true;
        }

        if (log.getEmployee() == null || log.getEmployee().getDepartment() == null) {
            return false;
        }

        String deptName = log.getEmployee().getDepartment().getDeptName();
        return deptName != null && deptName.equals(department.trim());
    }

    private boolean matchesAiType(AiLogEntity log, String aiType) {
        if (aiType == null || aiType.isBlank()) {
            return true;
        }

        String normalized = aiType.trim().toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(normalized)) {
            return false;
        }

        if ("CORRECTION".equals(normalized)) {
            return log.getType() == AiLogType.ASSISTANT && isCorrectionLog(log);
        }

        return log.getType() != null && log.getType().name().equals(normalized);
    }

    private boolean matchesResult(AiLogEntity log, String result) {
        if (result == null || result.isBlank()) {
            return true;
        }

        String normalized = result.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUCCESS" -> !isFallbackLog(log) && !isFailLog(log);
            case "FALLBACK" -> isFallbackLog(log);
            case "FAIL" -> isFailLog(log);
            default -> true;
        };
    }

    private boolean isFallbackLog(AiLogEntity log) {
        return containsIgnoreCase(log.getResponse(), "fallback=true");
    }

    private boolean isFailLog(AiLogEntity log) {
        return log.getErrorMessage() != null && !log.getErrorMessage().isBlank() && !isFallbackLog(log);
    }

    private boolean isCorrectionLog(AiLogEntity log) {
        return containsIgnoreCase(log.getQuery(), "feature=CORRECTION");
    }

    private AdAiDashboardRecentLogDto toRecentLogDto(AiLogEntity log) {
        String createdAt = log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FORMATTER);
        String durationText = log.getDurationMs() == null
                ? "0.0s"
                : String.format(Locale.KOREA, "%.1fs", log.getDurationMs() / 1000.0d);
        String responseSummary = safe(log.getResponse()).isBlank()
                ? "-"
                : truncate(log.getResponse(), 120);
        String errorMessage = safe(log.getErrorMessage());
        String messageContent = log.getMessage() == null || log.getMessage().getContent() == null
                ? "-"
                : safe(log.getMessage().getContent()).isBlank()
                ? "-"
                : log.getMessage().getContent().trim();

        return AdAiDashboardRecentLogDto.builder()
                .logId(log.getLogId() == null ? "-" : String.valueOf(log.getLogId()))
                .user(resolveEmployeeName(log))
                .department(resolveDepartmentName(log))
                .type(resolveTypeLabel(log))
                .requestSummary(buildRequestSummary(log))
                .responseSummary(responseSummary)
                .result(resolveResultCode(log))
                .resultLabel(resolveResultLabel(log))
                .durationText(durationText)
                .createdAt(createdAt)
                .errorMessage(errorMessage.isBlank() ? "-" : errorMessage)
                .messageContent(messageContent)
                .sessionId(log.getSession() == null || log.getSession().getSessionId() == null
                        ? "-"
                        : String.valueOf(log.getSession().getSessionId()))
                .messageId(log.getMessage() == null || log.getMessage().getMessageId() == null
                        ? "-"
                        : String.valueOf(log.getMessage().getMessageId()))
                .fallback(isFallbackLog(log))
                .build();
    }

    private String resolveEmployeeName(AiLogEntity log) {
        if (log.getEmployee() == null) {
            return "-";
        }

        String name = log.getEmployee().getName();
        return name == null || name.isBlank() ? "-" : name.trim();
    }

    private String resolveDepartmentName(AiLogEntity log) {
        if (log.getEmployee() == null || log.getEmployee().getDepartment() == null) {
            return "-";
        }

        String deptName = log.getEmployee().getDepartment().getDeptName();
        return deptName == null || deptName.isBlank() ? "-" : deptName.trim();
    }

    private String resolveTypeLabel(AiLogEntity log) {
        if (log.getType() == AiLogType.CHATBOT) {
            return "챗봇";
        }

        if (isCorrectionLog(log)) {
            return "문장 교정";
        }

        return "AI 비서";
    }

    private String buildRequestSummary(AiLogEntity log) {
        if (log.getType() == AiLogType.CHATBOT) {
            Integer questionLength = parseIntegerMeta(log.getQuery(), "questionLength");
            return questionLength == null ? "챗봇 요청" : "챗봇 " + questionLength + "자";
        }

        String feature = parseTextMeta(log.getQuery(), "feature");
        Integer inputLength = parseIntegerMeta(log.getQuery(), "inputLength");

        if ("CORRECTION".equalsIgnoreCase(feature)) {
            return inputLength == null ? "교정 요청" : "교정 " + inputLength + "자";
        }

        if ("ASSISTANT_REVISE".equalsIgnoreCase(feature)) {
            return inputLength == null ? "AI 비서 수정" : "AI 비서 수정 " + inputLength + "자";
        }

        if ("ASSISTANT_DRAFT".equalsIgnoreCase(feature)) {
            return inputLength == null ? "AI 비서 초안" : "AI 비서 초안 " + inputLength + "자";
        }

        String query = safe(log.getQuery());
        if (query.isBlank()) {
            return "-";
        }

        return truncate(query, 60);
    }

    private String resolveDurationText(AiLogEntity log) {
        return log.getDurationMs() == null
                ? "0.0s"
                : String.format(Locale.KOREA, "%.1fs", log.getDurationMs() / 1000.0d);
    }

    private String resolveResultCode(AiLogEntity log) {
        if (isFallbackLog(log)) {
            return "FALLBACK";
        }
        if (isFailLog(log)) {
            return "FAIL";
        }
        return "SUCCESS";
    }

    private String resolveResultLabel(AiLogEntity log) {
        return switch (resolveResultCode(log)) {
            case "FALLBACK" -> "대체 응답";
            case "FAIL" -> "실패";
            default -> "성공";
        };
    }

    private String requireText(String value, String message) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safe(String value, String fallback) {
        String normalized = safe(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String safe(Object value, String fallback) {
        String normalized = safe(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String extractRootMessage(Exception exception) {
        if (exception == null) {
            return "알 수 없는 오류가 발생했습니다.";
        }

        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }

        if (message == null || message.isBlank()) {
            return "알 수 없는 오류가 발생했습니다.";
        }

        return message.trim();
    }

    private AiKnowledgeStatus parseReviewStatus(String value) {
        String normalized = safe(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED" -> AiKnowledgeStatus.APPROVED;
            case "REJECTED" -> AiKnowledgeStatus.REJECTED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 또는 반려 상태만 처리할 수 있습니다.");
        };
    }

    private LocalDate parseDate(String value) {
        String trimmed = safe(value);
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String value) {
        String safeValue = safe(value);
        if (safeValue.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(safeValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "-";
        }

        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 1) + "...";
    }

    private Integer parseIntegerMeta(String meta, String key) {
        String value = parseTextMeta(meta, key);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String parseTextMeta(String meta, String key) {
        if (meta == null || meta.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        String[] parts = meta.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            String prefix = key + "=";
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private String decodeMetaValue(String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return "";
        }

        try {
            return URLDecoder.decode(normalized, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return normalized;
        }
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private long countAssistantOnly(List<AiLogEntity> logs) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == AiLogType.ASSISTANT)
                .filter(log -> !isCorrectionLog(log))
                .count();
    }

    private long countCorrectionLogs(List<AiLogEntity> logs) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == AiLogType.ASSISTANT)
                .filter(this::isCorrectionLog)
                .count();
    }

    private long countTemplateUsage(PeriodRange range) {
        if (range == null) {
            return 0L;
        }

        return aiTemplateDashboardRepository.countByCreatedAtBetween(range.startAt(), range.endAt());
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(values[i] == null ? "" : values[i]);
        }
        csv.append("\r\n");
    }

    private String formatCsvCell(String value) {
        String safeValue = value == null ? "" : value;
        String normalized = safeValue.replace("\r\n", "\n").replace("\r", "\n");
        boolean needsQuoting = normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n");
        String escaped = normalized.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private String buildDocumentStatusTitle() {
        LocalDate today = LocalDate.now(SEOUL);
        int weekOfMonth = ((today.getDayOfMonth() - 1) / 7) + 1;
        return today.getMonthValue() + "월 " + weekOfMonth + "주 RAG 문서 처리 상태 요약";
    }

    private List<AdAiDashboardDocumentStatusDto> buildDocumentStatusSummary() {
        List<DocumentEntity> aiRagDocuments = Optional.ofNullable(aiDocumentRepository.findAiRagDocuments())
                .orElse(List.of());

        long total = aiRagDocuments.size();
        long processing = aiRagDocuments.stream()
                .filter(document -> {
                    DocumentStage stage = document.getCurrentStage();
                    return stage == DocumentStage.CHUNKING || stage == DocumentStage.EMBEDDING;
                })
                .count();
        long pending = aiRagDocuments.stream()
                .filter(document -> {
                    DocumentStage stage = document.getCurrentStage();
                    return stage == DocumentStage.UPLOADED || stage == DocumentStage.APPROVAL_PENDING;
                })
                .count();
        long published = aiRagDocuments.stream()
                .filter(document -> document.getCurrentStage() == DocumentStage.PUBLISHED)
                .count();

        return List.of(
                buildDocumentStatus("TOTAL", "전체 RAG 문서", total, "챗봇/RAG 검색 대상으로 관리되는 AI_RAG 문서 전체 수입니다. 온보딩 문서는 포함하지 않습니다.", "text-bg-dark"),
                buildDocumentStatus("PROCESSING", "처리 진행", processing, "본문 추출, 청크 분할 또는 임베딩 생성이 진행 중인 문서 수입니다. CHUNKING, EMBEDDING 상태가 포함됩니다.", "text-bg-primary"),
                buildDocumentStatus("PENDING", "반영 대기", pending, "업로드 후 처리 시작 전이거나, 임베딩 완료 후 관리자 반영을 기다리는 문서 수입니다. UPLOADED, APPROVAL_PENDING 상태가 포함됩니다.", "text-bg-warning"),
                buildDocumentStatus("PUBLISHED", "RAG 반영 완료", published, "챗봇 RAG 검색에 실제로 사용 가능한 문서 수입니다. PUBLISHED 상태만 포함됩니다.", "text-bg-success")
        );
    }

    private AdAiDashboardDocumentStatusDto buildDocumentStatus(
            String key,
            String title,
            long count,
            String description,
            String badgeClass
    ) {
        return AdAiDashboardDocumentStatusDto.builder()
                .key(key)
                .title(title)
                .count(count)
                .description(description)
                .badgeClass(badgeClass)
                .build();
    }

    private List<AiLogEntity> loadLogs(PeriodRange range) {
        if (range == null) {
            return List.of();
        }
        return Optional.ofNullable(
                        aiLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(range.startAt(), range.endAt())
                )
                .orElseGet(List::of);
    }

    private double averageDurationSeconds(List<AiLogEntity> logs) {
        if (logs.isEmpty()) {
            return 0.0d;
        }

        long count = 0L;
        long total = 0L;
        for (AiLogEntity log : logs) {
            if (log.getDurationMs() != null) {
                total += log.getDurationMs();
                count++;
            }
        }

        if (count == 0L) {
            return 0.0d;
        }

        return total / (double) count / 1000.0d;
    }

    private long countByType(List<AiLogEntity> logs, AiLogType type) {
        return logs.stream()
                .filter(Objects::nonNull)
                .filter(log -> log.getType() == type)
                .count();
    }

    private PeriodRange resolveRange(int periodDays, String startDateText, String endDateText) {
        LocalDate startDate = parseLocalDate(startDateText);
        LocalDate endDate = parseLocalDate(endDateText);

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }

            long diff = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
            int days = (int) Math.max(diff, 1L);
            LocalDateTime startAt = startDate.atStartOfDay();
            LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
            LocalDateTime compareEndAt = startAt;
            LocalDateTime compareStartAt = startAt.minusDays(days);
        return new PeriodRange(startAt, endAt, compareStartAt, compareEndAt, "이전 기간과 비교", days);
        }

        int days = periodDays == 30 ? 30 : 7;
        LocalDate today = LocalDate.now(SEOUL);
        String compareLabel = days == 30 ? "이전 30일과 비교" : "이전 7일과 비교";

        LocalDateTime endAt = today.plusDays(1).atStartOfDay();
        LocalDateTime startAt = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime compareEndAt = startAt;
        LocalDateTime compareStartAt = startAt.minusDays(days);

        return new PeriodRange(startAt, endAt, compareStartAt, compareEndAt, compareLabel, days);
    }

    private static final class PeriodRange {
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final LocalDateTime compareStartAt;
        private final LocalDateTime compareEndAt;
        private final String compareLabel;
        private final int days;

        private PeriodRange(
                LocalDateTime startAt,
                LocalDateTime endAt,
                LocalDateTime compareStartAt,
                LocalDateTime compareEndAt,
                String compareLabel,
                int days
        ) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.compareStartAt = compareStartAt;
            this.compareEndAt = compareEndAt;
            this.compareLabel = compareLabel;
            this.days = days;
        }

        private LocalDateTime startAt() {
            return startAt;
        }

        private LocalDateTime endAt() {
            return endAt;
        }

        private LocalDateTime compareStartAt() {
            return compareStartAt;
        }

        private LocalDateTime compareEndAt() {
            return compareEndAt;
        }

        private String compareLabel() {
            return compareLabel;
        }

        private int days() {
            return days;
        }
    }

    private static final class TrendCounter {
        private long totalCount;
        private long chatbotCount;
        private long assistantCount;
    }
}
