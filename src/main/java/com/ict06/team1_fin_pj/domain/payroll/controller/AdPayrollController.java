package com.ict06.team1_fin_pj.domain.payroll.controller;

import com.ict06.team1_fin_pj.common.dto.payroll.*;
import com.ict06.team1_fin_pj.domain.payroll.service.AdPayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// 관리자용 급여대장 컨트롤러
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/payroll")
public class AdPayrollController {

    private final AdPayrollService adPayrollService;

    // 급여관리 관리자단 메인 페이지
    @GetMapping({"", "/"})
    public String payrollAdminMainPage() {
        return "admin/payroll/payrollMain";
    }

    // 급여대장관리 메인 페이지
    @GetMapping("/main")
    public String payrollMainPage() {
        return "admin/payroll/payroll";
    }

    // 사원 검색 autocomplete
    @GetMapping("/main/employees/search")
    @ResponseBody
    public List<PayrollEmployeeSearchResponseDTO> searchEmployees(PayrollEmployeeSearchDTO searchDTO) {
        return adPayrollService.searchEmployees(searchDTO);
    }

    // 사원 인사정보 조회
    @GetMapping("/main/employees/{empNo}")
    @ResponseBody
    public PayrollEmployeeInfoResponseDTO getEmployeeInfo(@PathVariable String empNo) {
        return adPayrollService.getEmployeeInfo(empNo);
    }

    // 작성년월 select 옵션 조회
    @GetMapping("/main/period-options/{empNo}")
    @ResponseBody
    public PayrollPeriodOptionDTO getPeriodOptions(@PathVariable String empNo) {
        return adPayrollService.getPeriodOptions(empNo);
    }

    // 급여대장 상태 조회
    @GetMapping("/main/status")
    @ResponseBody
    public PayrollStatusResponseDTO getPayrollStatus(PayrollMainRequestDTO requestDTO) {
        return adPayrollService.getPayrollStatus(requestDTO);
    }

    // 기본급 자동 로딩
    @GetMapping("/main/base-salary")
    @ResponseBody
    public PayrollBaseSalaryResponseDTO getBaseSalary(PayrollMainRequestDTO requestDTO) {
        return adPayrollService.getBaseSalary(requestDTO);
    }

    // 급여대장 저장
    @PostMapping("/main/save")
    @ResponseBody
    public String savePayroll(@RequestBody PayrollSaveRequestDTO requestDTO) {
        return adPayrollService.savePayroll(requestDTO);
    }

    // 지급/공제항목 조회
    @GetMapping("/main/items")
    @ResponseBody
    public PayrollItemLoadResponseDTO getPayrollItems(PayrollMainRequestDTO requestDTO) {
        return adPayrollService.getPayrollItems(requestDTO);
    }

    // 지급/공제항목 변경 경고 확인 처리
    @PostMapping("/main/item-settings/decision")
    @ResponseBody
    public String decidePayItemSettingChange(@RequestBody PayrollMainRequestDTO requestDTO) {
        return adPayrollService.decidePayItemSettingChange(requestDTO);
    }

    // 지급/공제항목 설정 저장
    @PostMapping("/main/item-settings/save")
    @ResponseBody
    public List<PayrollItemLoadResponseDTO.Item> savePayItemSettings(
            @RequestBody PayItemSettingSaveRequestDTO requestDTO
    ) {
        return adPayrollService.savePayItemSettings(requestDTO);
    }

    // 계산 미리보기
    @PostMapping("/main/preview")
    @ResponseBody
    public PayrollPreviewResponseDTO previewPayroll(@RequestBody PayrollSaveRequestDTO requestDTO) {
        return adPayrollService.previewPayroll(requestDTO);
    }

    // 급여대장 확정
    @PostMapping("/main/confirm")
    @ResponseBody
    public String confirmPayroll(@RequestBody PayrollSaveRequestDTO requestDTO) {
        return adPayrollService.confirmPayroll(requestDTO);
    }

    // 급여대장 지급확정
    @PostMapping("/main/pay-confirm")
    @ResponseBody
    public String payConfirmPayroll(@RequestBody PayrollSaveRequestDTO  requestDTO) {
        return adPayrollService.payConfirmPayroll(requestDTO);
    }

    // 급여대장 삭제
    @PostMapping("/main/delete")
    @ResponseBody
    public String deletePayroll(@RequestBody PayrollMainRequestDTO requestDTO) {
        return adPayrollService.deletePayroll(requestDTO);
    }
}
