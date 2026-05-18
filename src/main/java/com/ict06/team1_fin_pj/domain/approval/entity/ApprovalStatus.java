package com.ict06.team1_fin_pj.domain.approval.entity;

import lombok.Getter;

@Getter
public enum ApprovalStatus {
    DRAFT("임시저장"),
    PENDING("대기"),
    IN_PROGRESS("진행"),
    COMPLETED("완료"),
    REJECTED("반려"),
    CANCELED("취소");

    private final String label;

    ApprovalStatus(String label) {
        this.label = label;
    }
}
