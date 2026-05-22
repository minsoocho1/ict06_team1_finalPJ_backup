package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 급여요약 전체조회 목록 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryResponseDTO {

    // 급여대장 PK, 미작성(NEW)이면 null
    private Long payrollId;

    // 사원 정보
    private String empNo;
    private String empName;

    // 부서 정보
    private Integer deptId;
    private String deptName;
    private String parentDeptName;

    // 직급 정보
    private Integer positionId;
    private String positionName;

    // 조회월 yyyy-MM
    private String payMonth;

    // 급여 합계
    private BigDecimal totalGross;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;

    // 상태 코드/표시명
    private String payrollStatus;
    private String payrollStatusName;
}
