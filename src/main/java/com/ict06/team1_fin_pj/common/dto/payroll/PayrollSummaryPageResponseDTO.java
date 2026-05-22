package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 급여요약 페이징 응답 DTO
@Getter
@AllArgsConstructor
public class PayrollSummaryPageResponseDTO {

    private List<PayrollSummaryResponseDTO> content;

    private long totalCount;

    private int page;
    private int size;
    private int totalPages;
}
