/**
 * @FileName : ProgressStatus.java
 * @Description : 온보딩 학습 진행 상태 Enum
 * @Author : 김다솜
 * @Date : 2026. 04. 30
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.30    김다솜        최초 생성 및 온보딩 학습 진행 상태 정의
 * @ 2026.05.12    김다솜        상태 표시 라벨 한글 주석 및 문구 정리
 */
package com.ict06.team1_fin_pj.domain.onboarding.entity;

import lombok.Getter;

@Getter
public enum ProgressStatus {
    NOT_STARTED("미시작"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료");

    private final String label;

    ProgressStatus(String label) {
        this.label = label;
    }
}
