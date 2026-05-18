package com.ict06.team1_fin_pj.common.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 급여대장 근태연동 계산용 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAttendanceSummaryDTO {

    // 전자결재 승인 후 ATTENDANCE에 반영된 연장근무 총 분
    private Integer overtimeMinutes;

    // 결근일수
    private Integer absenceDays;

    // 근무예정일수
    private Integer workingDays;
}
