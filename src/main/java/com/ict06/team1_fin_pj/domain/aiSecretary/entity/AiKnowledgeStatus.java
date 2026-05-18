package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import lombok.Getter;

@Getter
public enum AiKnowledgeStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    PUBLISHED("반영완료");

    private final String description;

    AiKnowledgeStatus(String description) {
        this.description = description;
    }
}
