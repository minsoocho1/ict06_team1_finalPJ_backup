package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// 급여대장 지급/공제항목 조회 응답 DTO
@Data
@NoArgsConstructor
public class PayrollItemLoadResponseDTO {

    // 지급/공제항목 설정 변경 여부
    private boolean itemSettingChanged;

    // 변경 알림 메시지
    private String warningMessage;

    // 지급/공제항목 목록
    private List<Item> items;

    @Data
    @NoArgsConstructor
    public static class Item {

        // PAY_ITEM_SETTING ID
        private Integer itemSettingId;

        // 항목명
        private String itemNameSnapshot;

        // ALLOWANCE / DEDUCTION
        private String itemType;

        // 일반항목: 실제 금액
        // 근태연동 지급항목: 60분당 단가
        // 근태연동 공제항목: 하루당 공제단가
        private BigDecimal amount;

        // TAXABLE / NON_TAXABLE
        private String taxType;

        // MEAL / CAR / RESEARCH / CHILDCARE / OVERSEAS
        private String nonTaxCode;

        // OVERTIME / ABSENCE 등
        // 현재 설정 기준으로 내려준다.
        private String linkedAttendanceType;
    }
}
