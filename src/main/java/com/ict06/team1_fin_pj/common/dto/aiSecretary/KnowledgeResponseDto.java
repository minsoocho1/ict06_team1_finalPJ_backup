package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeRequestEntity;
import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiKnowledgeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class KnowledgeResponseDto {

    private Long knowledgeRequestId;

    private String requesterNo;

    private String requesterName;

    private String reviewerName;

    private String title;

    private String requestType;

    private String category;

    private String targetDept;

    private String reason;

    private String sampleQuestion;

    private String referenceUrl;

    private String accessLevel;

    private String status;

    private String statusLabel;

    private String adminComment;

    private Integer targetDocId;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static KnowledgeResponseDto from(AiKnowledgeRequestEntity entity) {
        AiKnowledgeStatus status = entity.getStatus() == null ? AiKnowledgeStatus.PENDING : entity.getStatus();

        return KnowledgeResponseDto.builder()
                .knowledgeRequestId(entity.getRequestId())
                .requesterNo(entity.getRequester() == null ? null : entity.getRequester().getEmpNo())
                .requesterName(entity.getRequester() == null ? null : entity.getRequester().getName())
                .reviewerName(entity.getReviewer() == null ? null : entity.getReviewer().getName())
                .title(entity.getTitle())
                .requestType(entity.getRequestType())
                .category(entity.getCategory())
                .targetDept(entity.getTargetDept())
                .reason(entity.getReason())
                .sampleQuestion(entity.getSampleQuestion())
                .referenceUrl(entity.getReferenceUrl())
                .accessLevel(entity.getAccessLevel())
                .status(status.name())
                .statusLabel(resolveStatusLabel(status))
                .adminComment(entity.getAdminComment())
                .targetDocId(entity.getTargetDoc() == null ? null : entity.getTargetDoc().getDocId())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static String resolveStatusLabel(AiKnowledgeStatus status) {
        if (status == null) {
            return "대기중";
        }

        return switch (status) {
            case PENDING -> "대기중";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case PUBLISHED -> "반영 완료";
        };
    }
}
