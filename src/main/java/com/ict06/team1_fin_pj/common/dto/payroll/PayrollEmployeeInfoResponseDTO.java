package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

//사원 인사정보 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEmployeeInfoResponseDTO {

    // 사원 PK
    private String empId;

    // 사번
    private String empNo;

    // 사원명
    private String empName;

    // 본부/부서명
    private String deptName;
    private String parentDeptName;

    // 부서 PK
    private Integer deptId;

    // 직급명
    private String positionName;

    // 직급 PK
    private Integer positionId;

    // 급여등급
    private String gradeId;

    // 급여등급 설명
    private String gradeDescription;

    // 재직 상태
    private String status;

    // 재직상태 표시명
    private String statusName;

    // 입사일
    private LocalDate hireDate;

    // 은행명
    private String bank;

    // 계좌번호
    private String accountNo;
}
