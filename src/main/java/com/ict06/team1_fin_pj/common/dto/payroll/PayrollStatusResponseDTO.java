package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 급여대장 상태 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollStatusResponseDTO {

    // 급여대장 PK
    private Long payrollId;

    // 상태 코드
    private String payrollStatus;

    // 상태명
    private String payrollStatusName;

    // 기본급
    private BigDecimal baseSalary;

    // 지급일
    private String payDate;

    // 수정 가능 여부
    private boolean editable;

    // 삭제 가능 여부
    private boolean deletable;

    // 계산 미리보기 가능 여부
    private boolean previewAvailable;

    // 확정 가능 여부
    private boolean confirmAvailable;

    // 지급확정 가능 여부
    private boolean payConfirmAvailable;

    // 입력 초기화 가능 여부
    private boolean resetAvailable;

    // 소득세
    private BigDecimal incomeTax;

    // 지방소득세
    private BigDecimal localIncomeTax;

    // 총 공제액
    private BigDecimal totalDeduction;

    // 총 지급액
    private BigDecimal totalGross;

    // 실수령액
    private BigDecimal netSalary;

    private BigDecimal nationalPensionAmount;
    private BigDecimal healthInsuranceAmount;
    private BigDecimal longTermCareAmount;
    private BigDecimal employmentInsuranceAmount;
    private BigDecimal totalInsurance;
}
