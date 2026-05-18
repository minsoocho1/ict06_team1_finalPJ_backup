package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 지급/공제항목 설정 저장 요청 DTO
@Data
@NoArgsConstructor
public class PayItemSettingSaveRequestDTO {

    // 모달에서 최종 등록할 항목 목록
    private List<Item> items;

    @Data
    @NoArgsConstructor
    public static class Item {

        // 기존 항목이면 ID 있음, 신규 항목이면 null
        private Integer itemSettingId;

        // 항목명
        private String itemName;

        // ALLOWANCE / DEDUCTION
        private String itemType;

        // TAXABLE / NON_TAXABLE
        // 공제항목은 null
        private String taxType;

        // MEAL / CAR / RESEARCH / CHILDCARE / OVERSEAS
        // 과세항목/공제항목은 null
        private String nonTaxCode;

        // OVERTIME / ABSENCE
        // 일반항목이면 null
        private String linkedAttendanceType;
    }
}

