package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryPageResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummarySearchDTO;

import java.util.List;
import java.util.Optional;

// 급여요약 QueryDSL Repository
public interface PayrollSummaryRepositoryCustom {

    // 급여요약 전체조회
    PayrollSummaryPageResponseDTO selectPayrollSummaryList(PayrollSummarySearchDTO searchDTO, String payMonth);

    // 급여명세서 기본정보 조회
    Optional<PayrollStatementResponseDTO> selectPayrollStatement(String empNo, String payMonth);

    // 급여명세서 지급/공제항목 조회
    List<PayrollStatementResponseDTO.Item> selectPayrollStatementItems(Integer payrollId);
}
