package com.ict06.team1_fin_pj.domain.aiSecretary.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    REPORT("보고서"),
    MINUTES("회의록"),
    APPROVAL("결재 사유");

    private final String label;
}