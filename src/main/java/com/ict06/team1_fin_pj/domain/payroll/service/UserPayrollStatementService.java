package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;

// 사용자 급여명세서 서비스
public interface UserPayrollStatementService {

    // 내 급여명세서 조회
    PayrollStatementResponseDTO getMyPayrollStatement(String empNo, Integer payYear, Integer payMonth
    );

    // 내 급여명세서 조회년월 옵션
    PayrollPeriodOptionDTO getMyStatementPeriodOptions(String empNo);
}
