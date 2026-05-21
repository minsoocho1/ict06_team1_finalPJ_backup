package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryPageResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummarySearchDTO;

// 관리자 급여요약 서비스
public interface AdPayrollSummaryService {

    // 급여요약 전체조회
    PayrollSummaryPageResponseDTO getPayrollSummaryList(PayrollSummarySearchDTO searchDTO);

    // 급여명세서 조회
    PayrollStatementResponseDTO getPayrollStatement(String empNo, Integer payYear, Integer payMonth);

    // 급여명세서 작성년월 옵션 조회
    PayrollPeriodOptionDTO getStatementPeriodOptions(String empNo);
}
