package com.ict06.team1_fin_pj.domain.aiSecretary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiEmbeddingRequestDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.AiEmbeddingResponseDto;
import com.ict06.team1_fin_pj.common.dto.aiSecretary.RagRetrievedChunkDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiDocumentRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.AiKnowledgeRequestRepository;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import com.ict06.team1_fin_pj.domain.auth.repository.EmpRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.DepartmentEntity;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocVectorEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentDomain;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.40d;
    private static final double FALLBACK_SIMILARITY_FLOOR = 0.30d;
    private static final List<DocumentStage> DEFAULT_RETRIEVAL_STAGES = List.of(
            DocumentStage.PUBLISHED
    );

    private final RestTemplate restTemplate;
    private final AiDocumentRepository aiDocumentRepository;
    private final AiKnowledgeRequestRepository aiKnowledgeRequestRepository;
    private final EmpRepository empRepository;
    private final ThreadLocal<Map<String, String>> lastPermissionDeniedInfo = new ThreadLocal<>();

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    @Override
    public List<RagRetrievedChunkDto> retrieveTopChunks(String question, int topK, String empNo) {
        String normalizedQuestion = safe(question);
        String normalizedEmpNo = safe(empNo);
        lastPermissionDeniedInfo.remove();
        log.debug(
                "[RAG retrieval] start empNo={}, topK={}, questionLength={}",
                normalizedEmpNo,
                topK,
                normalizedQuestion.length()
        );
        if (normalizedQuestion.isBlank() || topK <= 0 || normalizedEmpNo.isBlank()) {
            log.debug(
                    "[RAG retrieval] short-circuited empNo={}, topK={}, questionBlank={}",
                    normalizedEmpNo,
                    topK,
                    normalizedQuestion.isBlank()
            );
            return List.of();
        }

        try {
            EmployeeAccessContext employeeAccessContext = resolveEmployeeAccessContext(normalizedEmpNo);
            if (employeeAccessContext == null) {
                log.warn("[RAG] employee access context not found. empNo={}", normalizedEmpNo);
                return List.of();
            }
            log.debug(
                    "[RAG retrieval] userAccess empNo={}, headquarter={}, team={}, position={}",
                    employeeAccessContext.empNo(),
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName()
            );

            AiEmbeddingResponseDto questionEmbedding = requestQuestionEmbedding(normalizedQuestion);
            double[] queryVector = toVector(questionEmbedding.getEmbedding());
            if (queryVector.length == 0) {
                log.debug("[RAG retrieval] empty query vector. empNo={}", normalizedEmpNo);
                return List.of();
            }
            double queryNorm = vectorNorm(queryVector);
            log.debug("[RAG similarity] queryVector dimension={}, norm={}", queryVector.length, queryNorm);

            List<DocumentEntity> documents = aiDocumentRepository
                    .findByDocumentDomainAndCurrentStageInOrderByUpdatedAtDesc(
                            DocumentDomain.AI_RAG,
                            DEFAULT_RETRIEVAL_STAGES
                    );
            log.debug("[RAG] candidate document count={}", documents.size());
            log.debug(
                    "[RAG retrieval] publishedDocuments count={}, docIds={}",
                    documents.size(),
                    documents.stream()
                            .map(DocumentEntity::getDocId)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList())
            );

            Map<Integer, String> targetDeptByDocId = loadTargetDeptByDocId(documents);
            log.debug("[RAG retrieval] targetDeptMap size={}", targetDeptByDocId.size());
            targetDeptByDocId.forEach((docId, targetDept) ->
                    log.debug("[RAG permission] docId={}, targetDept={}", docId, targetDept)
            );
            List<DocumentEntity> allowedDocuments = documents.stream()
                    .filter(document -> isAllowedDocument(document, targetDeptByDocId, employeeAccessContext))
                    .collect(Collectors.toList());
            log.info("[RAG] access allowed document count={}", allowedDocuments.size());
            List<Integer> allowedDocIds = allowedDocuments.stream()
                    .map(DocumentEntity::getDocId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            List<Integer> blockedDocIds = documents.stream()
                    .map(DocumentEntity::getDocId)
                    .filter(java.util.Objects::nonNull)
                    .filter(docId -> !allowedDocIds.contains(docId))
                    .collect(Collectors.toList());
            log.debug(
                    "[RAG retrieval] allowedDocuments count={}, allowedDocIds={}, blockedDocIds={}",
                    allowedDocuments.size(),
                    allowedDocIds,
                    blockedDocIds
            );
            if (!allowedDocuments.isEmpty()) {
                lastPermissionDeniedInfo.remove();
            }

            List<ScoredChunk> scoredChunks = new ArrayList<>();
            int candidateChunkCount = 0;
            int candidateVectorCount = 0;
            for (DocumentEntity document : allowedDocuments) {
                if (document == null || document.getDocId() == null || document.getChunks() == null) {
                    continue;
                }

                for (DocChunkEntity chunk : document.getChunks()) {
                    if (chunk == null || chunk.getChunkId() == null) {
                        continue;
                    }
                    candidateChunkCount++;

                    DocVectorEntity vector = chunk.getVector();
                    if (vector == null || vector.getEmbeddingData() == null || vector.getEmbeddingData().isBlank()) {
                        continue;
                    }
                    candidateVectorCount++;

                    double[] chunkVector;
                    try {
                        chunkVector = parseVector(vector.getEmbeddingData());
                    } catch (Exception e) {
                        log.warn(
                                "[RAG similarity] vector parse failed. vectorId={}, chunkId={}, message={}",
                                vector.getVectorId(),
                                chunk.getChunkId(),
                                e.getMessage()
                        );
                        continue;
                    }

                    double chunkNorm = vectorNorm(chunkVector);
                    log.debug(
                            "[RAG similarity] docId={}, chunkId={}, vectorDimension={}, vectorNorm={}",
                            document.getDocId(),
                            chunk.getChunkId(),
                            chunkVector.length,
                            chunkNorm
                    );

                    if (chunkVector.length == 0) {
                        log.warn(
                                "[RAG similarity] empty doc vector. docId={}, chunkId={}, vectorId={}",
                                document.getDocId(),
                                chunk.getChunkId(),
                                vector.getVectorId()
                        );
                        continue;
                    }

                    if (chunkVector.length != queryVector.length) {
                        log.warn(
                                "[RAG similarity] dimension mismatch. queryDim={}, docDim={}, docId={}, chunkId={}",
                                queryVector.length,
                                chunkVector.length,
                                document.getDocId(),
                                chunk.getChunkId()
                        );
                        continue;
                    }

                    double score = cosineSimilarity(queryVector, chunkVector);
                    if (Double.isNaN(score) || Double.isInfinite(score)) {
                        log.warn(
                                "[RAG similarity] invalid similarity. docId={}, chunkId={}, queryNorm={}, docNorm={}, similarity={}",
                                document.getDocId(),
                                chunk.getChunkId(),
                                queryNorm,
                                chunkNorm,
                                score
                        );
                        continue;
                    }
                    double dot = dotProduct(queryVector, chunkVector);
                    log.debug(
                            "[RAG similarity] docId={}, chunkId={}, dot={}, queryNorm={}, docNorm={}, similarity={}",
                            document.getDocId(),
                            chunk.getChunkId(),
                            dot,
                            queryNorm,
                            chunkNorm,
                            score
                    );

                    scoredChunks.add(new ScoredChunk(document, chunk, score));
                }
            }
            log.debug("[RAG] candidate chunk count={}", scoredChunks.size());
            log.debug("[RAG retrieval] candidateChunks count={}", candidateChunkCount);
            log.debug("[RAG retrieval] candidateVectors count={}", candidateVectorCount);
            log.debug("[RAG retrieval] scoredChunks count={}", scoredChunks.size());

            List<ScoredChunk> thresholdPassedChunks = scoredChunks.stream()
                    .filter(scoredChunk -> scoredChunk.similarityScore() >= DEFAULT_SIMILARITY_THRESHOLD)
                    .collect(Collectors.toList());
            log.debug("[RAG] threshold passed count={}", thresholdPassedChunks.size());

            double rawMaxSimilarityScore = scoredChunks.stream()
                    .mapToDouble(ScoredChunk::similarityScore)
                    .max()
                    .orElse(0.0d);
            log.debug("[RAG similarity] raw max similarity score={}", rawMaxSimilarityScore);

            double topSimilarityScore = thresholdPassedChunks.stream()
                    .mapToDouble(ScoredChunk::similarityScore)
                    .max()
                    .orElse(0.0d);
            log.debug("[RAG] top similarity score={}", topSimilarityScore);
            log.debug(
                    "[RAG retrieval] similarity threshold={}, maxSimilarity={}, rawMaxSimilarity={}, passedCount={}",
                    DEFAULT_SIMILARITY_THRESHOLD,
                    topSimilarityScore,
                    rawMaxSimilarityScore,
                    thresholdPassedChunks.size()
            );

            if (thresholdPassedChunks.isEmpty()) {
                if (!scoredChunks.isEmpty() && rawMaxSimilarityScore >= FALLBACK_SIMILARITY_FLOOR) {
                    ScoredChunk fallbackChunk = scoredChunks.stream()
                            .sorted(Comparator
                                    .comparing(ScoredChunk::similarityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(sc -> sc.document().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(sc -> sc.chunk().getChunkNo(), Comparator.nullsLast(Comparator.naturalOrder())))
                            .findFirst()
                            .orElse(null);

                    if (fallbackChunk != null) {
                        List<RagRetrievedChunkDto> fallbackResult = List.of(toDto(fallbackChunk));
                        log.debug(
                                "[RAG retrieval] threshold fallback applied. rawMaxSimilarity={}, fallbackFloor={}, chunkId={}",
                                rawMaxSimilarityScore,
                                FALLBACK_SIMILARITY_FLOOR,
                                fallbackChunk.chunk().getChunkId()
                        );
                        log.info("[RAG] retrieved chunk count={}", fallbackResult.size());
                        log.debug(
                                "[RAG retrieval] result count={}, docIds={}, chunkIds={}",
                                fallbackResult.size(),
                                fallbackResult.stream()
                                        .map(RagRetrievedChunkDto::getDocumentId)
                                        .filter(java.util.Objects::nonNull)
                                        .distinct()
                                        .collect(Collectors.toList()),
                                fallbackResult.stream()
                                        .map(RagRetrievedChunkDto::getChunkId)
                                        .filter(java.util.Objects::nonNull)
                                        .collect(Collectors.toList())
                        );
                        return fallbackResult;
                    }
                }

                log.debug(
                        "[RAG retrieval] threshold fallback skipped. rawMaxSimilarity={}, fallbackFloor={}",
                        rawMaxSimilarityScore,
                        FALLBACK_SIMILARITY_FLOOR
                );
                log.info("[RAG] retrieved chunk count=0");
                log.debug("[RAG retrieval] result count=0, docIds=[], chunkIds=[]");
                return List.of();
            }

            List<RagRetrievedChunkDto> retrievedChunks = thresholdPassedChunks.stream()
                    .sorted(Comparator
                            .comparing(ScoredChunk::similarityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(sc -> sc.document().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(sc -> sc.chunk().getChunkNo(), Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(topK)
                    .map(this::toDto)
                    .collect(Collectors.toList());
            log.info("[RAG] retrieved chunk count={}", retrievedChunks.size());
            log.debug(
                    "[RAG retrieval] result count={}, docIds={}, chunkIds={}",
                    retrievedChunks.size(),
                    retrievedChunks.stream()
                            .map(RagRetrievedChunkDto::getDocumentId)
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList()),
                    retrievedChunks.stream()
                            .map(RagRetrievedChunkDto::getChunkId)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList())
            );
            return retrievedChunks;
        } catch (Exception e) {
            lastPermissionDeniedInfo.remove();
            log.warn(
                    "[RAG retrieval] failed. empNo={}, questionLength={}, errorType={}, message={}",
                    normalizedEmpNo,
                    normalizedQuestion.length(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            return List.of();
        }
    }

    @Override
    public Map<String, String> consumeLastPermissionDeniedInfo() {
        Map<String, String> deniedInfo = lastPermissionDeniedInfo.get();
        lastPermissionDeniedInfo.remove();
        return deniedInfo == null ? Map.of() : deniedInfo;
    }

    private AiEmbeddingResponseDto requestQuestionEmbedding(String question) {
        String url = normalizeBaseUrl(aiServerBaseUrl) + "/api/ai/embeddings";
        log.debug("[RAG] embedding endpoint=/api/ai/embeddings");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiEmbeddingRequestDto body = AiEmbeddingRequestDto.builder()
                .text(question)
                .build();

        HttpEntity<AiEmbeddingRequestDto> entity = new HttpEntity<>(body, headers);
        ResponseEntity<AiEmbeddingResponseDto> response = restTemplate.postForEntity(
                url,
                entity,
                AiEmbeddingResponseDto.class
        );

        AiEmbeddingResponseDto payload = response.getBody();
        if (payload == null || payload.getEmbedding() == null || payload.getEmbedding().isEmpty()) {
            throw new IllegalStateException("질문 임베딩을 생성하지 못했습니다.");
        }

        return payload;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8000";
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private RagRetrievedChunkDto toDto(ScoredChunk scoredChunk) {
        DocumentEntity document = scoredChunk.document();
        DocChunkEntity chunk = scoredChunk.chunk();

        return RagRetrievedChunkDto.builder()
                .documentId(document.getDocId())
                .documentTitle(safe(document.getTitle()))
                .chunkId(chunk.getChunkId())
                .chunkNo(chunk.getChunkNo())
                .sectionTitle(safe(chunk.getSectionTitle()))
                .content(safe(chunk.getContent()))
                .filePath(safe(document.getFilePath()))
                .similarityScore(scoredChunk.similarityScore())
                .build();
    }

    private double[] parseVector(String embeddingData) throws Exception {
        List<Double> values = OBJECT_MAPPER.readValue(
                embeddingData,
                new TypeReference<List<Double>>() {}
        );

        if (values == null || values.isEmpty()) {
            return new double[0];
        }

        double[] vector = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            vector[i] = value == null ? 0.0d : value;
        }
        return vector;
    }

    private double[] toVector(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new double[0];
        }

        double[] vector = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            vector[i] = value == null ? 0.0d : value;
        }
        return vector;
    }

    private double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return Double.NaN;
        }

        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;

        for (int i = 0; i < left.length; i++) {
            double l = left[i];
            double r = right[i];
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return Double.NaN;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double vectorNorm(double[] vector) {
        if (vector == null || vector.length == 0) {
            return 0.0d;
        }

        double normSquared = 0.0d;
        for (double value : vector) {
            normSquared += value * value;
        }

        return Math.sqrt(normSquared);
    }

    private double dotProduct(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return Double.NaN;
        }

        double dot = 0.0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<Integer, String> loadTargetDeptByDocId(List<DocumentEntity> documents) {
        List<Integer> docIds = documents.stream()
                .map(DocumentEntity::getDocId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (docIds.isEmpty()) {
            return Map.of();
        }

        List<AiKnowledgeRequestEntity> requests = aiKnowledgeRequestRepository
                .findByTargetDoc_DocIdInOrderByCreatedAtDesc(docIds);

        Map<Integer, String> targetDeptByDocId = new LinkedHashMap<>();
        for (AiKnowledgeRequestEntity request : requests) {
            if (request == null || request.getTargetDoc() == null || request.getTargetDoc().getDocId() == null) {
                continue;
            }

            Integer docId = request.getTargetDoc().getDocId();
            targetDeptByDocId.putIfAbsent(docId, safe(request.getTargetDept()));
        }

        return targetDeptByDocId;
    }

    private EmployeeAccessContext resolveEmployeeAccessContext(String empNo) {
        EmpEntity employee = empRepository.findByEmpNo(empNo).orElse(null);
        if (employee == null) {
            return null;
        }

        DepartmentEntity department = employee.getDepartment();
        String teamName = department == null ? "" : safe(department.getDeptName());
        String headquarterName = "";

        if (department != null) {
            DepartmentEntity parentDept = department.getParentDept();
            headquarterName = parentDept == null
                    ? safe(department.getDeptName())
                    : safe(parentDept.getDeptName());
        }

        String positionName = employee.getPosition() == null ? "" : safe(employee.getPosition().getPositionName());

        return new EmployeeAccessContext(empNo, headquarterName, teamName, positionName);
    }

    private boolean isAllowedDocument(
            DocumentEntity document,
            Map<Integer, String> targetDeptByDocId,
            EmployeeAccessContext employeeAccessContext
    ) {
        if (document == null || document.getDocId() == null) {
            return false;
        }

        String targetDept = safe(targetDeptByDocId.get(document.getDocId()));
        if (targetDept.isBlank() || "-".equals(targetDept)) {
            log.debug(
                    "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                    document.getDocId(),
                    "",
                    "",
                    "",
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName(),
                    true,
                    "public"
            );
            return true;
        }

        TargetDeptCondition condition = parseTargetDeptCondition(targetDept);
        if (condition.parseFailed()) {
            recordPermissionDeniedInfoIfAbsent(document, targetDept, employeeAccessContext, "parse-failed");
            log.debug(
                    "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                    document.getDocId(),
                    condition.requiredHeadquarter(),
                    condition.requiredTeamsText(),
                    condition.requiredPositionsText(),
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName(),
                    false,
                    "parse-failed"
            );
            return false;
        }

        log.debug(
                "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}",
                document.getDocId(),
                condition.requiredHeadquarter(),
                condition.requiredTeamsText(),
                condition.requiredPositionsText()
        );

        if (!isWildcard(condition.requiredHeadquarter())
                && !condition.requiredHeadquarter().equals(employeeAccessContext.headquarterName())) {
            recordPermissionDeniedInfoIfAbsent(document, targetDept, employeeAccessContext, "headquarter-mismatch");
            log.debug(
                    "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                    document.getDocId(),
                    condition.requiredHeadquarter(),
                    condition.requiredTeamsText(),
                    condition.requiredPositionsText(),
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName(),
                    false,
                    "headquarter-mismatch"
            );
            return false;
        }

        if (!hasWildcard(condition.requiredTeams())
                && !matchesAny(condition.requiredTeams(), employeeAccessContext.teamName())) {
            recordPermissionDeniedInfoIfAbsent(document, targetDept, employeeAccessContext, "team-mismatch");
            log.debug(
                    "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                    document.getDocId(),
                    condition.requiredHeadquarter(),
                    condition.requiredTeamsText(),
                    condition.requiredPositionsText(),
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName(),
                    false,
                    "team-mismatch"
            );
            return false;
        }

        if (!hasWildcard(condition.requiredPositions())
                && !matchesAny(condition.requiredPositions(), employeeAccessContext.positionName())) {
            recordPermissionDeniedInfoIfAbsent(document, targetDept, employeeAccessContext, "position-mismatch");
            log.debug(
                    "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                    document.getDocId(),
                    condition.requiredHeadquarter(),
                    condition.requiredTeamsText(),
                    condition.requiredPositionsText(),
                    employeeAccessContext.headquarterName(),
                    employeeAccessContext.teamName(),
                    employeeAccessContext.positionName(),
                    false,
                    "position-mismatch"
            );
            return false;
        }

        log.debug(
                "[RAG permission] docId={}, parsedHeadquarter={}, parsedTeam={}, parsedPosition={}, userHeadquarter={}, userTeam={}, userPosition={}, allowed={}, reason={}",
                document.getDocId(),
                condition.requiredHeadquarter(),
                condition.requiredTeamsText(),
                condition.requiredPositionsText(),
                employeeAccessContext.headquarterName(),
                employeeAccessContext.teamName(),
                employeeAccessContext.positionName(),
                true,
                "matched"
        );
        return true;
    }

    private void recordPermissionDeniedInfoIfAbsent(
            DocumentEntity document,
            String targetDept,
            EmployeeAccessContext employeeAccessContext,
            String deniedReason
    ) {
        if (document == null || document.getDocId() == null || employeeAccessContext == null) {
            return;
        }
        if (lastPermissionDeniedInfo.get() != null && !lastPermissionDeniedInfo.get().isEmpty()) {
            return;
        }

        Map<String, String> deniedInfo = new LinkedHashMap<>();
        deniedInfo.put("permissionDenied", "true");
        deniedInfo.put("deniedReason", safe(deniedReason));
        deniedInfo.put("deniedDocId", String.valueOf(document.getDocId()));
        deniedInfo.put("deniedDocTitle", safe(document.getTitle()));
        deniedInfo.put("userHeadquarter", safe(employeeAccessContext.headquarterName()));
        deniedInfo.put("userTeam", safe(employeeAccessContext.teamName()));
        deniedInfo.put("userPosition", safe(employeeAccessContext.positionName()));
        deniedInfo.put("targetDept", safe(targetDept));
        lastPermissionDeniedInfo.set(deniedInfo);
    }

    private TargetDeptCondition parseTargetDeptCondition(String targetDept) {
        String normalized = safe(targetDept);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return new TargetDeptCondition("", List.of(), List.of(), false, false);
        }

        String requiredHeadquarter = extractConditionValue(normalized, "본부", "대상 본부");
        String requiredTeam = extractConditionValue(normalized, "팀", "대상 팀");
        String requiredPosition = extractConditionValue(normalized, "직책", "직책 기준");
        List<String> requiredTeams = splitConditionValues(requiredTeam);
        List<String> requiredPositions = splitConditionValues(requiredPosition);

        boolean hasCondition =
                normalized.contains("본부:")
                        || normalized.contains("대상 본부:")
                        || normalized.contains("팀:")
                        || normalized.contains("대상 팀:")
                        || normalized.contains("직책:")
                        || normalized.contains("직책 기준:")
                        || normalized.contains("사원:")
                        || normalized.contains("선택 사원:");

        boolean parseFailed = hasCondition
                && requiredHeadquarter.isBlank()
                && requiredTeams.isEmpty()
                && requiredPositions.isEmpty();

        return new TargetDeptCondition(
                requiredHeadquarter,
                requiredTeams,
                requiredPositions,
                hasCondition,
                parseFailed
        );
    }

    private String extractConditionValue(String source, String... labels) {
        String normalized = safe(source);
        if (normalized.isBlank()) {
            return "";
        }

        for (String part : normalized.split("/")) {
            String trimmed = safe(part);
            for (String label : labels) {
                String prefix = label + ":";
                if (trimmed.startsWith(prefix)) {
                    return safe(trimmed.substring(prefix.length()));
                }
            }
        }

        return "";
    }

    private List<String> splitConditionValues(String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (String token : normalized.split(",")) {
            String trimmed = safe(token);
            if (!trimmed.isBlank() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }

        return values;
    }

    private boolean isWildcard(String value) {
        String normalized = safe(value);
        if (normalized.isBlank() || "-".equals(normalized)) {
            return true;
        }

        if ("전원".equals(normalized) || "전체".equals(normalized) || "ALL".equalsIgnoreCase(normalized)) {
            return true;
        }

        return normalized.startsWith("전원(") && normalized.endsWith(")");
    }

    private boolean hasWildcard(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }

        return values.stream().anyMatch(this::isWildcard);
    }

    private boolean matchesAny(List<String> values, String actualValue) {
        String normalizedActual = safe(actualValue);
        if (values == null || values.isEmpty()) {
            return true;
        }

        return values.stream()
                .map(this::safe)
                .anyMatch(value -> value.equals(normalizedActual));
    }

    private record ScoredChunk(DocumentEntity document, DocChunkEntity chunk, double similarityScore) {
    }

    private record TargetDeptCondition(
            String requiredHeadquarter,
            List<String> requiredTeams,
            List<String> requiredPositions,
            boolean hasCondition,
            boolean parseFailed
    ) {
        private String requiredTeamsText() {
            return requiredTeams == null || requiredTeams.isEmpty()
                    ? ""
                    : String.join(", ", requiredTeams);
        }

        private String requiredPositionsText() {
            return requiredPositions == null || requiredPositions.isEmpty()
                    ? ""
                    : String.join(", ", requiredPositions);
        }
    }

    private record EmployeeAccessContext(String empNo, String headquarterName, String teamName, String positionName) {
    }
}
