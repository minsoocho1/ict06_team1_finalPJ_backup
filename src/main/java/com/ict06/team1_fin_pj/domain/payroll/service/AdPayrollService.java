package com.ict06.team1_fin_pj.domain.payroll.service;

import com.ict06.team1_fin_pj.common.dto.payroll.*;

import java.util.List;

// 관리자용 급여대장 서비스
public interface AdPayrollService {

    // 사원 검색 autocomplete
    List<PayrollEmployeeSearchResponseDTO> searchEmployees(PayrollEmployeeSearchDTO searchDTO);

    // 사원 인사정보 조회
    PayrollEmployeeInfoResponseDTO getEmployeeInfo(String empNo);

    // 작성년월 select 옵션 조회
    PayrollPeriodOptionDTO getPeriodOptions(String empNo);

    // 급여대장 상태 조회
    PayrollStatusResponseDTO getPayrollStatus(PayrollMainRequestDTO requestDTO);

    // 기본급 자동 로딩
    PayrollBaseSalaryResponseDTO getBaseSalary(PayrollMainRequestDTO requestDTO);

    // 급여대장 저장
    String savePayroll(PayrollSaveRequestDTO requestDTO);

    // 지급/공제항목 조회
    PayrollItemLoadResponseDTO getPayrollItems(PayrollMainRequestDTO requestDTO);

    // 지급/공제항목 변경 경고 확인 처리
    String decidePayItemSettingChange(PayrollMainRequestDTO requestDTO);

    // 지급/공제항목 설정 저장
    List<PayrollItemLoadResponseDTO.Item> savePayItemSettings(PayItemSettingSaveRequestDTO requestDTO);

    // 계산 미리보기
    PayrollPreviewResponseDTO previewPayroll(PayrollSaveRequestDTO requestDTO);

    // 급여대장 확정
    String confirmPayroll(PayrollSaveRequestDTO requestDTO);

    // 급여대장 지급확정
    String payConfirmPayroll(PayrollSaveRequestDTO requestDTO);

    // 급여대장 삭제
    String deletePayroll(PayrollMainRequestDTO requestDTO);
}
