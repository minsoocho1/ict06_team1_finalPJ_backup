package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import com.ict06.team1_fin_pj.domain.aiSecretary.entity.AiTemplateRequestEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TemplateRequestResponseDto {

    private Integer requestId;

    private String title;

    private String type;

    private String category;

    private String dept;

    private String situation;

    private String tone;

    private String description;

    private String content;

    private String status;

    private String statusLabel;

    private Boolean reflected;

    private String adminComment;

    private List<String> preview;

    private Map<String, Object> options;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    public static TemplateRequestResponseDto from(AiTemplateRequestEntity entity) {
        return TemplateRequestResponseDto.builder()
                .requestId(entity.getRequestId())
                .title(entity.getTitle())
                .type(entity.getType() == null ? "REPORT" : entity.getType().name())
                .category(entity.getCategory())
                .dept(entity.getDept())
                .situation(entity.getSituation())
                .tone(entity.getTone())
                .description(entity.getDescription())
                .content(entity.getContent())
                .status(entity.getStatus() == null ? "PENDING" : entity.getStatus().name())
                .statusLabel(entity.getStatus() == null ? "검토 대기" : entity.getStatus().getLabel())
                .reflected(entity.getStatus() != null && "APPROVED".equals(entity.getStatus().name()))
                .adminComment(entity.getAdminComment())
                .preview(entity.getPreviewJson())
                .options(entity.getOptionsJson())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}