package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// 급여 계산 미리보기 응답 DTO
@Data
@Builder
public class PayrollPreviewResponseDTO {
    private String empNo;
    private String empName;
    private String deptName;
    private String positionName;
    private String payMonth;

    // 지급 합계
    private BigDecimal baseSalary;
    private BigDecimal taxableAllowance;
    private BigDecimal nonTaxableAllowance;
    private BigDecimal totalAllowance;
    private BigDecimal totalGross;
    private BigDecimal taxableIncome;

    // 세금/보험/공제
    private BigDecimal nationalPensionAmount;
    private BigDecimal healthInsuranceAmount;
    private BigDecimal longTermCareAmount;
    private BigDecimal employmentInsuranceAmount;
    private BigDecimal totalInsurance;

    private BigDecimal incomeTax;
    private BigDecimal localIncomeTax;
    private BigDecimal otherDeduction;
    private BigDecimal totalDeduction;

    // 최종 실수령액
    private BigDecimal netSalary;

    // 모달 상세 표시용
    private List<InsuranceRow> insuranceRows;

    // 지급/공제항목 계산 상세
    private List<ItemRow> itemRows;

    @Data
    @Builder
    public static class InsuranceRow {
        private String name;
        private BigDecimal baseAmount;
        private BigDecimal rate;
        private BigDecimal amount;
        private String formula;
    }

    @Data
    @Builder
    public static class ItemRow {

        private String itemNameSnapshot;
        private String itemType;

        // 입력값
        private BigDecimal inputAmount;

        // 실제 계산 반영 금액
        private BigDecimal calculatedAmount;

        private String taxType;
        private String nonTaxCode;
        private String linkedAttendanceType;

        private BigDecimal taxableAmount;
        private BigDecimal nonTaxableAmount;
        private Boolean validNonTax;

        // 모달 표시용 계산식
        private String formula;
    }
}
