package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 급여대장 저장 요청 DTO
// - 2차 백단에서는 DTO를 최소화하기 위해 저장 요청 DTO 하나 안에 항목 DTO를 함께 둔다.
@Data
@NoArgsConstructor
public class PayrollSaveRequestDTO {
    // 사번
    private String empNo;

    // 작성년도
    private Integer payYear;

    // 작성월
    private Integer payMonth;

    // 기본급
    private BigDecimal baseSalary;

    // 부양가족 수
    private Integer familyCount;

    // 지급일
    private LocalDate payDate;

    // 지급/공제항목 목록
    private List<Item> items;

    private BigDecimal nationalPensionAmount;
    private BigDecimal healthInsuranceAmount;
    private BigDecimal longTermCareAmount;
    private BigDecimal employmentInsuranceAmount;
    private BigDecimal totalInsurance;

    private BigDecimal incomeTax;
    private BigDecimal localIncomeTax;
    private BigDecimal totalDeduction;
    private BigDecimal totalGross;
    private BigDecimal netSalary;

    // 지급/공제항목 저장 DTO
    @Data
    @NoArgsConstructor
    public static class Item {

        // PAY_ITEM_SETTING ID
        private Integer itemSettingId;

        // 저장 당시 항목명
        private String itemNameSnapshot;

        // ALLOWANCE / DEDUCTION
        private String itemType;

        // 일반항목 금액 또는 근태연동 기준 단가
        private BigDecimal amount;

        // TAXABLE / NON_TAXABLE
        private String taxType;

        // MEAL / CAR / RESEARCH / CHILDCARE / OVERSEAS
        private String nonTaxCode;

        // OVERTIME / ABSENCE - 일반항목이면 null
        private String linkedAttendanceType;

    }
}
