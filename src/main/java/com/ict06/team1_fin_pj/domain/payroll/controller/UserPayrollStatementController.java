package com.ict06.team1_fin_pj.domain.payroll.controller;

import com.ict06.team1_fin_pj.common.dto.payroll.PayrollPeriodOptionDTO;
import com.ict06.team1_fin_pj.common.dto.payroll.PayrollStatementResponseDTO;
import com.ict06.team1_fin_pj.domain.payroll.service.UserPayrollStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

// 사용자 급여명세서 REST Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payroll/statements")
public class UserPayrollStatementController {

    private final UserPayrollStatementService userPayrollStatementService;

    // 내 급여명세서 조회
    @GetMapping("/me")
    public PayrollStatementResponseDTO getMyPayrollStatement(
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth,
            Authentication authentication
    ) {

        String empNo = resolveLoginEmpNo(authentication);

        YearMonth now = YearMonth.now();

        if (payYear == null) {
            payYear = now.getYear();
        }

        if (payMonth == null) {
            payMonth = now.getMonthValue();
        }

        return userPayrollStatementService.getMyPayrollStatement(
                empNo,
                payYear,
                payMonth
        );
    }

    // 내 급여명세서 조회년월 옵션
    @GetMapping("/me/period-options")
    public PayrollPeriodOptionDTO getMyStatementPeriodOptions(
            Authentication authentication
    ) {

        String empNo = resolveLoginEmpNo(authentication);

        return userPayrollStatementService.getMyStatementPeriodOptions(empNo);
    }

    /**
     * 로그인 사번 추출
     *
     * 현재 프로젝트 로그인은 사번 기반으로 동작하므로 authentication.getName()을 사용한다.
     * 만약 팀 인증 객체에서 empNo를 별도 필드로 관리하고 있다면,
     * 이 메서드 안쪽만 팀 Security Principal 구조에 맞게 바꾸면 된다.
     */
    private String resolveLoginEmpNo(Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }

        return authentication.getName();
    }
}
