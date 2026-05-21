package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 관리자 급여명세서 조회 응답 DTO
@Data
@NoArgsConstructor
public class PayrollStatementResponseDTO {

    // 사원 정보
    private String empNo;
    private String empName;
    private String deptName;
    private String parentDeptName;
    private String positionName;
    private String gradeId;
    private String gradeDescription;
    private String empStatus;
    private String empStatusName;
    private LocalDate hireDate;
    private String bank;
    private String accountNo;

    // 조회 년월
    private Integer payYear;
    private Integer payMonth;
    private String payMonthText;

    // 급여대장 정보
    private Integer payrollId;
    private String payrollStatus;
    private String payrollStatusName;
    private LocalDate payDate;

    // 계산 결과
    private BigDecimal baseSalary;
    private BigDecimal totalGross;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;

    // 과세대상금액
    private BigDecimal taxableIncome;

    // 4대보험 상세 금액
    private BigDecimal nationalPensionAmount;
    private BigDecimal healthInsuranceAmount;
    private BigDecimal longTermCareAmount;
    private BigDecimal employmentInsuranceAmount;
    private BigDecimal totalInsurance;

    // 원천징수세
    private BigDecimal incomeTax;
    private BigDecimal localIncomeTax;

    // 화면 제어
    private boolean statementAvailable;
    private boolean pdfAvailable;
    private String unavailableMessage;

    // 지급/공제 항목
    private List<Item> allowanceItems = new ArrayList<>();
    private List<Item> deductionItems = new ArrayList<>();

    // 지급/공제 내역 줄수 맞춤용 빈 행 개수
    private Integer emptyAllowanceRowCount = 0;
    private Integer emptyDeductionRowCount = 0;

    @Data
    @NoArgsConstructor
    public static class Item {

        private String itemName;
        private String itemType;
        private BigDecimal amount;
        private String taxType;
        private String nonTaxCode;
        private String linkedAttendanceType;
    }
}