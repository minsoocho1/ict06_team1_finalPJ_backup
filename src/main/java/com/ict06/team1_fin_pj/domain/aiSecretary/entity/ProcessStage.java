package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import lombok.Getter;

@Getter
public enum ProcessStage {
    UPLOAD,
    CHUNK,
    EMBED,
    INDEX,
    APPROVAL,
    PUBLISH
}
