package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 기본급 조회 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollBaseSalaryResponseDTO {

    // 기본급
    private BigDecimal baseSalary;

    // 기본급 출처
    private String salarySource;

    // 기본급 정책 존재 여부
    private boolean policyExists;

    // 정책 변경 여부
    private boolean policyChanged;

    // 정책 경고 메시지
    private String warningMessage;

    // 급여등급 코드
    private String gradeId;

    // 정책 경고 표시 여부
    private boolean warningRequired;

    // 정책 선택 강제 여부
    private boolean policyDecisionRequired;

    // 현재 기준 정책 기본급
    private BigDecimal policyBaseSalary;

    // 저장된 기본급
    private BigDecimal savedBaseSalary;

    // 현재 기준 정책 수정일
    private LocalDateTime policyUpdatedAt;

    // 정책 경고 처리 여부
    private boolean policyDecisionCompleted;

    // 저장된 급여대장 수정일
    private LocalDateTime payrollUpdatedAt;

    // 기본급 정책 등록일
    private LocalDateTime policyCreatedAt;

}
