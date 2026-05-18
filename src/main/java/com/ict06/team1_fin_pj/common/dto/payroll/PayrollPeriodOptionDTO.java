package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// 급여대장 작성년월 선택 옵션 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodOptionDTO {

    // 기본 선택 연도
    private Integer defaultYear;

    // 기본 선택 월
    private Integer defaultMonth;

    // 입사일
    private LocalDate hireDate;

    // 선택 가능한 연도 목록
    private List<Integer> availableYears;

    // 기본 연도 기준 선택 가능한 월 목록
    private List<Integer> availableMonths;
}
