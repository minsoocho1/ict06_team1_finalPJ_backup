package com.ict06.team1_fin_pj.domain.payroll.controller;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummaryPageResponseDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollSummarySearchDTO;
import com.ict06.team1_fin_pj.domain.payroll.service.AdPayrollSummaryService;
import com.ict06.team1_fin_pj.domain.payroll.service.AdSalaryPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.YearMonth;
import java.util.List;

// 관리자 급여요약 컨트롤러
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/payroll/summary")
public class AdPayrollSummaryController {

    private final AdPayrollSummaryService adPayrollSummaryService;
    private final AdSalaryPolicyService adSalaryPolicyService;

    // 급여요약 전체조회 페이지
    @GetMapping({"", "/"})
    public String payrollSummaryPage(
            PayrollSummarySearchDTO searchDTO,
            Model model
    ) {

        PayrollSummaryPageResponseDTO pageResponse =
                adPayrollSummaryService.getPayrollSummaryList(searchDTO);

        model.addAttribute("payrollSummaryList", pageResponse.getContent());
        model.addAttribute("pageResponse", pageResponse);

        // 검색 필터 select box 데이터
        model.addAttribute("departmentList", adSalaryPolicyService.getDepartmentList());
        model.addAttribute("positionList", adSalaryPolicyService.getPositionList());

        // 상태 필터 select box 데이터
        model.addAttribute("statusList", List.of(
                "NEW",
                "DRAFT",
                "CONFIRMED",
                "PAID"
        ));

        // 조회년월 select box 기준값
            YearMonth now = YearMonth.now();

            model.addAttribute("currentYear", now.getYear());
            model.addAttribute("currentMonth", now.getMonthValue());

        // 급여요약 조회 시작년도
            model.addAttribute("startYear", 2021);

            // 검색조건 유지
            model.addAttribute("searchDTO", searchDTO);

            return "admin/payroll/payroll-summary";
        }

    // 급여명세서 조회 페이지
    @GetMapping("/statement")
    public String payrollStatementPage(
            @RequestParam String empNo,
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth,

            // 급여요약 복귀용 파라미터
            @RequestParam(required = false) Integer returnPayYear,
            @RequestParam(required = false) Integer returnPayMonth,
            @RequestParam(required = false) String returnKeyword,
            @RequestParam(required = false) String returnDeptId,
            @RequestParam(required = false) String returnPositionId,
            @RequestParam(required = false) String returnStatus,
            @RequestParam(required = false) String returnSortType,
            @RequestParam(required = false) Integer returnPage,

            Model model
    ) {

        YearMonth now = YearMonth.now();

        if (payYear == null) {
            payYear = now.getYear();
        }

        if (payMonth == null) {
            payMonth = now.getMonthValue();
        }

        PayrollStatementResponseDTO statement =
                adPayrollSummaryService.getPayrollStatement(empNo, payYear, payMonth);

        model.addAttribute("statement", statement);

        // 현재 조회 중인 명세서 기준값
        model.addAttribute("empNo", empNo);
        model.addAttribute("payYear", payYear);
        model.addAttribute("payMonth", payMonth);

        // 급여요약으로 돌아갈 때 사용할 원래 목록 상태
        model.addAttribute("returnPayYear", returnPayYear);
        model.addAttribute("returnPayMonth", returnPayMonth);
        model.addAttribute("returnKeyword", returnKeyword);
        model.addAttribute("returnDeptId", returnDeptId);
        model.addAttribute("returnPositionId", returnPositionId);
        model.addAttribute("returnStatus", returnStatus);
        model.addAttribute("returnSortType", returnSortType);
        model.addAttribute("returnPage", returnPage);

        return "admin/payroll/payroll-statement";
    }

    // 급여명세서 데이터 조회
    @GetMapping("/statement/data")
    @ResponseBody
    public PayrollStatementResponseDTO getPayrollStatementData(
            @RequestParam String empNo,
            @RequestParam Integer payYear,
            @RequestParam Integer payMonth
    ) {

        return adPayrollSummaryService.getPayrollStatement(empNo, payYear, payMonth);
    }

    // 급여명세서 작성년월 옵션 조회
    @GetMapping("/statement/period-options")
    @ResponseBody
    public PayrollPeriodOptionDTO getStatementPeriodOptions(
            @RequestParam String empNo
    ) {

        return adPayrollSummaryService.getStatementPeriodOptions(empNo);
    }
}
