package com.ict06.team1_fin_pj.domain.onboarding.entity;

import lombok.Getter;

@Getter
public enum DocumentStage {
    UPLOADED,
    CHUNKING,
    CHUNK_FAILED,
    EMBEDDING,
    EMBED_FAILED,
    APPROVAL_PENDING,
    PUBLISHED
}
