package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//급여대장 사원 검색 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEmployeeSearchResponseDTO {

    // 사원 PK
    private String empId;

    // 사번
    private String empNo;

    // 사원명
    private String empName;

    // 부서 PK
    private Integer deptId;

    // 본부/부서명
    private String deptName;
    private String parentDeptName;

    // 직급명
    private String positionName;

    // 급여등급
    private String gradeId;

    // 급여대장 조회 가능 여부
    private Boolean payrollAvailable;

    // 조회 불가 사유
    private String payrollUnavailableReason;
}
