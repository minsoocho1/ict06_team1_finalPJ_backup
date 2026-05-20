/**
 * @FileName : DocumentProcessingService.java
 * @Description : 관리자 문서 RAG 처리 서비스
 * @Author : 김다솜
 * @Date : 2026. 05. 13
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -----------------------------------------------
 * @ 2026.05.11    김다솜        문서 본문 추출, 청크 분할, 벡터 생성, 단계 갱신 처리 구현
 * @ 2026.05.12    김다솜        문서 삭제, 기존 청크 초기화, UTF-8 특수문자 정제 처리 추가
 * @ 2026.05.13    김다솜        문서 처리 완료 후 연결 PDF 콘텐츠 퀴즈 자동 생성 연계 추가
 * @ 2026.05.14    김다솜        문서 요약 미리보기 생성 및 특수문자 정제 로직 강화
 * @ 2026.05.18    김다솜        문서/RAG 재처리 응답 docId 검증, 다중 연결 콘텐츠 퀴즈 자동 생성 및 깨진 메시지 복구
 * @ 2026.05.19    김다솜        RAG 처리 실패 알림에 문서 ID 전달 추가
 */
package com.ict06.team1_fin_pj.domain.onboarding.service;

import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentChunkResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentProcessRequestDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.AiDocumentProcessResponseDto;
import com.ict06.team1_fin_pj.common.dto.onboarding.DocumentProcessingResultDto;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.DocumentProcessLogEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.ProcessStage;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.ProcessStatus;
import com.ict06.team1_fin_pj.domain.aiSecretary.repository.DocumentProcessLogRepository;
import com.ict06.team1_fin_pj.domain.employee.entity.EmpEntity;
import com.ict06.team1_fin_pj.domain.evaluation.service.AdEvaluationService;
import com.ict06.team1_fin_pj.domain.notification.service.NotificationServiceImpl;
import com.ict06.team1_fin_pj.domain.onboarding.entity.ContentType;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocChunkEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocVectorEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.entity.DocumentStage;
import com.ict06.team1_fin_pj.domain.onboarding.entity.OnContentEntity;
import com.ict06.team1_fin_pj.domain.onboarding.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessLogRepository documentProcessLogRepository;
    private final AdEvaluationService adEvaluationService;
    private final NotificationServiceImpl notificationService;
    private final RestTemplate restTemplate;
    private final EntityManager entityManager;

    @Value("${ai.server.base-url:http://localhost:8000}")
    private String aiServerBaseUrl;

    @Transactional
    public DocumentProcessingResultDto processDocument(Integer docId, EmpEntity processedBy) {
        DocumentEntity document = findDocument(docId);

        document.updateStage(DocumentStage.CHUNKING);
        documentRepository.saveAndFlush(document);

        DocumentProcessLogEntity chunkLog = createLog(document, processedBy, ProcessStage.CHUNK);
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
            documentRepository.saveAndFlush(document);

            // [트리거] 1. 청크 분할 실패 시 관리자 알림 발송
            notificationService.sendRagFailureNotification(document.getDocId(), document.getTitle(), "청크 분할");

            return DocumentProcessingResultDto.builder()
                    .success(false)
                    .message("문서 청크 생성에 실패했습니다: " + errorMessage)
                    .stage(DocumentStage.CHUNK_FAILED)
                    .chunkCount(0)
                    .vectorCount(0)
                    .build();
        }

        document.updateStage(DocumentStage.EMBEDDING);
        documentRepository.saveAndFlush(document);

        DocumentProcessLogEntity embedLog = createLog(document, processedBy, ProcessStage.EMBED);
        documentProcessLogRepository.saveAndFlush(embedLog);

        try {
            deleteExistingChunks(document);
            document = findDocument(docId);

            applyChunks(document, response.getChunks());
            document.updateSummaryPreview(sanitizeText(response.getExtractedTextPreview()));
            document.updateStage(DocumentStage.PUBLISHED);
            documentRepository.saveAndFlush(document);

            embedLog.markSuccess();
            documentProcessLogRepository.saveAndFlush(embedLog);

            int chunkCount = document.getChunks().size();
            int vectorCount = (int) document.getChunks().stream()
                    .filter(chunk -> chunk.getVector() != null)
                    .count();

            Integer generatedQuestionCount = tryGenerateQuestionsForLinkedContent(document);
            String successMessage = generatedQuestionCount != null
                    ? String.format("문서 청크와 벡터 생성이 완료되었습니다. 연결 콘텐츠 퀴즈 %d문항도 생성되었습니다.", generatedQuestionCount)
                    : "문서 청크와 벡터 생성이 완료되었습니다.";

            return DocumentProcessingResultDto.builder()
                    .success(true)
                    .message(successMessage)
                    .stage(DocumentStage.PUBLISHED)
                    .chunkCount(chunkCount)
                    .vectorCount(vectorCount)
                    .build();
        } catch (Exception e) {
            String errorMessage = extractRootMessage(e);
            embedLog.markFail(errorMessage);
            documentProcessLogRepository.saveAndFlush(embedLog);
            document.updateStage(DocumentStage.EMBED_FAILED);
            documentRepository.saveAndFlush(document);

            // [트리거] 2. 벡터 임베딩 처리 실패 시 관리자 알림 발송
            notificationService.sendRagFailureNotification(document.getDocId(), document.getTitle(), "벡터 임베딩");

            return DocumentProcessingResultDto.builder()
                    .success(false)
                    .message("문서 벡터 생성에 실패했습니다: " + errorMessage)
                    .stage(DocumentStage.EMBED_FAILED)
                    .chunkCount(0)
                    .vectorCount(0)
                    .build();
        }
    }

    private DocumentEntity findDocument(Integer docId) {
        return documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));
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

    private void applyChunks(DocumentEntity document, List<AiDocumentChunkResponseDto> chunkDtos) {
        for (AiDocumentChunkResponseDto chunkDto : chunkDtos) {
            DocChunkEntity chunk = DocChunkEntity.builder()
                    .chunkNo(chunkDto.getChunkNo())
                    .content(sanitizeText(chunkDto.getContent()))
                    .tokenCount(chunkDto.getTokenCount())
                    .sectionTitle(sanitizeText(chunkDto.getSectionTitle()))
                    .build();

            if (chunkDto.getEmbeddingData() != null && !chunkDto.getEmbeddingData().isBlank()) {
                DocVectorEntity vector = DocVectorEntity.builder()
                        .embeddingData(sanitizeText(chunkDto.getEmbeddingData()))
                        .modelName(sanitizeText(chunkDto.getModelName() != null ? chunkDto.getModelName() : "hash-embedding-v1"))
                        .dimension(chunkDto.getDimension() != null ? chunkDto.getDimension() : 256)
                        .build();
                chunk.setVector(vector);
            }

            document.addChunk(chunk);
        }
    }

    private Integer tryGenerateQuestionsForLinkedContent(DocumentEntity document) {
        try {
            List<OnContentEntity> linkedContents = findLinkedContents(document);
            if (linkedContents.isEmpty()) {
                return null;
            }

            return linkedContents.stream()
                    .mapToInt(content -> adEvaluationService.generateAndSaveQuestionsForContent(content.getContentId()))
                    .sum();
        } catch (Exception e) {
            return null;
        }
    }

    private List<OnContentEntity> findLinkedContents(DocumentEntity document) {
        if (document.getRelatedContents() != null && !document.getRelatedContents().isEmpty()) {
            return document.getRelatedContents().stream()
                    .filter(this::isQuizGeneratableContent)
                    .toList();
        }

        if (document.getRelatedContent() != null && isQuizGeneratableContent(document.getRelatedContent())) {
            return List.of(document.getRelatedContent());
        }

        return List.of();
    }

    private boolean isQuizGeneratableContent(OnContentEntity content) {
        return content.getType() == ContentType.PDF || content.getType() == ContentType.LINK;
    }

    private DocumentProcessLogEntity createLog(DocumentEntity document, EmpEntity processedBy, ProcessStage stage) {
        return DocumentProcessLogEntity.builder()
                .document(document)
                .stage(stage)
                .status(ProcessStatus.PROCESSING)
                .startedAt(LocalDateTime.now())
                .processedBy(processedBy)
                .build();
    }

    @Transactional
    public void deleteDocument(Integer docId) {
        DocumentEntity document = findDocument(docId);

        documentProcessLogRepository.deleteByDocument_DocId(docId);
        documentProcessLogRepository.flush();

        deleteExistingChunks(document);

        documentRepository.delete(document);
        documentRepository.flush();
    }

    private void deleteExistingChunks(DocumentEntity document) {
        Integer docId = document.getDocId();
        if (docId == null) {
            return;
        }

        document.clearChunks();
        entityManager.flush();

        entityManager.createNativeQuery("""
                DELETE FROM doc_vector
                WHERE chunk_id IN (
                    SELECT chunk_id
                    FROM doc_chunks
                    WHERE doc_id = :docId
                )
                """)
                .setParameter("docId", docId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM doc_chunks WHERE doc_id = :docId")
                .setParameter("docId", docId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }

        // PostgreSQL UTF-8 저장을 방해하는 NULL/control 문자를 제거한다.
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "").trim();
    }

    private String extractRootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current.getMessage() != null ? current.getMessage() : throwable.getClass().getSimpleName();
    }
}
