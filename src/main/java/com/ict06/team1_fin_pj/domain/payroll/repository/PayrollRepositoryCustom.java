package com.ict06.team1_fin_pj.domain.payroll.repository;

import com.ict06.team1_fin_pj.common.dto.payroll.*;

import java.util.List;
import java.util.Optional;

// 급여대장 QueryDSL Repository
public interface PayrollRepositoryCustom {

    // 사원 검색 autocomplete
    List<PayrollEmployeeSearchResponseDTO> searchEmployees(PayrollEmployeeSearchDTO searchDTO);

    // 사원 인사정보 조회
    Optional<PayrollEmployeeInfoResponseDTO> selectEmployeeInfo(String empNo);

    // 급여대장 상태 조회
    Optional<PayrollStatusResponseDTO> selectPayrollStatus(String empNo, String payMonth);

    // 기존 저장 급여대장의 기본급 조회
    Optional<PayrollBaseSalaryResponseDTO> selectSavedPayrollBaseSalary(String empNo, String payMonth);

    // 이전 확정/지급완료 기본급 조회
    Optional<PayrollBaseSalaryResponseDTO> selectRecentConfirmedBaseSalary(String empNo, String payMonth);

    // 현재 기본급 정책 조회
    Optional<PayrollBaseSalaryResponseDTO> selectCurrentSalaryPolicyBaseSalary(String empNo);

    // 현재 활성 지급/공제항목 설정 조회
    List<PayrollItemLoadResponseDTO.Item> selectCurrentPayItemSettings();

    // 저장된 급여대장의 지급/공제항목 조회
    List<PayrollItemLoadResponseDTO.Item> selectSavedPayrollItems(String empNo, String payMonth);

    // 현재 활성 지급/공제항목 설정의 최신 수정일 조회
    java.time.LocalDateTime selectLatestPayItemSettingUpdatedAt();

    // 선택 지급월 기준 근태연동 계산용 근태 집계
    PayrollAttendanceSummaryDTO selectAttendanceSummary(String empNo,  java.time.LocalDate startDate, java.time.LocalDate endDate);

    // 현재 지급월 이전의 확정/지급완료 급여월 조회
    List<PayrollClosedMonthDTO> selectClosedPayrollMonthsBefore(String empNo, String currentPayMonth);

    // 특정 확정/지급완료 월의 저장된 지급/공제항목 snapshot 조회
    List<PayrollItemLoadResponseDTO.Item> selectPayrollItemSnapshots(String empNo, String payMonth);

    // 특정 발생월 조정항목이 이미 반영되었는지 확인
    boolean existsAdjustmentItem(String empNo, String itemNameSnapshot);
}
